package com.n4y.client.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;

class CinematicModeModuleTest {
	private static final class Fake extends Module {
		Fake(String id) {
			super(id, ModuleCategory.VISUAL, Icons.DOTS);
		}

		@Override
		protected void onEnable(Minecraft mc) {
		}

		@Override
		protected void onDisable(Minecraft mc) {
		}
	}

	private ModuleManager manager;
	private CinematicModeModule cinematic;

	@BeforeEach
	void setUp() {
		manager = new ModuleManager();
		cinematic = new CinematicModeModule(manager);
		manager.register(cinematic);
		for (String id : cinematic.parts()) {
			manager.register(new Fake(id));
		}
	}

	@Test
	void turnsEveryPartOn() {
		cinematic.setEnabled(true);
		for (String id : cinematic.parts()) {
			assertTrue(manager.byId(id).orElseThrow().isEnabled(), id + " was not turned on");
		}
	}

	@Test
	void givesBackExactlyWhatItFound() {
		Module alreadyOn = manager.byId("hide_hud").orElseThrow();
		alreadyOn.setEnabled(true);

		cinematic.setEnabled(true);
		cinematic.setEnabled(false);

		assertTrue(alreadyOn.isEnabled(), "a module the player had on was switched off");
		assertFalse(manager.byId("hide_hand").orElseThrow().isEnabled(), "a module it turned on stayed on");
		assertFalse(manager.byId("no_view_bobbing").orElseThrow().isEnabled());
		assertFalse(manager.byId("cinematic_camera").orElseThrow().isEnabled());
	}

	@Test
	void missingPartsAreIgnored() {
		ModuleManager empty = new ModuleManager();
		CinematicModeModule lonely = new CinematicModeModule(empty);
		empty.register(lonely);
		lonely.setEnabled(true);
		lonely.setEnabled(false);
		assertFalse(lonely.isEnabled());
	}
}
