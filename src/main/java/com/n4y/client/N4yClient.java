package com.n4y.client;

import com.n4y.client.config.ConfigManager;
import com.n4y.client.core.event.EventBus;
import com.n4y.client.core.event.Priority;
import com.n4y.client.core.event.events.ClientShutdownEvent;
import com.n4y.client.core.event.events.ClientTickEvent;
import com.n4y.client.core.io.N4yPaths;
import com.n4y.client.core.keybind.KeybindManager;
import com.n4y.client.core.keybind.N4yKeybinds;
import com.n4y.client.gui.screen.N4yMainScreen;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.module.ModuleManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Fabric client entrypoint. Builds the managers, wires Fabric's callbacks to the n4y
 * {@link EventBus} and keeps the stored configuration in sync with the running game.
 */
public final class N4yClient implements ClientModInitializer {
	private static N4yClient instance;

	private final EventBus eventBus = new EventBus();
	private final ThemeEngine themeEngine = new ThemeEngine();
	private final ModuleManager moduleManager = new ModuleManager();
	private ConfigManager configManager;
	private KeybindManager keybindManager;

	public static N4yClient get() {
		return instance;
	}

	public EventBus events() {
		return eventBus;
	}

	public ThemeEngine themes() {
		return themeEngine;
	}

	public ModuleManager modules() {
		return moduleManager;
	}

	public ConfigManager config() {
		return configManager;
	}

	public KeybindManager keybinds() {
		return keybindManager;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		long start = System.nanoTime();

		N4yPaths.ensureCreated();
		N4yKeybinds.register();
		N4yKeybinds.onOpenGui(mc -> mc.setScreen(new N4yMainScreen()));
		moduleManager.registerDefaults();
		moduleManager.attach(eventBus);

		configManager = new ConfigManager(N4yPaths.root(), themeEngine, moduleManager);
		moduleManager.onChange(configManager::markDirty);
		themeEngine.onChange(configManager::markDirty);
		// Reading the files is safe this early; applying them to the game is not, so that
		// waits until Minecraft has finished starting.
		configManager.loadFromDisk();

		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			keybindManager = KeybindManager.forGame(moduleManager, mc);
			configManager.applyToGame();
			N4y.LOGGER.info("Profile '{}' loaded with {} module(s) active",
					configManager.activeProfileName(), moduleManager.enabled().size());
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			N4yKeybinds.tick(mc);
			if (keybindManager != null) {
				keybindManager.tick(mc.screen == null);
			}
			configManager.tick();
			eventBus.post(new ClientTickEvent(mc));
		});

		// Highest priority: the configuration must be written before the modules are turned
		// off to restore the player's vanilla options.
		eventBus.subscribe(ClientShutdownEvent.class, Priority.HIGHEST, event -> configManager.shutdown());
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> eventBus.post(new ClientShutdownEvent()));

		N4y.LOGGER.info("{} {} by {} initialised in {} ms", N4y.NAME, N4y.version(), N4y.AUTHOR,
				(System.nanoTime() - start) / 1_000_000);
	}

	/** Convenience for code that only needs the game instance. */
	public static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
