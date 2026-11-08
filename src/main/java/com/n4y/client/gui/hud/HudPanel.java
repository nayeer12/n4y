package com.n4y.client.gui.hud;

import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The shared look of the on-screen elements: a small glass card with an icon and one line of
 * text per value, so every n4y overlay matches the rest of the client.
 */
public final class HudPanel {
	public static final float LINE = 11f;
	public static final float PAD_X = 8f;
	public static final float PAD_Y = 6f;
	public static final float ICON = 13f;
	public static final float TEXT_SCALE = 0.82f;

	private HudPanel() {
	}

	public static float width(String[] lines) {
		float widest = 0f;
		for (String line : lines) {
			widest = Math.max(widest, N4yText.width(line, false, TEXT_SCALE));
		}
		return PAD_X * 2 + ICON + 6f + widest;
	}

	public static float height(int lines) {
		return PAD_Y * 2 + Math.max(1, lines) * LINE;
	}

	public static void draw(GuiGraphics g, Theme theme, float x, float y, Icon icon, String[] lines) {
		float w = width(lines);
		float h = height(lines.length);
		GlassRenderer.shadow(g, x, y + 1f, w, h, 7f, 7, 0x55000000);
		GlassRenderer.roundedRect(g, x, y, w, h, 7f,
				Colors.fade(theme.panelTop(), 0.95f), Colors.fade(theme.panelBottom(), 0.95f), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(g, x, y, w, h, 7f, 0.5f, theme.borderTop(), theme.borderBottom());
		IconTextures.draw(g, icon, x + PAD_X, y + (h - ICON) / 2f, ICON, theme.accent());

		float textX = x + PAD_X + ICON + 6f;
		float textY = y + PAD_Y + LINE / 2f;
		for (int i = 0; i < lines.length; i++) {
			N4yText.drawMiddle(g, lines[i], textX, textY + i * LINE, TEXT_SCALE,
					i == 0 ? theme.textPrimary() : theme.textSecondary(), i == 0);
		}
	}
}
