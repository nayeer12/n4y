package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Narrows the field of view to 30° for a telescopic view; restores the user's FOV when off. */
public final class ZoomModule extends Module {
	private static final int ZOOM_FOV = 30;
	private int previous = 70;

	public ZoomModule() {
		super("zoom", ModuleCategory.VISUAL, Icons.ZOOM);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.fov().get();
		mc.options.fov().set(ZOOM_FOV);
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.fov().set(previous);
	}
}
