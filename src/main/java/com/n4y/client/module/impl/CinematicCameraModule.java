package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Smooth, damped camera movement (vanilla cinematic camera) for recording shots. */
public final class CinematicCameraModule extends Module {
	private boolean previous;

	public CinematicCameraModule() {
		super("cinematic_camera", ModuleCategory.VISUAL, Icons.CAMERA);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.smoothCamera;
		mc.options.smoothCamera = true;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.smoothCamera = previous;
	}
}
