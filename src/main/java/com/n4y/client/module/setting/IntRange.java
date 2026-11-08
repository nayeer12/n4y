package com.n4y.client.module.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A closed range of whole numbers. TriggerBot stores its delays this way and draws a new value
 * from the range before every attack.
 */
public record IntRange(int min, int max) {
	public static final Codec<IntRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("min").forGetter(IntRange::min),
			Codec.INT.fieldOf("max").forGetter(IntRange::max)
	).apply(instance, IntRange::new));

	public IntRange {
		if (max < min) {
			int swap = min;
			min = max;
			max = swap;
		}
	}

	/** A value inside the range, inclusive at both ends. */
	public int random(java.util.random.RandomGenerator random) {
		return min == max ? min : random.nextInt(min, max + 1);
	}

	public boolean contains(int value) {
		return value >= min && value <= max;
	}
}
