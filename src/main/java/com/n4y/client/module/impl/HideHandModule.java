package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.Minecraft;

/** Hides the first-person hands and whatever they hold. */
public final class HideHandModule extends Module {
	public HideHandModule() {
		super("hide_hand", ModuleCategory.CREATOR, Icons.HAND);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		ModuleFlags.hideHand = true;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		ModuleFlags.hideHand = false;
	}
}
