package com.n4y.client.module.impl;

import com.n4y.client.N4yClient;
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

/** How long this session has been running, the active profile and how much is turned on. */
public final class SessionStatsModule extends HudModule {
	public SessionStatsModule() {
		super("session_stats", ModuleCategory.MISC, Icons.CLOCK, HudPlacement.of(HudAnchor.BOTTOM_LEFT, 6f, -6f));
	}

	private String[] lines(Minecraft mc) {
		long seconds = N4yClient.get().sessionSeconds();
		String time = String.format("%d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
		String profile = N4yClient.get().config().activeProfileName();
		int active = N4yClient.get().modules().enabled().size();
		return new String[] {
				Component.translatable("n4y.hud.session", time).getString(),
				Component.translatable("n4y.hud.profile", profile, active).getString()
		};
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
		HudPanel.draw(g, theme, x, y, Icons.CLOCK, lines(mc));
	}
}
