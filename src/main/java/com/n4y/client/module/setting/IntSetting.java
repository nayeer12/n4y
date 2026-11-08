package com.n4y.client.module.setting;

import com.mojang.serialization.Codec;

/**
 * A whole number between {@code min} and {@code max}, moving in steps. Used for delays in
 * ticks, percentages and hotbar slots.
 */
public final class IntSetting extends Setting<Integer> {
	private final int min;
	private final int max;
	private final int step;

	public IntSetting(String id, int min, int max, int defaultValue, int step) {
		super(id, clamp(defaultValue, min, max, Math.max(1, step)));
		this.min = min;
		this.max = max;
		this.step = Math.max(1, step);
	}

	private static int clamp(int raw, int min, int max, int step) {
		int bounded = Math.max(min, Math.min(max, raw));
		int snapped = min + Math.round((bounded - min) / (float) step) * step;
		return Math.max(min, Math.min(max, snapped));
	}

	public int value() {
		return get();
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	public int step() {
		return step;
	}

	/** Where the value sits between min and max, as 0..1, for the slider. */
	public float fraction() {
		return max == min ? 0f : (get() - min) / (float) (max - min);
	}

	/** Sets the value from a 0..1 position on the slider. */
	public void setFraction(float fraction) {
		set(Math.round(min + fraction * (max - min)));
	}

	@Override
	protected Integer sanitise(Integer raw) {
		return clamp(raw, min, max, step);
	}

	@Override
	public Codec<Integer> codec() {
		return Codec.INT;
	}
}
