package com.n4y.client.gui.screen.panel;

import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.widget.UiContext;

/**
 * The scrollbar and the fades that hide rows clipped by the top and bottom of a list.
 *
 * <p>Every scrollable tab draws the same thing beside its list, so it is written once here
 * instead of once per tab.
 */
public final class PanelScrollbar {
	private static final float TRACK_W = 2.5f;
	private static final float TRACK_GAP = 3f;
	private static final float FADE_H = 10f;
	private static final float MIN_BAR_H = 18f;

	private PanelScrollbar() {
	}

	/** Draws nothing when the list fits. {@code (x, y, w, h)} is the visible list area. */
	public static void render(UiContext ctx, ScrollState scroll, float x, float y, float w, float h) {
		if (!scroll.hasOverflow()) {
			return;
		}
		float extent = scroll.extent();
		float offset = scroll.offset(ctx.now());
		int edge = Colors.fade(ctx.theme().panelBottom(), ctx.alpha() * 0.9f);
		if (offset > 1f) {
			ctx.g().fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + FADE_H),
					edge, Colors.fade(edge, 0f));
		}
		if (offset < extent - 1f) {
			ctx.g().fillGradient(Math.round(x), Math.round(y + h - FADE_H - 2f), Math.round(x + w), Math.round(y + h),
					Colors.fade(edge, 0f), edge);
		}
		float trackX = x + w + TRACK_GAP;
		float barH = Math.max(MIN_BAR_H, h * (h / (h + extent)));
		float barY = y + (h - barH) * (offset / extent);
		GlassRenderer.roundedRect(ctx.g(), trackX, y, TRACK_W, h, TRACK_W / 2f, Colors.fade(0x18FFFFFF, ctx.alpha()));
		GlassRenderer.roundedRect(ctx.g(), trackX, barY, TRACK_W, barH, TRACK_W / 2f,
				Colors.fade(ctx.theme().accent(), ctx.alpha() * 0.8f));
	}
}
