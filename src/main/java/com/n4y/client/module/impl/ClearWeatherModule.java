package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Clears the sky and stops the weather from changing, on your own singleplayer world.
 * The weather cycle is handed back when the module is turned off.
 */
public final class ClearWeatherModule extends Module {
	private static final int FOREVER = 1_000_000;

	private boolean previousCycle = true;
	private boolean applied;

	public ClearWeatherModule() {
		super("clear_weather", ModuleCategory.WORLD, Icons.CLOUD);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server == null) {
			return;
		}
		server.execute(() -> {
			ServerLevel level = server.overworld();
			previousCycle = level.getGameRules().get(GameRules.ADVANCE_WEATHER);
			applied = true;
			level.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
			level.setWeatherParameters(FOREVER, 0, false, false);
		});
	}

	@Override
	protected void onDisable(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server == null || !applied) {
			applied = false;
			return;
		}
		server.execute(() -> {
			server.overworld().getGameRules().set(GameRules.ADVANCE_WEATHER, previousCycle, server);
			applied = false;
		});
	}
}
