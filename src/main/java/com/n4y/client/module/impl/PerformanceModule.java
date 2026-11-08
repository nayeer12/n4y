package com.n4y.client.module.impl;

import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPanel;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Frames per second and memory use, sampled once a second so the numbers stay readable. */
public final class PerformanceModule extends HudModule {
	private static final long SAMPLE_MILLIS = 500L;

	private long lastSample;
	private String fpsLine = "";
	private String memoryLine = "";

	public PerformanceModule() {
		super("performance", ModuleCategory.MISC, Icons.CHART, HudPlacement.of(HudAnchor.TOP_LEFT, 6f, 41f));
	}

	private String[] lines(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (now - lastSample > SAMPLE_MILLIS || fpsLine.isEmpty()) {
			lastSample = now;
			Runtime runtime = Runtime.getRuntime();
			long used = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
			long max = runtime.maxMemory() / 1_048_576L;
			fpsLine = Component.translatable("n4y.hud.fps", mc.getFps()).getString();
			memoryLine = Component.translatable("n4y.hud.memory", used, max).getString();
		}
		return new String[] {fpsLine, memoryLine};
	}

	@Override
	public float width(Minecraft mc) {
		return HudPanel.width(lines(mc));
	}

	@Override
	public float height(Minecraft mc) {
		return HudPanel.height(2);
	}

	@Override
	public void drawHud(GuiGraphics g, Minecraft mc, Theme theme, float x, float y) {
		HudPanel.draw(g, mc == null ? theme : theme, x, y, Icons.CHART, lines(mc));
	}
}
