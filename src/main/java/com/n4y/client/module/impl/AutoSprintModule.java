package com.n4y.client.module.impl;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Keeps the player sprinting whenever they walk forward and are able to sprint. */
public final class AutoSprintModule extends Module {
	public AutoSprintModule() {
		super("auto_sprint", ModuleCategory.MOVEMENT, Icons.RUN);
	}

	@Override
	public void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.screen != null) {
			return;
		}
		boolean canSprint = player.input.hasForwardImpulse()
				&& !player.isSprinting()
				&& !player.isShiftKeyDown()
				&& !player.isUsingItem()
				&& !player.horizontalCollision
				&& (player.getFoodData().hasEnoughFood() || player.getAbilities().mayfly);
		if (canSprint) {
			player.setSprinting(true);
		}
	}
}
