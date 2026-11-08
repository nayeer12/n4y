package com.n4y.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;

class ConfigManagerTest {
	/** Module that records nothing but its own state, so no game instance is needed. */
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

	@TempDir
	Path root;

	private ThemeEngine themes;
	private ModuleManager modules;
	private ConfigManager config;
	private Fake zoom;
	private Fake hud;

	@BeforeEach
	void setUp() {
		themes = new ThemeEngine();
		modules = new ModuleManager();
		zoom = new Fake("zoom");
		hud = new Fake("hide_hud");
		modules.register(zoom);
		modules.register(hud);
		config = new ConfigManager(root, themes, modules);
		modules.onChange(config::markDirty);
		themes.onChange(config::markDirty);
		config.loadFromDisk();
		config.applyToGame();
	}

	@Test
	void savesAndReloadsThemeAndModules() {
		zoom.setEnabled(true);
		zoom.setKey(86); // V
		themes.select(Themes.GREEN);
		config.markDirty();
		config.flush();

		assertTrue(Files.isRegularFile(root.resolve("client.json")), "client.json was not written");
		assertTrue(Files.isRegularFile(root.resolve("profiles/default.json")), "the profile was not written");

		ThemeEngine freshThemes = new ThemeEngine();
		ModuleManager freshModules = new ModuleManager();
		Fake freshZoom = new Fake("zoom");
		freshModules.register(freshZoom);
		freshModules.register(new Fake("hide_hud"));
		ConfigManager reloaded = new ConfigManager(root, freshThemes, freshModules);
		reloaded.loadFromDisk();
		reloaded.applyToGame();

		assertTrue(freshZoom.isEnabled(), "the enabled module was not restored");
		assertEquals(86, freshZoom.key(), "the key bind was not restored");
		assertEquals(Themes.GREEN.id(), freshThemes.selected().id(), "the theme was not restored");
		reloaded.shutdown();
	}

	@Test
	void profilesAreIndependent() {
		zoom.setEnabled(true);
		themes.select(Themes.BLUE);
		config.markDirty();

		assertTrue(config.createProfile("Grabación"), "the second profile was not created");
		assertEquals("Grabación", config.activeProfileName());

		hud.setEnabled(true);
		themes.select(Themes.PINK);
		config.markDirty();
		config.flush();

		assertTrue(config.profileNames().containsAll(java.util.List.of("Default", "Grabación")));

		assertTrue(config.switchProfile("Default"), "could not switch back");
		assertTrue(zoom.isEnabled(), "the first profile lost its module state");
		assertFalse(hud.isEnabled(), "state leaked from the other profile");
		assertEquals(Themes.BLUE.id(), themes.selected().id(), "the first profile lost its theme");

		assertTrue(config.switchProfile("Grabación"));
		assertTrue(hud.isEnabled());
		assertEquals(Themes.PINK.id(), themes.selected().id());
		config.flush();
	}

	@Test
	void rejectsBadProfileNamesAndKeepsTheActiveOne() {
		assertFalse(config.createProfile("   "), "a blank name was accepted");
		assertFalse(config.createProfile("***"), "a name with no usable characters was accepted");
		assertFalse(config.createProfile("Default"), "the active profile was duplicated");
		assertTrue(config.createProfile("PvP"));
		assertFalse(config.deleteProfile("PvP"), "the active profile must not be deletable");
		assertTrue(config.switchProfile("Default"));
		assertTrue(config.deleteProfile("PvP"));
		assertFalse(config.profileNames().contains("PvP"));
		config.flush();
	}

	@Test
	void unreadableConfigFallsBackToDefaults() throws Exception {
		Files.createDirectories(root.resolve("profiles"));
		Files.writeString(root.resolve("client.json"), "{ this is not json");
		Files.writeString(root.resolve("profiles/default.json"), "{ \"theme\": 12 }");

		ConfigManager broken = new ConfigManager(root, new ThemeEngine(), new ModuleManager());
		broken.loadFromDisk();
		broken.applyToGame();
		assertEquals("Default", broken.activeProfileName());
		broken.shutdown();
	}
}
