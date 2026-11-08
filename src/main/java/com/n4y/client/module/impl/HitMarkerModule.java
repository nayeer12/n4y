package com.n4y.client.module.impl;

import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.ModuleCategory;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionResult;

/**
 * Flashes four short strokes around the crosshair when a hit lands, the visual feedback a
 * recording needs. It only draws: it never changes the hit itself.
 */
public final class HitMarkerModule extends HudModule {
	private static final float SIZE = 13f;
	private static final long FLASH_MILLIS = 260L;

	private long lastHit = Long.MIN_VALUE;

	public HitMarkerModule() {
		super("hit_marker", ModuleCategory.COMBAT, Icons.CROSSHAIR, HudPlacement.of(HudAnchor.CENTER, 0f, 0f));
	}

	/** Hooks the vanilla attack event once, at start-up. */
	public void listen() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (isEnabled() && level.isClientSide()) {
				lastHit = System.currentTimeMillis();
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	public boolean movable() {
		return false;
	}

	@Override
	public float width(Minecraft mc) {
		return SIZE;
	}

	@Override
	public float height(Minecraft mc) {
		return SIZE;
	}

	@Override
	public void drawHud(GuiGraphics g, Minecraft mc, Theme theme, float x, float y) {
		float age = System.currentTimeMillis() - lastHit;
		if (age > FLASH_MILLIS || age < 0) {
			return;
		}
		float fade = 1f - age / FLASH_MILLIS;
		int color = Colors.fade(0xFFFFFFFF, fade);
		float cx = x + SIZE / 2f;
		float cy = y + SIZE / 2f;
		float inner = 2.5f;
		float outer = SIZE / 2f;
		for (int i = 0; i < 4; i++) {
			float dx = (i == 0 || i == 3) ? -1f : 1f;
			float dy = (i < 2) ? -1f : 1f;
			stroke(g, cx + dx * inner, cy + dy * inner, dx, dy, outer - inner, color);
		}
	}

	/** One diagonal tick of the marker, drawn as a few short segments. */
	private void stroke(GuiGraphics g, float x, float y, float dx, float dy, float length, int color) {
		int steps = Math.max(1, Math.round(length));
		for (int i = 0; i < steps; i++) {
			float px = x + dx * i;
			float py = y + dy * i;
			GlassRenderer.roundedRect(g, Math.min(px, px + dx), Math.min(py, py + dy), 1.2f, 1.2f, 0.4f, color);
		}
	}
}
