package com.n4y.client.core.keybind;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Per-module keys. n4y stores its own binds in its profile instead of vanilla's options file,
 * so the keys travel with the profile, and reads the keyboard directly each tick rather than
 * registering one vanilla key mapping per module.
 *
 * <p>Only the moment a key goes down counts, so holding it does not toggle repeatedly, and
 * nothing fires while a screen is open.
 */
public final class KeybindManager {
	/** Where the key states come from; swapped in the tests. */
	@FunctionalInterface
	public interface KeyState {
		boolean isDown(int keyCode);
	}

	private final ModuleManager modules;
	private final KeyState keys;
	private final Set<Integer> held = new HashSet<>();

	public KeybindManager(ModuleManager modules, KeyState keys) {
		this.modules = modules;
		this.keys = keys;
	}

	/** Reads the real keyboard of the running game. */
	public static KeybindManager forGame(ModuleManager modules, Minecraft mc) {
		return new KeybindManager(modules, key -> InputConstants.isKeyDown(mc.getWindow(), key));
	}

	/**
	 * @param allowed false while a screen is open: key states are still tracked, so a key held
	 *                during a menu does not fire the moment the menu closes
	 */
	public void tick(boolean allowed) {
		for (Module module : modules.all()) {
			if (!module.hasKey()) {
				continue;
			}
			int key = module.key();
			boolean down = keys.isDown(key);
			boolean wasDown = held.contains(key);
			if (down && !wasDown && allowed) {
				module.toggle();
			}
			if (down) {
				held.add(key);
			} else {
				held.remove(key);
			}
		}
	}

	/** Human readable name of a key code, e.g. {@code V} or {@code Left Shift}. */
	public static Component displayName(int keyCode) {
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName();
	}
}
