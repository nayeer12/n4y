package com.n4y.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.n4y.client.N4y;
import com.n4y.client.core.Strings;
import com.n4y.client.core.event.EventBus;
import com.n4y.client.core.event.events.ClientShutdownEvent;
import com.n4y.client.core.event.events.ClientTickEvent;
import java.util.function.Consumer;

import com.n4y.client.module.impl.AutoJumpModule;
import com.n4y.client.module.impl.AutoSprintModule;
import com.n4y.client.module.impl.CinematicCameraModule;
import com.n4y.client.module.impl.CinematicModeModule;
import com.n4y.client.module.impl.CleanScreenshotModule;
import com.n4y.client.module.impl.ClearWeatherModule;
import com.n4y.client.module.impl.CoordinatesModule;
import com.n4y.client.module.impl.DayLockModule;
import com.n4y.client.module.impl.FullbrightModule;
import com.n4y.client.module.impl.HideHandModule;
import com.n4y.client.module.impl.HitMarkerModule;
import com.n4y.client.module.impl.NoFireOverlayModule;
import com.n4y.client.module.impl.NoHurtCamModule;
import com.n4y.client.module.impl.PerformanceModule;
import com.n4y.client.module.impl.SessionStatsModule;
import com.n4y.client.module.impl.HideHudModule;
import com.n4y.client.module.impl.NoViewBobbingModule;
import com.n4y.client.module.impl.ToggleSneakModule;
import com.n4y.client.module.impl.ZoomModule;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Registry and driver of every {@link Module}. */
public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private Runnable changeListener = () -> { };
	private Consumer<Module> stateListener = module -> { };
	/** Suppresses change notifications while a stored configuration is being applied. */
	private boolean applying;

	/**
	 * Registers the built-in modules. Order here is the order shown in the interface.
	 *
	 * @param onScreenshot where the Clean Screenshot module reports the saved file
	 */
	public void registerDefaults(Consumer<Component> onScreenshot) {
		// Movement
		register(new AutoSprintModule());
		register(new AutoJumpModule());
		// Player
		register(new ToggleSneakModule());
		register(new NoHurtCamModule());
		// Visual
		register(new ZoomModule());
		register(new FullbrightModule());
		register(new NoViewBobbingModule());
		register(new CinematicCameraModule());
		register(new NoFireOverlayModule());
		register(new HideHudModule());
		// World
		register(new DayLockModule());
		register(new ClearWeatherModule());
		// Creator
		register(new CinematicModeModule(this));
		register(new CleanScreenshotModule(onScreenshot));
		register(new HideHandModule());
		// Combat
		HitMarkerModule hitMarker = new HitMarkerModule();
		hitMarker.listen();
		register(hitMarker);
		// On screen
		register(new CoordinatesModule());
		register(new PerformanceModule());
		register(new SessionStatsModule());
	}

	public void register(Module module) {
		if (byId(module.id()).isPresent()) {
			throw new IllegalArgumentException("Duplicate module id: " + module.id());
		}
		module.setChangeListener(this::onModuleChanged);
		module.setStateListener(this::onModuleToggled);
		modules.add(module);
	}

	/** Called whenever any module is toggled or rebound; the config layer listens here. */
	public void onChange(Runnable listener) {
		this.changeListener = listener;
	}

	private void onModuleChanged() {
		if (!applying) {
			changeListener.run();
		}
	}

	/** Called when a module is switched on or off by the player, not while loading a profile. */
	public void onToggle(Consumer<Module> listener) {
		this.stateListener = listener;
	}

	private void onModuleToggled(Module module) {
		if (!applying) {
			stateListener.accept(module);
		}
	}

	/** Runs {@code action} without reporting the resulting changes (used while loading). */
	public void applySilently(Runnable action) {
		applying = true;
		try {
			action.run();
		} finally {
			applying = false;
		}
	}

	/** Hooks the manager onto the client event bus. */
	public void attach(EventBus bus) {
		bus.subscribe(ClientTickEvent.class, e -> tick(e.client()));
		bus.subscribe(ClientShutdownEvent.class, e -> disableAll(Minecraft.getInstance()));
	}

	private void tick(Minecraft mc) {
		for (Module module : modules) {
			if (module.isEnabled()) {
				try {
					module.onTick(mc);
				} catch (Throwable t) {
					N4y.LOGGER.error("Module '{}' failed while ticking, disabling it", module.id(), t);
					module.setEnabled(false);
				}
			}
		}
	}

	/** Turns everything off, clears the render flags and writes the restored options to disk. */
	public void disableAll(Minecraft mc) {
		boolean[] any = {false};
		applySilently(() -> {
			for (Module module : modules) {
				if (module.isEnabled()) {
					module.setEnabled(false);
					any[0] = true;
				}
			}
		});
		ModuleFlags.reset();
		if (any[0] && mc.options != null) {
			mc.options.save();
		}
	}

	public List<Module> all() {
		return List.copyOf(modules);
	}

	public Optional<Module> byId(String id) {
		return modules.stream().filter(m -> m.id().equals(id)).findFirst();
	}

	public List<Module> enabled() {
		return modules.stream().filter(Module::isEnabled).toList();
	}

	/**
	 * Modules of {@code category} ({@link ModuleCategory#ALL} means every one) whose name or
	 * description contains {@code query}, ignoring case and accents.
	 */
	public List<Module> filter(ModuleCategory category, String query) {
		String needle = normalise(query);
		List<Module> out = new ArrayList<>();
		for (Module module : modules) {
			if (category != ModuleCategory.ALL && module.category() != category) {
				continue;
			}
			if (!needle.isEmpty()
					&& !normalise(module.displayName().getString()).contains(needle)
					&& !normalise(module.description().getString()).contains(needle)
					&& !normalise(module.id()).contains(needle)) {
				continue;
			}
			out.add(module);
		}
		return out;
	}

	/** Lower-cases and strips accents so "zoom" matches "Zoom" and "camara" matches "Cámara". */
	public static String normalise(String text) {
		return Strings.normalise(text);
	}
}
