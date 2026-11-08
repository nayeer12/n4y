package com.n4y.client.gui.render;

import net.minecraft.client.Minecraft;

/**
 * Extra zoom applied to a whole n4y screen so the premium layout fits on small windows and
 * low GUI scales instead of being clipped.
 *
 * <p>Render-thread only ambient value: a screen sets it around its own drawing and resets it
 * afterwards. Shape and icon textures multiply it into the physical pixel size they rasterise
 * at, which keeps corners and strokes pixel-exact at any zoom.
 */
public final class UiScale {
	private static float extra = 1f;

	private UiScale() {
	}

	public static float get() {
		return extra;
	}

	public static void set(float value) {
		extra = value <= 0f ? 1f : value;
	}

	public static void reset() {
		extra = 1f;
	}

	/** GUI units to physical pixels, including the current zoom. */
	public static float physical() {
		return (float) Minecraft.getInstance().getWindow().getGuiScale() * extra;
	}
}
