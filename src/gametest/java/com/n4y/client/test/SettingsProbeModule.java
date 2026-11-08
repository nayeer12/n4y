package com.n4y.client.test;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntRangeSetting;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * A module that exists only for the in-game tests: one setting of every kind, so the settings
 * panel, the sliders and the persistence can be exercised before any combat module exists.
 * It is part of the test source set and never ships with the client.
 */
public final class SettingsProbeModule extends Module {
	final BooleanSetting flag = new BooleanSetting("probe_flag", false);
	final IntSetting number = new IntSetting("probe_number", 0, 20, 0, 1);
	final IntRangeSetting range = new IntRangeSetting("probe_range", 0, 1000, 540, 550);
	final KeySetting activateKey = new KeySetting("probe_key", InputBinding.mouse(1));

	public SettingsProbeModule() {
		super("probe", ModuleCategory.MISC, Icons.DOTS);
		addSettings(flag, number, range, activateKey);
	}

	@Override
	public Component displayName() {
		return Component.literal("Settings Probe");
	}

	@Override
	public Component description() {
		return Component.literal("One setting of every kind.");
	}

	@Override
	protected void onEnable(Minecraft mc) {
	}

	@Override
	protected void onDisable(Minecraft mc) {
	}
}
