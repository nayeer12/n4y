package com.n4y.client.gui.widget;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.theme.Theme;

/** The pill switch of the reference design: track tints to the accent, knob slides and pops. */
public final class ToggleSwitch {
	private final Animation progress = new Animation(0f, 260, Easing.OUT_BACK);

	public void set(boolean on, long now) {
		progress.animateTo(on ? 1f : 0f, now);
	}

	public void setInstant(boolean on) {
		progress.set(on ? 1f : 0f);
	}

	/** Draws the switch; {@code p} is clamped because OUT_BACK overshoots past 1. */
	public void draw(UiContext ctx, float x, float y, float w, float h) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		float raw = progress.value(ctx.now());
		float p = Math.max(0f, Math.min(1f, raw));
		float r = h / 2f;

		if (p > 0.02f) {
			GlassRenderer.shadow(ctx.g(), x, y, w, h, r, 6, Colors.fade(t.accentGlow(), 0.5f * p * a));
		}
		int track = Colors.lerp(t.toggleOff(), t.accent(), p);
		GlassRenderer.roundedRect(ctx.g(), x, y, w, h, r, Colors.fade(Colors.shade(track, -0.12f), a), Colors.fade(track, a), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), x, y, w, h, r, 0.5f, Colors.fade(0x40FFFFFF, a), Colors.fade(0x18FFFFFF, a));

		float pad = 1.6f;
		float knob = h - pad * 2f;
		float kx = x + pad + Math.max(0f, Math.min(w - h, raw * (w - h)));
		GlassRenderer.shadow(ctx.g(), kx, y + pad + 0.5f, knob, knob, knob / 2f, 3, Colors.fade(0x66000000, a));
		GlassRenderer.roundedRect(ctx.g(), kx, y + pad, knob, knob, knob / 2f,
				Colors.fade(0xFFFFFFFF, a), Colors.fade(0xFFE8E6F5, a), GlassRenderer.CORNERS_ALL);
	}
}
