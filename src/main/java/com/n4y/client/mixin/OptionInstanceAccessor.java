package com.n4y.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.OptionInstance;

/**
 * Writes an option's value past its validation. Needed for Fullbright: the gamma slider is
 * clamped to 0..1, and a brightness above that is exactly what the module is for.
 *
 * <p>Only used for gamma, whose change callback does nothing, so nothing else is bypassed.
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
	@Accessor("value")
	void n4y$setValue(T value);
}
