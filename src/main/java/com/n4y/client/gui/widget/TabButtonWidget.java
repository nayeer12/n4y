package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.network.chat.Component;

/** Icon tab in the header bar. Shows its name as a tooltip. */
public final class TabButtonWidget extends N4yWidget {
	private final Icon icon;
	private final Component tooltip;
	private final BooleanSupplier selected;
	private final Animation selection = new Animation(0f, 220, Easing.OUT_CUBIC);

	public TabButtonWidget(Icon icon, Component tooltip, BooleanSupplier selected) {
		this.icon = icon;
		this.tooltip = tooltip;
		this.selected = selected;
		this.selection.set(selected.getAsBoolean() ? 1f : 0f);
	}

	public Component tooltip() {
		return tooltip;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		selection.animateTo(selected.getAsBoolean() ? 1f : 0f, ctx.now());
		float sel = selection.value(ctx.now());

		if (sel > 0.01f) {
			GlassRenderer.shadow(ctx.g(), x, y, width, height, 7f, 7, Colors.fade(t.accentGlow(), 0.45f * sel * a));
		}
		if (sel > 0.01f || hovered > 0.01f) {
			int fill = Colors.lerp(Colors.fade(t.surfaceHover(), 0.5f * hovered), t.selection(), sel);
			GlassRenderer.roundedRect(ctx.g(), x, y, width, height, 7f, Colors.fade(Colors.shade(fill, 0.06f), a), Colors.fade(fill, a), GlassRenderer.CORNERS_ALL);
			GlassRenderer.border(ctx.g(), x, y, width, height, 7f, 0.5f,
					Colors.fade(t.borderTop(), Math.max(sel, hovered * 0.6f) * a), Colors.fade(t.borderBottom(), sel * a));
		}
		int color = Colors.fade(Colors.lerp(t.textSecondary(), t.textPrimary(), Math.max(sel, hovered)), a);
		IconTextures.draw(ctx.g(), icon, x + (width - 15f) / 2f, y + (height - 15f) / 2f, 15f, color);
	}
}
