package com.n4y.client.module.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.Minecraft;

/**
 * One switch for recording: hides the HUD and the hands, steadies the camera and stops the
 * view bobbing. Turning it off puts every one of those modules back exactly as it found them.
 */
public final class CinematicModeModule extends Module {
	private static final List<String> PARTS = List.of("hide_hud", "hide_hand", "no_view_bobbing", "cinematic_camera");

	private final ModuleManager manager;
	private final Map<String, Boolean> before = new LinkedHashMap<>();

	public CinematicModeModule(ModuleManager manager) {
		super("cinematic_mode", ModuleCategory.CREATOR, Icons.FILM);
		this.manager = manager;
	}

	/** The modules this one drives, so the interface can explain what it will change. */
	public List<String> parts() {
		return PARTS;
	}

	@Override
	protected void onEnable(Minecraft mc) {
		before.clear();
		for (String id : PARTS) {
			manager.byId(id).ifPresent(module -> {
				before.put(id, module.isEnabled());
				module.setEnabled(true);
			});
		}
	}

	@Override
	protected void onDisable(Minecraft mc) {
		for (String id : PARTS) {
			manager.byId(id).ifPresent(module -> module.setEnabled(before.getOrDefault(id, false)));
		}
		before.clear();
	}
}
