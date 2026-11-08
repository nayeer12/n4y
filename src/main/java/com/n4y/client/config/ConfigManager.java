package com.n4y.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.n4y.client.N4y;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleManager;

/**
 * The one place where n4y reads and writes preferences. Everything lives in
 * {@code config/n4y/}: {@code client.json} remembers which profile is active and each profile
 * is a file in {@code profiles/} holding the theme and the state of every module.
 *
 * <p>Saving is debounced and the file write happens on a small background thread, so toggling
 * a module never stalls the game. {@link #flush()} writes synchronously on shutdown.
 */
public final class ConfigManager {
	/** Minimum gap between automatic saves. */
	private static final long SAVE_DELAY_MILLIS = 1_000L;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path root;
	private final Path profilesDir;
	private final ThemeEngine themes;
	private final ModuleManager modules;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "n4y-config-writer");
		thread.setDaemon(true);
		return thread;
	});

	private ClientPrefs prefs = ClientPrefs.DEFAULT;
	private Profile active = Profile.empty(Profile.DEFAULT_NAME);
	private boolean dirty;
	private long lastSave;

	public ConfigManager(Path root, ThemeEngine themes, ModuleManager modules) {
		this.root = root;
		this.profilesDir = root.resolve("profiles");
		this.themes = themes;
		this.modules = modules;
	}

	// ---------------------------------------------------------------- loading

	/** Reads the files from disk. Does not touch the game, so it is safe during mod init. */
	public void loadFromDisk() {
		prefs = read(root.resolve("client.json"), ClientPrefs.CODEC).orElse(ClientPrefs.DEFAULT);
		active = read(profilePath(prefs.activeProfile()), Profile.CODEC)
				.orElseGet(() -> Profile.empty(Profile.DEFAULT_NAME));
	}

	/** Applies what was loaded to the theme engine and the modules. Call once the game is up. */
	public void applyToGame() {
		apply(active);
	}

	private void apply(Profile profile) {
		Themes.byId(profile.theme()).ifPresent(themes::selectInstant);
		modules.applySilently(() -> {
			for (Module module : modules.all()) {
				ModuleState state = profile.state(module.id());
				module.setKey(state.key());
				module.setEnabled(state.enabled());
			}
		});
	}

	// ---------------------------------------------------------------- saving

	/** Marks the configuration as changed; the write happens shortly after, off the game thread. */
	public void markDirty() {
		dirty = true;
	}

	/** Called every client tick: writes at most once per second and only when something changed. */
	public void tick() {
		if (dirty && System.currentTimeMillis() - lastSave >= SAVE_DELAY_MILLIS) {
			save(false);
		}
	}

	/** Writes any pending change immediately, on the calling thread. */
	public void flush() {
		if (dirty) {
			save(true);
		}
	}

	/** Flushes and stops the writer thread; called once, when the game closes. */
	public void shutdown() {
		flush();
		writer.shutdown();
		try {
			if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
				N4y.LOGGER.warn("Timed out waiting for the configuration to be written");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void save(boolean synchronous) {
		dirty = false;
		lastSave = System.currentTimeMillis();
		active = snapshot(active.name());
		prefs = new ClientPrefs(active.fileName());

		JsonElement profileJson = encode(active, Profile.CODEC);
		JsonElement prefsJson = encode(prefs, ClientPrefs.CODEC);
		Path profileFile = profilePath(active.fileName());
		Path prefsFile = root.resolve("client.json");

		Runnable task = () -> {
			write(profileFile, profileJson);
			write(prefsFile, prefsJson);
		};
		if (synchronous) {
			task.run();
		} else {
			writer.execute(task);
		}
	}

	/**
	 * The current state of the game as a profile. Modules left at their defaults are not
	 * written, so the file only lists what the user actually changed.
	 */
	public Profile snapshot(String name) {
		Map<String, ModuleState> states = new LinkedHashMap<>();
		for (Module module : modules.all()) {
			ModuleState state = ModuleState.of(module);
			if (!state.equals(ModuleState.DEFAULT)) {
				states.put(module.id(), state);
			}
		}
		return new Profile(name, themes.selected().id(), states);
	}

	// ---------------------------------------------------------------- profiles

	public String activeProfileName() {
		return active.name();
	}

	/** Every profile on disk plus the active one, sorted by name. */
	public List<String> profileNames() {
		List<String> names = new ArrayList<>();
		names.add(active.name());
		if (Files.isDirectory(profilesDir)) {
			try (Stream<Path> files = Files.list(profilesDir)) {
				files.filter(p -> p.getFileName().toString().endsWith(".json"))
						.map(p -> read(p, Profile.CODEC).map(Profile::name).orElse(null))
						.filter(name -> name != null && !name.equalsIgnoreCase(active.name()))
						.forEach(names::add);
			} catch (IOException e) {
				N4y.LOGGER.error("Could not list the profiles in {}", profilesDir, e);
			}
		}
		names.sort(Comparator.comparing(String::toLowerCase));
		return names;
	}

	public boolean exists(String name) {
		return Files.exists(profilePath(Profile.fileNameOf(name)));
	}

	/**
	 * Saves the current state as a new profile and switches to it.
	 *
	 * @return false when the name is unusable or already taken
	 */
	public boolean createProfile(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (!Profile.isValidName(trimmed) || exists(trimmed) || trimmed.equalsIgnoreCase(active.name())) {
			return false;
		}
		save(true);
		active = snapshot(trimmed);
		markDirty();
		save(true);
		return true;
	}

	/** Loads {@code name} and applies it to the game, saving the profile being left first. */
	public boolean switchProfile(String name) {
		if (name.equalsIgnoreCase(active.name())) {
			return false;
		}
		Optional<Profile> loaded = read(profilePath(Profile.fileNameOf(name)), Profile.CODEC);
		if (loaded.isEmpty()) {
			return false;
		}
		save(true);
		active = loaded.get();
		apply(active);
		prefs = new ClientPrefs(active.fileName());
		markDirty();
		save(true);
		return true;
	}

	/** Deletes a profile. The active profile cannot be deleted. */
	public boolean deleteProfile(String name) {
		if (name.equalsIgnoreCase(active.name())) {
			return false;
		}
		try {
			return Files.deleteIfExists(profilePath(Profile.fileNameOf(name)));
		} catch (IOException e) {
			N4y.LOGGER.error("Could not delete the profile '{}'", name, e);
			return false;
		}
	}

	// ---------------------------------------------------------------- files

	private Path profilePath(String fileName) {
		return profilesDir.resolve(fileName + ".json");
	}

	private <T> Optional<T> read(Path path, Codec<T> codec) {
		if (!Files.isRegularFile(path)) {
			return Optional.empty();
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement json = JsonParser.parseReader(reader);
			return codec.parse(JsonOps.INSTANCE, json)
					.resultOrPartial(error -> N4y.LOGGER.error("Invalid configuration in {}: {}", path, error))
					.map(value -> value);
		} catch (Exception e) {
			N4y.LOGGER.error("Could not read {}", path, e);
			return Optional.empty();
		}
	}

	private <T> JsonElement encode(T value, Codec<T> codec) {
		return codec.encodeStart(JsonOps.INSTANCE, value)
				.resultOrPartial(error -> N4y.LOGGER.error("Could not serialise the configuration: {}", error))
				.orElseGet(com.google.gson.JsonObject::new);
	}

	/** Writes through a temporary file so a crash mid-write cannot corrupt the configuration. */
	private void write(Path path, JsonElement json) {
		try {
			Files.createDirectories(path.getParent());
			Path temp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(json, out);
				out.write('\n');
			}
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			N4y.LOGGER.error("Could not write {}", path, e);
		}
	}

	/** Exposed for the tests: the theme currently selected, as it would be stored. */
	public Theme selectedTheme() {
		return themes.selected();
	}
}
