package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.icon.Icons;
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
	/** Width taken by the arrow that opens the settings panel. */
	private static final float CHEVRON_W = 11f;

	private final Module module;
	private final BooleanSupplier binding;
	private final ToggleSwitch toggle = new ToggleSwitch();
	private final Animation glow = new Animation(0f, 240, Easing.OUT_CUBIC);
	private boolean shownState;
	private String cachedName = "";
	private String cachedDesc = "";
	private String trimmedName = "";
	private String trimmedDesc = "";
	private float trimmedWidth = -1f;

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
		return module.hasKey() ? module.key().displayName().getString() : "+";
	}

	private float chipWidth() {
		return Math.min(CHIP_MAX_W, N4yText.width(chipLabel(), false, 0.72f) + 11f);
	}

	/** Space kept on the right edge for the toggle and, when there are settings, the arrow. */
	private float rightInset() {
		return 12f + (module.hasSettings() ? CHEVRON_W : 0f);
	}

	private float chipX() {
		return x + width - rightInset() - TOGGLE_W - 7f - chipWidth();
	}

	/** True when the cursor is over the arrow that opens this module's settings. */
	public boolean chevronContains(double mx, double my) {
		return module.hasSettings() && contains(mx, my) && mx >= x + width - CHEVRON_W - 6f;
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
		float textW = width - (textX - x) - rightInset() - TOGGLE_W - 7f - (showChip ? chipWidth() + 6f : 0f);

		String name = module.displayName().getString();
		String desc = isBinding
				? Component.translatable("n4y.keybind.press").getString()
				: module.description().getString();
		// Trimming measures the text glyph by glyph, so only redo it when something changed.
		if (textW != trimmedWidth || !name.equals(cachedName) || !desc.equals(cachedDesc)) {
			cachedName = name;
			cachedDesc = desc;
			trimmedWidth = textW;
			trimmedName = N4yText.ellipsize(name, true, 1f, textW);
			trimmedDesc = N4yText.ellipsize(desc, false, 0.78f, textW);
		}
		N4yText.draw(ctx.g(), trimmedName, textX, y + 11.5f, 1f, Colors.fade(t.textPrimary(), a), true);
		int descColor = Colors.fade(isBinding ? t.accent() : t.textSecondary(), a);
		N4yText.draw(ctx.g(), trimmedDesc, textX, y + 25f, 0.78f, descColor, false);

		if (showChip) {
			drawChip(ctx, isBinding, hovered, a, t);
		}
		toggle.draw(ctx, x + width - rightInset() - TOGGLE_W, y + (height - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H);

		if (module.hasSettings()) {
			boolean over = chevronContains(ctx.mouseX(), ctx.mouseY());
			IconTextures.draw(ctx.g(), Icons.CHEVRON_RIGHT, x + width - CHEVRON_W - 3f, y + (height - 10f) / 2f, 10f,
					Colors.fade(over ? t.textPrimary() : t.textMuted(), a));
		}
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
