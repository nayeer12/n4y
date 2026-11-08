package com.n4y.client.core.keybind;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;

class KeybindManagerTest {
	private static final InputBinding KEY_V = InputBinding.key(86);

	private static final class Fake extends Module {
		Fake(String id) {
			super(id, ModuleCategory.MISC, Icons.DOTS);
		}

		@Override
		protected void onEnable(Minecraft mc) {
		}

		@Override
		protected void onDisable(Minecraft mc) {
		}
	}

	private static Fake boundModule(ModuleManager modules) {
		Fake module = new Fake("zoom");
		modules.register(module);
		module.setKey(KEY_V);
		return module;
	}

	@Test
	void togglesOnceWhileTheKeyIsHeld() {
		Set<InputBinding> down = new HashSet<>();
		ModuleManager modules = new ModuleManager();
		Fake module = boundModule(modules);
		KeybindManager keybinds = new KeybindManager(modules, down::contains);

		keybinds.tick(true);
		assertFalse(module.isEnabled());

		down.add(KEY_V);
		keybinds.tick(true);
		assertTrue(module.isEnabled(), "pressing the key did not toggle the module");
		keybinds.tick(true);
		keybinds.tick(true);
		assertTrue(module.isEnabled(), "holding the key toggled it again");

		down.remove(KEY_V);
		keybinds.tick(true);
		down.add(KEY_V);
		keybinds.tick(true);
		assertFalse(module.isEnabled(), "pressing it again did not toggle it back");
	}

	@Test
	void ignoresKeysWhileAScreenIsOpen() {
		Set<InputBinding> down = new HashSet<>();
		ModuleManager modules = new ModuleManager();
		Fake module = boundModule(modules);
		KeybindManager keybinds = new KeybindManager(modules, down::contains);

		down.add(KEY_V);
		keybinds.tick(false);
		assertFalse(module.isEnabled(), "a key pressed inside a screen toggled a module");

		keybinds.tick(true);
		assertFalse(module.isEnabled(), "the key held while the screen closed fired on its own");

		down.remove(KEY_V);
		keybinds.tick(true);
		down.add(KEY_V);
		keybinds.tick(true);
		assertTrue(module.isEnabled());
	}

	@Test
	void unboundModulesAreIgnored() {
		ModuleManager modules = new ModuleManager();
		Fake module = new Fake("zoom");
		modules.register(module);
		KeybindManager keybinds = new KeybindManager(modules, binding -> true);
		keybinds.tick(true);
		assertFalse(module.isEnabled());
	}
}
