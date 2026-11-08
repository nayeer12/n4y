package com.n4y.client.gui.hud;

import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A module that draws something on the screen while enabled. The manager works out where it
 * goes from its {@link HudPlacement}; the module only draws itself at the given corner.
 */
public abstract class HudModule extends Module {
	private final HudPlacement defaultPlacement;
	private HudPlacement placement;

	protected HudModule(String id, ModuleCategory category, Icon icon, HudPlacement defaultPlacement) {
		super(id, category, icon);
		this.defaultPlacement = defaultPlacement;
		this.placement = defaultPlacement;
	}

	public final HudPlacement defaultPlacement() {
		return defaultPlacement;
	}

	public final boolean isMoved() {
		return !placement.equals(defaultPlacement);
	}

	public final void resetPlacement() {
		this.placement = defaultPlacement;
	}

	public final HudPlacement placement() {
		return placement;
	}

	public final void setPlacement(HudPlacement value) {
		this.placement = value;
	}

	/** False for elements that must stay where they are, such as the hit marker. */
	public boolean movable() {
		return true;
	}

	public abstract float width(Minecraft mc);

	public abstract float height(Minecraft mc);

	/** Draws the element with its top-left corner at {@code (x, y)}. */
	public abstract void drawHud(GuiGraphics g, Minecraft mc, Theme theme, float x, float y);
}
