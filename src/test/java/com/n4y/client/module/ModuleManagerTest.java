package com.n4y.client.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.n4y.client.gui.icon.Icons;

import net.minecraft.client.Minecraft;

class ModuleManagerTest {
	/** Test double: records the calls instead of touching the game. */
	private static final class Fake extends Module {
		int enables;
		int disables;
		int ticks;

		Fake(String id, ModuleCategory category) {
			super(id, category, Icons.DOTS);
		}

		@Override
		protected void onEnable(Minecraft mc) {
			enables++;
		}

		@Override
		protected void onDisable(Minecraft mc) {
			disables++;
		}

		@Override
		public void onTick(Minecraft mc) {
			ticks++;
		}
	}

	@Test
	void togglingCallsEnableAndDisableOnce() {
		Fake module = new Fake("zoom", ModuleCategory.VISUAL);
		assertFalse(module.isEnabled());
		module.toggle();
		module.setEnabled(true); // no-op, already enabled
		assertTrue(module.isEnabled());
		module.toggle();
		assertEquals(1, module.enables);
		assertEquals(1, module.disables);
	}

	@Test
	void filtersByCategoryAndQuery() {
		ModuleManager manager = new ModuleManager();
		manager.register(new Fake("zoom", ModuleCategory.VISUAL));
		manager.register(new Fake("auto_sprint", ModuleCategory.MOVEMENT));
		manager.register(new Fake("hide_hud", ModuleCategory.VISUAL));

		assertEquals(3, manager.filter(ModuleCategory.ALL, "").size());
		assertEquals(2, manager.filter(ModuleCategory.VISUAL, "").size());
		assertEquals(0, manager.filter(ModuleCategory.COMBAT, "").size());

		List<Module> hits = manager.filter(ModuleCategory.ALL, "  ZOOM ");
		assertEquals(1, hits.size());
		assertEquals("zoom", hits.get(0).id());
		assertEquals(1, manager.filter(ModuleCategory.VISUAL, "hud").size());
		assertEquals(0, manager.filter(ModuleCategory.MOVEMENT, "hud").size());
	}

	@Test
	void normaliseStripsAccentsAndCase() {
		assertEquals("camara cine", ModuleManager.normalise("  Cámara Cine "));
		assertEquals("anadir", ModuleManager.normalise("Añadir"));
	}

	@Test
	void duplicateIdsAreRejected() {
		ModuleManager manager = new ModuleManager();
		manager.register(new Fake("zoom", ModuleCategory.VISUAL));
		assertThrows(IllegalArgumentException.class, () -> manager.register(new Fake("zoom", ModuleCategory.MISC)));
	}

	@Test
	void enabledListTracksState() {
		ModuleManager manager = new ModuleManager();
		Fake a = new Fake("a", ModuleCategory.MISC);
		Fake b = new Fake("b", ModuleCategory.MISC);
		manager.register(a);
		manager.register(b);
		a.setEnabled(true);
		assertEquals(List.of(a), manager.enabled());
	}
}
