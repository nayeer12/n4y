package com.n4y.client.core.keybind;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.n4y.client.N4y;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Global keybinds of the client. They appear in vanilla Options → Controls under
 * the "n4y Client" category, so the user can rebind them there.
 * Per-module binds are handled by the Keybind Manager (phase 4), not here.
 */
public final class N4yKeybinds {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "main"));

	private static KeyMapping openGui;
	private static Consumer<Minecraft> openGuiAction = mc -> { };

	private N4yKeybinds() {
	}

	public static void register() {
		openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.n4y.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
	}

	/** Sets what the "open GUI" key does (the screen lives in the GUI layer, not here). */
	public static void onOpenGui(Consumer<Minecraft> action) {
		openGuiAction = action;
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		while (openGui.consumeClick()) {
			if (mc.screen == null) {
				openGuiAction.accept(mc);
			}
		}
	}
}
