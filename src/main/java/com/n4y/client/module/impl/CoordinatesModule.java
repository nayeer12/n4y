package com.n4y.client.module.impl;

import com.n4y.client.core.Numbers;
import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.hud.PanelHudModule;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Position and facing on screen, without opening the debug overlay. */
public final class CoordinatesModule extends PanelHudModule {
	/** Ten times a second: smooth to read, and a tiny fraction of the frames at high FPS. */
	private static final long REFRESH_MILLIS = 100L;
	private static final String PLACEHOLDER = "-";

	/** The facing line only changes when the player turns, so it is built from the direction. */
	private Direction lastDirection;
	private String facingLine = PLACEHOLDER;

	public CoordinatesModule() {
		super("coordinates", ModuleCategory.MISC, Icons.PIN,
				HudPlacement.of(HudAnchor.TOP_LEFT, 6f, 6f), 2, REFRESH_MILLIS);
	}

	@Override
	protected void refresh(Minecraft mc) {
		Player player = mc == null ? null : mc.player;
		if (player == null) {
			set(0, PLACEHOLDER);
			set(1, PLACEHOLDER);
			lastDirection = null;
			return;
		}
		StringBuilder position = line();
		Numbers.appendFixed1(position, player.getX());
		position.append("  ");
		Numbers.appendFixed1(position, player.getY());
		position.append("  ");
		Numbers.appendFixed1(position, player.getZ());
		set(0, position);
		set(1, facing(player.getDirection()));
	}

	/**
	 * The translated facing line. Resolving a translation builds a component and walks it, so the
	 * result is kept until the player actually turns to face somewhere else.
	 */
	private String facing(Direction direction) {
		if (direction != lastDirection) {
			lastDirection = direction;
			facingLine = Component.translatable("n4y.hud.facing",
					Component.translatable("n4y.direction." + direction.getSerializedName()).getString()).getString();
		}
		return facingLine;
	}
}
