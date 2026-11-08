package com.n4y.client;

import com.n4y.client.combat.CombatContext;
import com.n4y.client.config.ConfigManager;
import com.n4y.client.core.event.EventBus;
import com.n4y.client.core.event.Priority;
import com.n4y.client.core.event.events.ClientShutdownEvent;
import com.n4y.client.core.event.events.ClientTickEvent;
import com.n4y.client.core.io.N4yPaths;
import com.n4y.client.core.keybind.KeybindManager;
import com.n4y.client.core.keybind.N4yKeybinds;
import com.n4y.client.gui.screen.N4yMainScreen;
import com.n4y.client.gui.hud.HudManager;
import com.n4y.client.gui.hud.NotificationCenter;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.module.ModuleManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Fabric client entrypoint. Builds the managers, wires Fabric's callbacks to the n4y
 * {@link EventBus} and keeps the stored configuration in sync with the running game.
 */
public final class N4yClient implements ClientModInitializer {
	private static N4yClient instance;

	private final EventBus eventBus = new EventBus();
	private final ThemeEngine themeEngine = new ThemeEngine();
	private final ModuleManager moduleManager = new ModuleManager();
	private final HudManager hudManager = new HudManager(moduleManager, themeEngine);
	private final long startedAt = System.currentTimeMillis();
	private ConfigManager configManager;
	private KeybindManager keybindManager;
	private CombatContext combat;

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

	public HudManager hud() {
		return hudManager;
	}

	public NotificationCenter notifications() {
		return hudManager.notifications();
	}

	public CombatContext combat() {
		return combat;
	}

	/** How long this play session has been running, used by the on-screen statistics. */
	public long sessionSeconds() {
		return (System.currentTimeMillis() - startedAt) / 1000L;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		long start = System.nanoTime();

		N4yPaths.ensureCreated();
		N4yKeybinds.register();
		N4yKeybinds.onOpenGui(mc -> mc.setScreen(new N4yMainScreen()));
		combat = CombatContext.create((title, detail) -> hudManager.notifications()
				.push(Icons.FLAME, title, detail, false));
		moduleManager.registerDefaults(message -> hudManager.notifications()
				.push(Icons.CAMERA, Component.translatable("n4y.notify.screenshot"), message, true), combat);
		moduleManager.attach(eventBus);
		hudManager.register();

		configManager = new ConfigManager(N4yPaths.root(), themeEngine, moduleManager);
		moduleManager.onChange(configManager::markDirty);
		moduleManager.onToggle(module -> hudManager.notifications().push(module.icon(), module.displayName(),
				Component.translatable(module.isEnabled() ? "n4y.notify.on" : "n4y.notify.off"), module.isEnabled()));
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
			// The shared use cooldown counts down before any module acts this tick.
			combat.interactions().tick();
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
