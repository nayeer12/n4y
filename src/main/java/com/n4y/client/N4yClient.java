package com.n4y.client;

import com.n4y.client.core.event.EventBus;
import com.n4y.client.core.event.events.ClientShutdownEvent;
import com.n4y.client.core.event.events.ClientTickEvent;
import com.n4y.client.core.io.N4yPaths;
import com.n4y.client.core.keybind.N4yKeybinds;
import com.n4y.client.gui.screen.GlassPreviewScreen;
import com.n4y.client.gui.theme.ThemeEngine;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Fabric client entrypoint. Wires Fabric callbacks to the n4y {@link EventBus};
 * every other system listens on the bus instead of talking to Fabric directly.
 */
public final class N4yClient implements ClientModInitializer {
	private static N4yClient instance;

	private final EventBus eventBus = new EventBus();
	private final ThemeEngine themeEngine = new ThemeEngine();

	public static N4yClient get() {
		return instance;
	}

	public EventBus events() {
		return eventBus;
	}

	public ThemeEngine themes() {
		return themeEngine;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		long start = System.nanoTime();

		N4yPaths.ensureCreated();
		N4yKeybinds.register();
		N4yKeybinds.onOpenGui(mc -> mc.setScreen(new GlassPreviewScreen()));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			N4yKeybinds.tick(mc);
			eventBus.post(new ClientTickEvent(mc));
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> eventBus.post(new ClientShutdownEvent()));

		N4y.LOGGER.info("{} {} by {} initialised in {} ms", N4y.NAME, N4y.version(), N4y.AUTHOR,
				(System.nanoTime() - start) / 1_000_000);
	}
}
