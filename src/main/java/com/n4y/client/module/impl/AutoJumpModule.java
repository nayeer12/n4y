package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

/** Turns on vanilla auto jump while active and restores the previous setting afterwards. */
public final class AutoJumpModule extends Module {
	private boolean previous;

	public AutoJumpModule() {
		super("auto_jump", ModuleCategory.MOVEMENT, Icons.ARROW_UP);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previous = mc.options.autoJump().get();
		mc.options.autoJump().set(true);
	}

	@Override
	protected void onDisable(Minecraft mc) {
		mc.options.autoJump().set(previous);
	}
}
