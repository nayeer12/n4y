package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.core.keybind.KeybindManager;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.Module;

import net.minecraft.network.chat.Component;

/**
 * One module in the grid: icon, name, description, its key bind and a working switch.
 * The key chip appears when a key is bound or while the cursor is on the card.
 */
public final class ModuleCardWidget extends N4yWidget {
	private static final float ICON = 17f;
	private static final float TOGGLE_W = 24f;
	private static final float TOGGLE_H = 13f;
	private static final float CHIP_H = 12f;
	private static final float CHIP_MAX_W = 46f;

	private final Module module;
	private final BooleanSupplier binding;
	private final ToggleSwitch toggle = new ToggleSwitch();
	private final Animation glow = new Animation(0f, 240, Easing.OUT_CUBIC);
	private boolean shownState;

	public ModuleCardWidget(Module module, BooleanSupplier binding) {
		this.module = module;
		this.binding = binding;
		shownState = module.isEnabled();
		toggle.setInstant(shownState);
		glow.set(shownState ? 1f : 0f);
	}

	public Module module() {
		return module;
	}

	/** Flips the module and starts the switch animation. */
	public void toggle(long now) {
		module.toggle();
		syncTo(now);
	}

	/** Re-syncs the visuals when the state changed elsewhere (key bind, profile load). */
	public void syncTo(long now) {
		shownState = module.isEnabled();
		toggle.set(shownState, now);
		glow.animateTo(shownState ? 1f : 0f, now);
	}

	private String chipLabel() {
		if (binding.getAsBoolean()) {
			return "…";
		}
		return module.hasKey() ? KeybindManager.displayName(module.key()).getString() : "+";
	}

	private float chipWidth() {
		return Math.min(CHIP_MAX_W, N4yText.width(chipLabel(), false, 0.72f) + 11f);
	}

	private float chipX() {
		return x + width - 12f - TOGGLE_W - 7f - chipWidth();
	}

	/** True when the cursor is over the key chip, which opens the key binding. */
	public boolean chipContains(double mx, double my) {
		if (!contains(mx, my)) {
			return false;
		}
		float cx = chipX();
		float cy = y + (height - CHIP_H) / 2f;
		return mx >= cx - 2 && my >= cy - 3 && mx < cx + chipWidth() + 2 && my < cy + CHIP_H + 3;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		if (shownState != module.isEnabled()) {
			syncTo(ctx.now());
		}
		Theme t = ctx.theme();
		float a = ctx.alpha();
		float on = glow.value(ctx.now());
		boolean isBinding = binding.getAsBoolean();

		GlassRenderer.glassCard(ctx.g(), x, y, width, height, 9f, t, hovered, on, a);

		int iconColor = Colors.fade(Colors.lerp(t.textSecondary(), t.textPrimary(), Math.max(on, hovered)), a);
		IconTextures.draw(ctx.g(), module.icon(), x + 11, y + (height - ICON) / 2f, ICON, iconColor);

		boolean showChip = module.hasKey() || isBinding || hovered > 0.35f;
		float textX = x + 11 + ICON + 9;
		float textW = width - (textX - x) - 12f - TOGGLE_W - 7f - (showChip ? chipWidth() + 6f : 0f);

		String name = N4yText.ellipsize(module.displayName().getString(), true, 1f, textW);
		N4yText.draw(ctx.g(), name, textX, y + 11.5f, 1f, Colors.fade(t.textPrimary(), a), true);

		String desc = isBinding
				? Component.translatable("n4y.keybind.press").getString()
				: module.description().getString();
		int descColor = Colors.fade(isBinding ? t.accent() : t.textSecondary(), a);
		N4yText.draw(ctx.g(), N4yText.ellipsize(desc, false, 0.78f, textW), textX, y + 25f, 0.78f, descColor, false);

		if (showChip) {
			drawChip(ctx, isBinding, hovered, a, t);
		}
		toggle.draw(ctx, x + width - 12 - TOGGLE_W, y + (height - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H);
	}

	private void drawChip(UiContext ctx, boolean isBinding, float hovered, float a, Theme t) {
		float cw = chipWidth();
		float cx = chipX();
		float cy = y + (height - CHIP_H) / 2f;
		boolean over = chipContains(ctx.mouseX(), ctx.mouseY());
		float strength = isBinding ? 1f : (over ? 0.7f : 0.3f);

		GlassRenderer.roundedRect(ctx.g(), cx, cy, cw, CHIP_H, 4f,
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), isBinding ? 0.8f : 0f), a * (0.35f + 0.4f * strength)),
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), isBinding ? 0.6f : 0f), a * (0.25f + 0.35f * strength)),
				GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), cx, cy, cw, CHIP_H, 4f, 0.5f,
				Colors.fade(t.borderTop(), a * strength), Colors.fade(t.borderBottom(), a * strength));
		int color = Colors.fade(module.hasKey() || isBinding ? t.textPrimary() : t.textMuted(), a * (0.6f + 0.4f * strength));
		N4yText.drawCentered(ctx.g(), N4yText.ellipsize(chipLabel(), false, 0.72f, cw - 6f), cx + cw / 2f, cy + CHIP_H / 2f, 0.72f, color, false);
	}
}
