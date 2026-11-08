package com.n4y.client.module.setting;

import java.util.concurrent.ThreadLocalRandom;

import com.mojang.serialization.Codec;

/**
 * A minimum and a maximum the player can drag apart, bounded by {@code min}..{@code max}.
 * Asking for {@link #randomValue()} gives a fresh number inside the chosen range.
 */
public final class IntRangeSetting extends Setting<IntRange> {
	private final int min;
	private final int max;

	public IntRangeSetting(String id, int min, int max, int defaultMin, int defaultMax) {
		super(id, clamp(new IntRange(defaultMin, defaultMax), min, max));
		this.min = min;
		this.max = max;
	}

	private static IntRange clamp(IntRange raw, int min, int max) {
		return new IntRange(
				Math.max(min, Math.min(max, raw.min())),
				Math.max(min, Math.min(max, raw.max())));
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	public int randomValue() {
		return get().random(ThreadLocalRandom.current());
	}

	/** Position of the range ends as 0..1, for the slider. */
	public float lowFraction() {
		return fractionOf(get().min());
	}

	public float highFraction() {
		return fractionOf(get().max());
	}

	private float fractionOf(int value) {
		return max == min ? 0f : (value - min) / (float) (max - min);
	}

	public void setLowFraction(float fraction) {
		int value = Math.round(min + fraction * (max - min));
		set(new IntRange(Math.min(value, get().max()), get().max()));
	}

	public void setHighFraction(float fraction) {
		int value = Math.round(min + fraction * (max - min));
		set(new IntRange(get().min(), Math.max(value, get().min())));
	}

	@Override
	protected IntRange sanitise(IntRange raw) {
		return clamp(raw, min, max);
	}

	@Override
	public Codec<IntRange> codec() {
		return IntRange.CODEC;
	}
}
