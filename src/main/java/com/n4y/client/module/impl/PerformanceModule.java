package com.n4y.client.module.impl;

import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.hud.PanelHudModule;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Frames per second and memory use, sampled twice a second so the numbers stay readable. */
public final class PerformanceModule extends PanelHudModule {
	private static final long REFRESH_MILLIS = 500L;
	private static final long BYTES_PER_MIB = 1_048_576L;

	public PerformanceModule() {
		super("performance", ModuleCategory.MISC, Icons.CHART,
				HudPlacement.of(HudAnchor.TOP_LEFT, 6f, 41f), 2, REFRESH_MILLIS);
	}

	@Override
	protected void refresh(Minecraft mc) {
		Runtime runtime = Runtime.getRuntime();
		long used = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MIB;
		long max = runtime.maxMemory() / BYTES_PER_MIB;
		set(0, Component.translatable("n4y.hud.fps", mc == null ? 0 : mc.getFps()).getString());
		set(1, Component.translatable("n4y.hud.memory", used, max).getString());
	}
}
