package com.n4y.client.core.keybind;

import java.util.HashSet;
import java.util.Set;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;

/**
 * Per-module keys. n4y stores its own binds in its profile instead of vanilla's options file,
 * so the keys travel with the profile, and reads the keyboard directly each tick rather than
 * registering one vanilla key mapping per module.
 *
 * <p>Only the moment a key goes down counts, so holding it does not toggle repeatedly, and
 * nothing fires while a screen is open.
 */
public final class KeybindManager {
	/** Where the input states come from; swapped in the tests. */
	@FunctionalInterface
	public interface InputState {
		boolean isDown(InputBinding binding);
	}

	private final ModuleManager modules;
	private final InputState input;
	private final Set<InputBinding> held = new HashSet<>();

	public KeybindManager(ModuleManager modules, InputState input) {
		this.modules = modules;
		this.input = input;
	}

	/** Reads the real keyboard and mouse of the running game. */
	public static KeybindManager forGame(ModuleManager modules, Minecraft mc) {
		return new KeybindManager(modules, binding -> binding.isDown(mc.getWindow()));
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
			InputBinding key = module.key();
			boolean down = input.isDown(key);
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
}
