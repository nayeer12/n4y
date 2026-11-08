package com.n4y.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntRange;
import com.n4y.client.module.setting.IntRangeSetting;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;

import net.minecraft.client.Minecraft;

class SettingPersistenceTest {
	/** A module with one setting of every kind, standing in for a combat module. */
	private static final class Probe extends Module {
		final BooleanSetting flag = new BooleanSetting("probe_flag", false);
		final IntSetting number = new IntSetting("probe_number", 0, 20, 0, 1);
		final IntRangeSetting range = new IntRangeSetting("probe_range", 0, 1000, 540, 550);
		final KeySetting key = new KeySetting("probe_key", InputBinding.mouse(1));

		Probe() {
			super("probe", ModuleCategory.MISC, Icons.DOTS);
			addSettings(flag, number, range, key);
		}

		@Override
		protected void onEnable(Minecraft mc) {
		}

		@Override
		protected void onDisable(Minecraft mc) {
		}
	}

	private record Setup(ConfigManager config, Probe probe, ThemeEngine themes) {
	}

	private Setup open(Path root) {
		ThemeEngine themes = new ThemeEngine();
		ModuleManager modules = new ModuleManager();
		Probe probe = new Probe();
		modules.register(probe);
		ConfigManager config = new ConfigManager(root, themes, modules);
		modules.onChange(config::markDirty);
		config.loadFromDisk();
		config.applyToGame();
		return new Setup(config, probe, themes);
	}

	@Test
	void everySettingKindSurvivesAReload(@TempDir Path root) {
		Setup first = open(root);
		first.probe().flag.set(true);
		first.probe().number.set(7);
		first.probe().range.set(new IntRange(100, 200));
		first.probe().key.set(com.n4y.client.core.input.InputBinding.key(86));
		first.config().markDirty();
		first.config().flush();

		Setup second = open(root);
		assertTrue(second.probe().flag.value());
		assertEquals(7, second.probe().number.value());
		assertEquals(new IntRange(100, 200), second.probe().range.get());
		assertEquals(InputBinding.key(86), second.probe().key.binding());
		second.config().shutdown();
	}

	@Test
	void untouchedSettingsAreNotWritten(@TempDir Path root) throws Exception {
		Setup setup = open(root);
		setup.probe().number.set(5);
		setup.config().markDirty();
		setup.config().flush();

		String json = Files.readString(root.resolve("profiles/default.json"));
		assertTrue(json.contains("probe_number"), "the changed setting was not saved");
		assertFalse(json.contains("probe_flag"), "a setting left at its default was written anyway");
		setup.config().shutdown();
	}

	@Test
	void aProfileFromBeforeSettingsStillLoads(@TempDir Path root) throws Exception {
		Files.createDirectories(root.resolve("profiles"));
		Files.writeString(root.resolve("client.json"), "{\"activeProfile\": \"default\"}");
		Files.writeString(root.resolve("profiles/default.json"), """
				{
				  "name": "Default",
				  "theme": "green",
				  "modules": { "probe": { "enabled": true, "key": 86 } }
				}
				""");

		Setup setup = open(root);
		assertTrue(setup.probe().isEnabled(), "the old profile lost its enabled module");
		assertEquals(InputBinding.key(86), setup.probe().key(), "the old bare key code was not read");
		assertEquals("green", setup.themes().selected().id());
		assertEquals(0, setup.probe().number.value(), "settings missing from the file should keep their defaults");
		setup.config().shutdown();
	}

	@Test
	void aCorruptSettingDoesNotBreakTheProfile(@TempDir Path root) throws Exception {
		Files.createDirectories(root.resolve("profiles"));
		Files.writeString(root.resolve("profiles/default.json"), """
				{
				  "name": "Default",
				  "modules": { "probe": { "enabled": true, "settings": { "probe_number": "nonsense" } } }
				}
				""");

		Setup setup = open(root);
		assertTrue(setup.probe().isEnabled(), "one bad value stopped the whole profile from loading");
		assertEquals(0, setup.probe().number.value(), "a corrupt value should fall back to the default");
		setup.config().shutdown();
	}
}
