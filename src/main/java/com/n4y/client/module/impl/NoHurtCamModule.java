package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.Minecraft;

/** Removes the camera tilt when you take damage, which otherwise ruins a recording. */
public final class NoHurtCamModule extends Module {
	public NoHurtCamModule() {
		super("no_hurt_cam", ModuleCategory.PLAYER, Icons.HEART);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		ModuleFlags.noHurtCam = true;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		ModuleFlags.noHurtCam = false;
	}
}
