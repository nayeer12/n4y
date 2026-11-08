package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Stops the camera bobbing while walking: steadier footage for recording. */
public final class NoViewBobbingModule extends Module {
	private boolean previous;

	public NoViewBobbingModule() {
		super("no_view_bobbing", ModuleCategory.VISUAL, Icons.WAVE);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.bobView().get();
		mc.options.bobView().set(false);
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.bobView().set(previous);
	}
}
