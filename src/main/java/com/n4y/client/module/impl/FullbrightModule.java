package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.mixin.OptionInstanceAccessor;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/**
 * Lights every cave as if it were daylight by pushing the gamma far past the slider's limit,
 * then puts the player's own brightness back when it is turned off.
 */
public final class FullbrightModule extends Module {
	private static final double BRIGHT = 15.0;
	private double previous = 0.5;

	public FullbrightModule() {
		super("fullbright", ModuleCategory.VISUAL, Icons.SUN);
	}

	@SuppressWarnings("unchecked")
	private static void setGamma(Minecraft mc, double value) {
		((OptionInstanceAccessor<Double>) (Object) mc.options.gamma()).n4y$setValue(value);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.gamma().get();
		setGamma(mc, BRIGHT);
	}

	@Override
	protected void onDisable(Minecraft mc) {
		setGamma(mc, previous);
	}
}
