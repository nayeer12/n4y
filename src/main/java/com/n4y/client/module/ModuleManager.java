package com.n4y.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.n4y.client.N4y;
import com.n4y.client.core.Strings;
import com.n4y.client.core.event.EventBus;
import com.n4y.client.core.event.events.ClientShutdownEvent;
import com.n4y.client.core.event.events.ClientTickEvent;
import com.n4y.client.module.impl.AutoJumpModule;
import com.n4y.client.module.impl.AutoSprintModule;
import com.n4y.client.module.impl.CinematicCameraModule;
import com.n4y.client.module.impl.HideHudModule;
import com.n4y.client.module.impl.NoViewBobbingModule;
import com.n4y.client.module.impl.ToggleSneakModule;
import com.n4y.client.module.impl.ZoomModule;

import net.minecraft.client.Minecraft;

/** Registry and driver of every {@link Module}. */
public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private Runnable changeListener = () -> { };
	/** Suppresses change notifications while a stored configuration is being applied. */
	private boolean applying;

	/** Registers the built-in modules. Order here is the order shown in the interface. */
	public void registerDefaults() {
		register(new AutoSprintModule());
		register(new AutoJumpModule());
		register(new ToggleSneakModule());
		register(new ZoomModule());
		register(new NoViewBobbingModule());
		register(new CinematicCameraModule());
		register(new HideHudModule());
	}

	public void register(Module module) {
		if (byId(module.id()).isPresent()) {
			throw new IllegalArgumentException("Duplicate module id: " + module.id());
		}
		module.setChangeListener(this::onModuleChanged);
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

	/** Turns everything off and writes the restored vanilla options to disk. */
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
