package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Makes sneaking a toggle instead of a held key (vanilla "Toggle Crouch"). */
public final class ToggleSneakModule extends Module {
	private boolean previous;

	public ToggleSneakModule() {
		super("toggle_sneak", ModuleCategory.PLAYER, Icons.ARROW_DOWN);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.toggleCrouch().get();
		mc.options.toggleCrouch().set(true);
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.toggleCrouch().set(previous);
	}
}
