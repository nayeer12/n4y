package com.n4y.client.gui.hud;

import java.util.ArrayList;
import java.util.List;

import com.n4y.client.N4y;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.ThemeEngine;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * Draws every enabled {@link HudModule} and the notification toasts, as one Fabric HUD
 * element. Elements are placed from their anchor, so a layout keeps working at any window
 * size, and the whole thing disappears with the vanilla HUD for clean screenshots.
 */
public final class HudManager {
	private final ModuleManager modules;
	private final ThemeEngine themes;
	private final NotificationCenter notifications = new NotificationCenter();
	/** Set while the HUD Studio is open, which draws the elements itself. */
	private boolean suppressed;
	private List<HudModule> cachedElements;

	public HudManager(ModuleManager modules, ThemeEngine themes) {
		this.modules = modules;
		this.themes = themes;
	}

	public NotificationCenter notifications() {
		return notifications;
	}

	public void setSuppressed(boolean value) {
		this.suppressed = value;
	}

	/**
	 * Every HUD element the client knows about, enabled or not. The list is built once: the
	 * modules are registered at start-up and this runs on every frame.
	 */
	public List<HudModule> elements() {
		if (cachedElements == null) {
			List<HudModule> out = new ArrayList<>();
			for (Module module : modules.all()) {
				if (module instanceof HudModule hud) {
					out.add(hud);
				}
			}
			cachedElements = List.copyOf(out);
		}
		return cachedElements;
	}

	public void register() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "hud"), (g, delta) -> render(g));
	}

	private void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		long now = System.nanoTime();
		if (suppressed) {
			// The HUD Studio draws the elements itself and wants a clean screen.
			return;
		}
		Theme theme = themes.current(now);
		drawElements(g, mc, theme);
		// While the n4y screen is open it draws the toasts itself, on top of the panel.
		if (!(mc.screen instanceof com.n4y.client.gui.screen.N4yMainScreen)) {
			notifications.render(g, theme, g.guiWidth(), now);
		}
	}

	/** Draws the enabled elements; also used by the HUD Studio to preview the layout. */
	public void drawElements(GuiGraphics g, Minecraft mc, Theme theme) {
		for (HudModule element : elements()) {
			if (!element.isEnabled()) {
				continue;
			}
			float w = element.width(mc);
			float h = element.height(mc);
			float x = element.placement().screenX(g.guiWidth(), w);
			float y = element.placement().screenY(g.guiHeight(), h);
			element.drawHud(g, mc, theme, x, y);
		}
	}
}
