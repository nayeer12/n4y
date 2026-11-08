package com.n4y.client.gui.render;

/**
 * Pure coverage math for the procedural shape textures (kept free of Minecraft classes so it
 * can be unit tested). All shapes are a square of {@code size} pixels containing a rounded
 * rectangle; pixel centres are sampled at {@code +0.5}.
 */
public final class ShapeMath {
	private ShapeMath() {
	}

	/** Signed distance from point to a rounded box centred at {@code c} with half extent {@code half}. */
	public static float roundedBoxSdf(float px, float py, float c, float half, float radius) {
		float qx = Math.abs(px - c) - (half - radius);
		float qy = Math.abs(py - c) - (half - radius);
		float ox = Math.max(qx, 0f);
		float oy = Math.max(qy, 0f);
		float outside = (float) Math.sqrt(ox * ox + oy * oy);
		float inside = Math.min(Math.max(qx, qy), 0f);
		return outside + inside - radius;
	}

	/** Anti-aliased coverage (0..1) of a filled rounded square of radius {@code r} in a {@code 2r+1} texture. */
	public static float fill(int x, int y, int r) {
		int size = 2 * r + 1;
		float sdf = roundedBoxSdf(x + 0.5f, y + 0.5f, size / 2f, size / 2f, r);
		return clamp01(0.5f - sdf);
	}

	/** Coverage of a ring (outline) of thickness {@code t} pixels. */
	public static float ring(int x, int y, int r, int t) {
		int size = 2 * r + 1;
		float sdf = roundedBoxSdf(x + 0.5f, y + 0.5f, size / 2f, size / 2f, r);
		return clamp01(0.5f - sdf) - clamp01(0.5f - (sdf + t));
	}

	/**
	 * Soft shadow intensity: 1 inside the rounded rect (inset by {@code blur}) and a smooth
	 * fall-off to 0 over {@code blur} pixels outside. Texture size is {@code 2(r+blur)+1}.
	 */
	public static float shadow(int x, int y, int r, int blur) {
		int size = 2 * (r + blur) + 1;
		float sdf = roundedBoxSdf(x + 0.5f, y + 0.5f, size / 2f, size / 2f - blur, Math.max(r, 0));
		if (sdf <= 0f) {
			return 1f;
		}
		float t = clamp01(1f - sdf / Math.max(blur, 1));
		return t * t * (3f - 2f * t) * t; // smoothstep, squared tail for a softer edge
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}
}
