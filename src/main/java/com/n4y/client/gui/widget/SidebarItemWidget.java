package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.ModuleCategory;

/** A category row in the sidebar; the selected one keeps a lit glass pill behind it. */
public final class SidebarItemWidget extends N4yWidget {
	private final ModuleCategory category;
	private final BooleanSupplier selected;
	private final Animation selection = new Animation(0f, 220, Easing.OUT_CUBIC);

	public SidebarItemWidget(ModuleCategory category, BooleanSupplier selected) {
		this.category = category;
		this.selected = selected;
		this.selection.set(selected.getAsBoolean() ? 1f : 0f);
	}

	public ModuleCategory category() {
		return category;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		selection.animateTo(selected.getAsBoolean() ? 1f : 0f, ctx.now());
		float sel = selection.value(ctx.now());

		if (sel > 0.01f || hovered > 0.01f) {
			int fill = Colors.lerp(Colors.fade(t.surfaceHover(), 0.55f * hovered), t.selection(), sel);
			GlassRenderer.roundedRect(ctx.g(), x, y, width, height, 7f, Colors.fade(Colors.shade(fill, 0.05f), a), Colors.fade(fill, a), GlassRenderer.CORNERS_ALL);
			if (sel > 0.01f) {
				GlassRenderer.border(ctx.g(), x, y, width, height, 7f, 0.5f, Colors.fade(t.borderTop(), sel * a), Colors.fade(t.borderBottom(), sel * a));
			}
		}

		int color = Colors.fade(Colors.lerp(t.textSecondary(), t.textPrimary(), Math.max(sel, hovered)), a);
		IconTextures.draw(ctx.g(), category.icon(), x + 9, y + (height - 13f) / 2f, 13f, color);
		N4yText.draw(ctx.g(), category.displayName().getString(), x + 28, y + (height - 7f) / 2f - 0.5f, 0.92f, color, sel > 0.5f);
	}
}
