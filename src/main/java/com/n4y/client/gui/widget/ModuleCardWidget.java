package com.n4y.client.gui.widget;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.Module;

/** One module in the grid: icon, name, description and a working switch. */
public final class ModuleCardWidget extends N4yWidget {
	private static final float ICON = 17f;

	private final Module module;
	private final ToggleSwitch toggle = new ToggleSwitch();
	private final Animation glow = new Animation(0f, 240, Easing.OUT_CUBIC);
	private boolean shownState;

	public ModuleCardWidget(Module module) {
		this.module = module;
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

	/** Re-syncs the visuals when the state changed elsewhere (keybind, config load). */
	public void syncTo(long now) {
		shownState = module.isEnabled();
		toggle.set(shownState, now);
		glow.animateTo(shownState ? 1f : 0f, now);
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		if (shownState != module.isEnabled()) {
			syncTo(ctx.now());
		}
		Theme t = ctx.theme();
		float a = ctx.alpha();
		float on = glow.value(ctx.now());

		GlassRenderer.glassCard(ctx.g(), x, y, width, height, 9f, t, hovered, on, a);

		int iconColor = Colors.fade(Colors.lerp(t.textSecondary(), t.textPrimary(), Math.max(on, hovered)), a);
		IconTextures.draw(ctx.g(), module.icon(), x + 11, y + (height - ICON) / 2f, ICON, iconColor);

		float textX = x + 11 + ICON + 9;
		float textW = width - (textX - x) - 38;
		String name = N4yText.ellipsize(module.displayName().getString(), true, 1f, textW);
		String desc = N4yText.ellipsize(module.description().getString(), false, 0.78f, textW);
		N4yText.draw(ctx.g(), name, textX, y + 11.5f, 1f, Colors.fade(t.textPrimary(), a), true);
		N4yText.draw(ctx.g(), desc, textX, y + 25f, 0.78f, Colors.fade(t.textSecondary(), a), false);

		float tw = 24f;
		float th = 13f;
		toggle.draw(ctx, x + width - 12 - tw, y + (height - th) / 2f, tw, th);
	}
}
