package com.n4y.client.module.impl;

import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.hud.HudPanel;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Position and facing on screen, without opening the debug overlay. */
public final class CoordinatesModule extends HudModule {
	public CoordinatesModule() {
		super("coordinates", ModuleCategory.MISC, Icons.PIN, HudPlacement.of(HudAnchor.TOP_LEFT, 6f, 6f));
	}

	private String[] lines(Minecraft mc) {
		if (mc.player == null) {
			return new String[] {"-", "-"};
		}
		String position = String.format("%.1f  %.1f  %.1f",
				mc.player.getX(), mc.player.getY(), mc.player.getZ());
		String facing = Component.translatable("n4y.hud.facing",
				Component.translatable("n4y.direction." + mc.player.getDirection().getSerializedName()).getString()).getString();
		return new String[] {position, facing};
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
		HudPanel.draw(g, theme, x, y, Icons.PIN, lines(mc));
	}
}
