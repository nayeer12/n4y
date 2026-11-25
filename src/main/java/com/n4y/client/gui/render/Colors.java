package com.n4y.client.gui.render;

/**
 * ARGB helpers. Every color in n4y is a packed {@code 0xAARRGGBB} int; text and shapes with
 * alpha 0 are invisible in 1.21.11, so always build colors with an explicit alpha.
 */
public final class Colors {
	private Colors() {
	}

	public static int alpha(int c) {
		return c >>> 24;
	}

	public static int red(int c) {
		return (c >> 16) & 0xFF;
	}

	public static int green(int c) {
		return (c >> 8) & 0xFF;
	}

	public static int blue(int c) {
		return c & 0xFF;
	}

	public static int argb(int a, int r, int g, int b) {
		return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	/** Replaces the alpha channel; {@code alpha} in 0..1. */
	public static int withAlpha(int c, float alpha) {
		return (clamp(Math.round(alpha * 255f)) << 24) | (c & 0x00FFFFFF);
	}

	/** Multiplies the existing alpha by {@code factor} (0..1), used for fades. */
	public static int fade(int c, float factor) {
		return (clamp(Math.round(alpha(c) * factor)) << 24) | (c & 0x00FFFFFF);
	}

	/** Linear interpolation of all four channels, {@code t} clamped to 0..1. */
	/**
	 * The largest difference between two colours across alpha, red, green and blue.
	 *
	 * <p>Used to decide whether a gradient between them is worth drawing as a gradient at all:
	 * below a few units per channel the eye cannot tell it from a flat fill.
	 */
	public static int maxChannelDelta(int a, int b) {
		int delta = Math.abs(alpha(a) - alpha(b));
		delta = Math.max(delta, Math.abs(red(a) - red(b)));
		delta = Math.max(delta, Math.abs(green(a) - green(b)));
		return Math.max(delta, Math.abs(blue(a) - blue(b)));
	}

	public static int lerp(int from, int to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		return argb(
				Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
				Math.round(red(from) + (red(to) - red(from)) * t),
				Math.round(green(from) + (green(to) - green(from)) * t),
				Math.round(blue(from) + (blue(to) - blue(from)) * t));
	}

	/** Mixes towards white ({@code amount} > 0) or black ({@code amount} < 0), keeping alpha. */
	public static int shade(int c, float amount) {
		int target = amount >= 0 ? (c & 0xFF000000) | 0xFFFFFF : c & 0xFF000000;
		return lerp(c, target, Math.abs(amount));
	}

	private static int clamp(int v) {
		return v < 0 ? 0 : Math.min(v, 255);
	}
}
