package com.n4y.client.gui.widget;

import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.setting.IntRangeSetting;

/**
 * Slider with two handles for a minimum and a maximum, used by the delays that pick a value
 * inside a range.
 */
public final class RangeSliderWidget extends N4yWidget {
	private static final float LABEL_W = 62f;
	private static final float TRACK_H = 3.5f;
	private static final float KNOB = 9f;

	private final IntRangeSetting setting;
	/** Which handle the cursor grabbed: 0 none, 1 low, 2 high. */
	private int dragging;

	public RangeSliderWidget(IntRangeSetting setting) {
		this.setting = setting;
	}

	public boolean isDragging() {
		return dragging != 0;
	}

	private float trackX() {
		return x + KNOB / 2f;
	}

	private float trackWidth() {
		return Math.max(1f, width - LABEL_W - KNOB);
	}

	/** Grabs whichever handle is closer to the click. */
	public boolean click(double mx, double my) {
		if (!contains(mx, my)) {
			return false;
		}
		float fraction = fractionAt(mx);
		dragging = Math.abs(fraction - setting.lowFraction()) <= Math.abs(fraction - setting.highFraction()) ? 1 : 2;
		drag(mx);
		return true;
	}

	public void drag(double mx) {
		float fraction = fractionAt(mx);
		if (dragging == 1) {
			setting.setLowFraction(fraction);
		} else if (dragging == 2) {
			setting.setHighFraction(fraction);
		}
	}

	public void release() {
		dragging = 0;
	}

	private float fractionAt(double mx) {
		return Math.max(0f, Math.min(1f, (float) ((mx - trackX()) / trackWidth())));
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		float cy = y + height / 2f;
		float tx = trackX();
		float tw = trackWidth();
		float low = tw * setting.lowFraction();
		float high = tw * setting.highFraction();

		GlassRenderer.roundedRect(ctx.g(), tx, cy - TRACK_H / 2f, tw, TRACK_H, TRACK_H / 2f,
				Colors.fade(t.toggleOff(), a * 0.8f));
		if (high - low > 0.5f) {
			GlassRenderer.roundedRect(ctx.g(), tx + low, cy - TRACK_H / 2f, high - low, TRACK_H, TRACK_H / 2f,
					Colors.fade(t.accent(), a));
		}
		drawKnob(ctx, tx + low, cy, hovered, a, dragging == 1);
		drawKnob(ctx, tx + high, cy, hovered, a, dragging == 2);

		String label = setting.get().min() + " - " + setting.get().max();
		N4yText.drawMiddle(ctx.g(), label, x + width - LABEL_W + 6f, cy, 0.82f, Colors.fade(t.textPrimary(), a), false);
	}

	private void drawKnob(UiContext ctx, float cx, float cy, float hovered, float a, boolean held) {
		float size = KNOB + (held ? 1.5f : hovered * 1f);
		if (held || hovered > 0.05f) {
			GlassRenderer.shadow(ctx.g(), cx - size / 2f, cy - size / 2f, size, size, size / 2f, 6,
					Colors.fade(ctx.theme().accentGlow(), 0.6f * (held ? 1f : hovered) * a));
		}
		GlassRenderer.roundedRect(ctx.g(), cx - size / 2f, cy - size / 2f, size, size, size / 2f,
				Colors.fade(0xFFFFFFFF, a), Colors.fade(0xFFE8E6F5, a), GlassRenderer.CORNERS_ALL);
	}
}
