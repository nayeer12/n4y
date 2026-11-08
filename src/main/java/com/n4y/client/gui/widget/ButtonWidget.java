package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.network.chat.Component;

/** Small glass button. A primary button is filled with the accent colour. */
public final class ButtonWidget extends N4yWidget {
	private final Component label;
	private final boolean primary;
	private final BooleanSupplier usable;

	public ButtonWidget(Component label, boolean primary, BooleanSupplier usable) {
		this.label = label;
		this.primary = primary;
		this.usable = usable;
	}

	public boolean isUsable() {
		return usable.getAsBoolean();
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha() * (isUsable() ? 1f : 0.45f);
		float h = isUsable() ? hovered : 0f;
		float r = height / 2f;

		if (primary && isUsable()) {
			GlassRenderer.shadow(ctx.g(), x, y, width, height, r, 7, Colors.fade(t.accentGlow(), (0.35f + 0.3f * h) * a));
			int fill = Colors.lerp(t.accent(), Colors.shade(t.accent(), 0.18f), h);
			GlassRenderer.roundedRect(ctx.g(), x, y, width, height, r,
					Colors.fade(Colors.shade(fill, 0.12f), a), Colors.fade(fill, a), GlassRenderer.CORNERS_ALL);
		} else {
			int fill = Colors.lerp(t.surface(), t.surfaceHover(), h);
			GlassRenderer.roundedRect(ctx.g(), x, y, width, height, r,
					Colors.fade(Colors.shade(fill, 0.05f), a), Colors.fade(fill, a), GlassRenderer.CORNERS_ALL);
		}
		GlassRenderer.border(ctx.g(), x, y, width, height, r, 0.5f,
				Colors.fade(t.borderTop(), a), Colors.fade(t.borderBottom(), a));

		int textColor = Colors.fade(primary && isUsable() ? 0xFFFFFFFF : t.textPrimary(), a);
		N4yText.drawCentered(ctx.g(), label.getString(), x + width / 2f, y + height / 2f, 0.88f, textColor, true);
	}
}
