package com.n4y.client.gui.widget;

import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.setting.IntSetting;

/** Glass slider for a whole-number setting, with the current value shown beside the track. */
public final class SliderWidget extends N4yWidget {
	private static final float LABEL_W = 34f;
	private static final float TRACK_H = 3.5f;
	private static final float KNOB = 9f;

	private final IntSetting setting;
	private boolean dragging;

	public SliderWidget(IntSetting setting) {
		this.setting = setting;
	}

	public boolean isDragging() {
		return dragging;
	}

	private float trackX() {
		return x + KNOB / 2f;
	}

	private float trackWidth() {
		return Math.max(1f, width - LABEL_W - KNOB);
	}

	/** Grabs the slider and jumps to the clicked position. */
	public boolean click(double mx, double my) {
		if (!contains(mx, my)) {
			return false;
		}
		dragging = true;
		drag(mx);
		return true;
	}

	public void drag(double mx) {
		if (!dragging) {
			return;
		}
		float fraction = (float) ((mx - trackX()) / trackWidth());
		setting.setFraction(Math.max(0f, Math.min(1f, fraction)));
	}

	public void release() {
		dragging = false;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		float active = Math.max(hovered, dragging ? 1f : 0f);
		float cy = y + height / 2f;
		float tx = trackX();
		float tw = trackWidth();
		float filled = tw * setting.fraction();

		GlassRenderer.roundedRect(ctx.g(), tx, cy - TRACK_H / 2f, tw, TRACK_H, TRACK_H / 2f,
				Colors.fade(t.toggleOff(), a * 0.8f));
		if (filled > 0.5f) {
			GlassRenderer.roundedRect(ctx.g(), tx, cy - TRACK_H / 2f, filled, TRACK_H, TRACK_H / 2f,
					Colors.fade(t.accent(), a));
		}

		float knob = KNOB + active * 1.5f;
		float kx = tx + filled - knob / 2f;
		if (active > 0.05f) {
			GlassRenderer.shadow(ctx.g(), kx, cy - knob / 2f, knob, knob, knob / 2f, 6,
					Colors.fade(t.accentGlow(), 0.6f * active * a));
		}
		GlassRenderer.roundedRect(ctx.g(), kx, cy - knob / 2f, knob, knob, knob / 2f,
				Colors.fade(0xFFFFFFFF, a), Colors.fade(0xFFE8E6F5, a), GlassRenderer.CORNERS_ALL);

		N4yText.drawMiddle(ctx.g(), Integer.toString(setting.value()), x + width - LABEL_W + 6f, cy, 0.82f,
				Colors.fade(t.textPrimary(), a), false);
	}
}
