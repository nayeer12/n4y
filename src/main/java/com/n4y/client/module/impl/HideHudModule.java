package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Hides the vanilla HUD (hotbar, hearts, chat) for clean screenshots and video. */
public final class HideHudModule extends Module {
	private boolean previous;

	public HideHudModule() {
		super("hide_hud", ModuleCategory.VISUAL, Icons.LAYOUT);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.hideGui;
		mc.options.hideGui = true;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.hideGui = previous;
	}
}
