package com.n4y.client.module.impl;

import java.util.function.Consumer;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

/**
 * Takes a screenshot with no HUD and no hands in the way, then puts everything back and turns
 * itself off. Bind it to a key and it works like a shutter button.
 */
public final class CleanScreenshotModule extends Module {
	/** Frames to wait after hiding, so the captured image no longer has the HUD in it. */
	private static final int SETTLE_TICKS = 2;

	private final Consumer<Component> onResult;
	private boolean previousHideGui;
	private boolean previousHideHand;
	private int countdown = -1;

	public CleanScreenshotModule(Consumer<Component> onResult) {
		super("clean_screenshot", ModuleCategory.CREATOR, Icons.CAMERA);
		this.onResult = onResult;
	}

	@Override
	protected void onEnable(Minecraft mc) {
		previousHideGui = mc.options.hideGui;
		previousHideHand = ModuleFlags.hideHand;
		mc.options.hideGui = true;
		ModuleFlags.hideHand = true;
		countdown = SETTLE_TICKS;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		restore(mc);
	}

	private void restore(Minecraft mc) {
		if (countdown >= 0) {
			mc.options.hideGui = previousHideGui;
			ModuleFlags.hideHand = previousHideHand;
			countdown = -1;
		}
	}

	@Override
	public void onTick(Minecraft mc) {
		if (countdown < 0) {
			return;
		}
		if (countdown-- > 0) {
			return;
		}
		Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), onResult);
		restore(mc);
		setEnabled(false);
	}
}
