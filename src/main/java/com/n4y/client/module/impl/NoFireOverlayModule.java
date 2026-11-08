package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.Minecraft;

/** Clears the flames drawn across the screen while you burn, so you can still see. */
public final class NoFireOverlayModule extends Module {
	public NoFireOverlayModule() {
		super("no_fire_overlay", ModuleCategory.VISUAL, Icons.FLAME);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		ModuleFlags.noFireOverlay = true;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		ModuleFlags.noFireOverlay = false;
	}
}
