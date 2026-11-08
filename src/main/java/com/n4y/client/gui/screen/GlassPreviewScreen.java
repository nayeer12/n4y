package com.n4y.client.gui.screen;

import com.n4y.client.N4y;
import com.n4y.client.N4yClient;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.gui.theme.Themes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Phase 2 test bench for the Glass engine: shows every primitive with the active theme and
 * lets you switch between the four themes. Replaced by the main interface in phase 3.
 */
public final class GlassPreviewScreen extends Screen {
	private static final float W = 340, H = 196, R = 14;

	private final Animation open = new Animation(0f, 320, Easing.OUT_CUBIC);
	private final Animation[] hover = new Animation[Themes.ALL.size()];

	public GlassPreviewScreen() {
		super(Component.literal(N4y.NAME));
		for (int i = 0; i < hover.length; i++) {
			hover[i] = new Animation(0f, 160, Easing.OUT_CUBIC);
		}
	}

	private ThemeEngine themes() {
		return N4yClient.get().themes();
	}

	@Override
	protected void init() {
		open.animateTo(1f);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null) {
			renderPanorama(g, delta);
		}
		renderBlurredBackground(g);
		GlassRenderer.backdrop(g, width, height, themes().current(), open.value());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Theme t = themes().current();
		float a = open.value();
		float x = (width - W) / 2f;
		float y = (height - H) / 2f + (1f - a) * 12f;

		GlassRenderer.glassPanel(g, x, y, W, H, R, t, a);

		// header: logo block + name + version
		GlassRenderer.roundedRect(g, x + 14, y + 12, 22, 22, 7, Colors.fade(t.accent(), a), Colors.fade(Colors.shade(t.accent(), -0.35f), a), GlassRenderer.CORNERS_ALL);
		N4yText.draw(g, "n4y", x + 17.5f, y + 18.5f, 0.9f, Colors.fade(0xFFFFFFFF, a), true);
		N4yText.draw(g, N4y.NAME, x + 44, y + 13, 1.25f, Colors.fade(t.textPrimary(), a), true);
		N4yText.draw(g, "v" + N4y.version() + " · Motor Glass (Fase 2)", x + 44, y + 26, 0.85f, Colors.fade(t.textMuted(), a), false);

		// sample cards: plain, hovered, glowing
		float cy = y + 48;
		float cw = (W - 28 - 16) / 3f;
		String[][] cards = {{"Superficie", "Tarjeta en reposo"}, {"Hover", "Brillo de borde"}, {"Activo", "Resplandor del acento"}};
		for (int i = 0; i < 3; i++) {
			float cx = x + 14 + i * (cw + 8);
			GlassRenderer.glassCard(g, cx, cy, cw, 50, 10, t, i == 1 ? 1f : 0f, i == 2 ? 1f : 0f, a);
			N4yText.draw(g, cards[i][0], cx + 10, cy + 13, 1f, Colors.fade(t.textPrimary(), a), true);
			N4yText.draw(g, cards[i][1], cx + 10, cy + 27, 0.8f, Colors.fade(t.textSecondary(), a), false);
		}

		// theme swatches (functional: click to switch)
		N4yText.draw(g, "Tema", x + 14, y + 114, 0.9f, Colors.fade(t.textSecondary(), a), true);
		long now = System.nanoTime();
		for (int i = 0; i < Themes.ALL.size(); i++) {
			Theme option = Themes.ALL.get(i);
			float[] b = swatchBounds(i);
			boolean over = inside(mouseX, mouseY, b);
			boolean selected = option.equals(themes().selected());
			hover[i].animateTo(over || selected ? 1f : 0f, now);
			float h = hover[i].value(now);
			GlassRenderer.glassCard(g, b[0], b[1], b[2], b[3], b[3] / 2f, t, h, selected ? 1f : 0f, a);
			GlassRenderer.roundedRect(g, b[0] + 8, b[1] + 7, 10, 10, 5, Colors.fade(option.accent(), a));
			N4yText.draw(g, option.displayName(), b[0] + 24, b[1] + 8, 0.9f, Colors.fade(t.textPrimary(), a), false);
		}

		// text scale ramp
		N4yText.draw(g, "Inter · títulos SemiBold, descripciones Regular", x + 14, y + H - 22, 0.8f, Colors.fade(t.textMuted(), a), false);
	}

	private float[] swatchBounds(int i) {
		float x = (width - W) / 2f;
		float y = (height - H) / 2f;
		float w = (W - 28 - 24) / 4f;
		return new float[] {x + 14 + i * (w + 8), y + 128, w, 24};
	}

	private static boolean inside(double mx, double my, float[] b) {
		return mx >= b[0] && my >= b[1] && mx < b[0] + b[2] && my < b[1] + b[3];
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (int i = 0; i < Themes.ALL.size(); i++) {
				if (inside(event.x(), event.y(), swatchBounds(i))) {
					themes().select(Themes.ALL.get(i));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
