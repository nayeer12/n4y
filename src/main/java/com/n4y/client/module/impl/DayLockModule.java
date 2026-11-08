package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Freezes the world at noon: perfect light for building and recording. Works on your own
 * singleplayer world, where n4y can talk to the integrated server directly.
 *
 * <p>The time of day and the day/night cycle rule are restored when it is turned off.
 */
public final class DayLockModule extends Module {
	private static final long NOON = 6000L;

	private long previousTime;
	private boolean previousCycle = true;
	private boolean applied;

	public DayLockModule() {
		super("day_lock", ModuleCategory.WORLD, Icons.CLOCK);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server == null) {
			return;
		}
		server.execute(() -> {
			ServerLevel level = server.overworld();
			previousTime = level.getDayTime();
			previousCycle = level.getGameRules().get(GameRules.ADVANCE_TIME);
			applied = true;
			level.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
			level.setDayTime(NOON);
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
			ServerLevel level = server.overworld();
			level.getGameRules().set(GameRules.ADVANCE_TIME, previousCycle, server);
			level.setDayTime(previousTime);
			applied = false;
		});
	}

	/** True when it could not be applied because this is not a singleplayer world. */
	public boolean needsSingleplayer(Minecraft mc) {
		return mc.getSingleplayerServer() == null;
	}
}
