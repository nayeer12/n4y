package com.n4y.client.gui.icon;

/**
 * Coverage maths for {@link Icon}s, free of Minecraft classes so it can be unit tested.
 * Distances are computed in pixel space, which gives smooth anti-aliasing and round caps.
 */
public final class IconRaster {
	/** Smallest half-stroke in pixels, so icons stay visible at small sizes. */
	private static final float MIN_HALF = 0.55f;

	private IconRaster() {
	}

	/** Coverage 0..1 of pixel {@code (x, y)} for {@code icon} rasterised into {@code size} pixels. */
	public static float coverage(Icon icon, int x, int y, int size) {
		float scale = size / Icon.SPACE;
		float px = x + 0.5f;
		float py = y + 0.5f;
		float half = Math.max(MIN_HALF, icon.stroke() * 0.5f * scale);
		float best = 0f;

		for (float[] poly : icon.polylines()) {
			float d = Float.MAX_VALUE;
			for (int i = 0; i + 3 < poly.length; i += 2) {
				d = Math.min(d, segmentDistance(px, py,
						poly[i] * scale, poly[i + 1] * scale, poly[i + 2] * scale, poly[i + 3] * scale));
			}
			if (poly.length == 2) {
				d = distance(px, py, poly[0] * scale, poly[1] * scale);
			}
			best = Math.max(best, clamp01(0.5f + half - d));
		}
		for (float[] ring : icon.rings()) {
			float d = Math.abs(distance(px, py, ring[0] * scale, ring[1] * scale) - ring[2] * scale);
			best = Math.max(best, clamp01(0.5f + half - d));
		}
		for (float[] dot : icon.dots()) {
			float d = distance(px, py, dot[0] * scale, dot[1] * scale) - dot[2] * scale;
			best = Math.max(best, clamp01(0.5f - d));
		}
		return best;
	}

	static float distance(float px, float py, float x, float y) {
		float dx = px - x;
		float dy = py - y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	static float segmentDistance(float px, float py, float x0, float y0, float x1, float y1) {
		float vx = x1 - x0;
		float vy = y1 - y0;
		float len2 = vx * vx + vy * vy;
		if (len2 < 1e-6f) {
			return distance(px, py, x0, y0);
		}
		float t = ((px - x0) * vx + (py - y0) * vy) / len2;
		t = t < 0f ? 0f : Math.min(t, 1f);
		return distance(px, py, x0 + t * vx, y0 + t * vy);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}
}
