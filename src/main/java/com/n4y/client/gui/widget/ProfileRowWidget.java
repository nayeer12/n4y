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

import net.minecraft.network.chat.Component;

/** One saved profile: click the row to load it, click the cross to delete it. */
public final class ProfileRowWidget extends N4yWidget {
	private static final float DELETE_SIZE = 22f;

	private final String name;
	private final BooleanSupplier active;
	private final Animation selection = new Animation(0f, 220, Easing.OUT_CUBIC);

	public ProfileRowWidget(String name, BooleanSupplier active) {
		this.name = name;
		this.active = active;
		this.selection.set(active.getAsBoolean() ? 1f : 0f);
	}

	public String profileName() {
		return name;
	}

	public boolean isActive() {
		return active.getAsBoolean();
	}

	/** True when the cursor is over the delete cross (only shown for inactive profiles). */
	public boolean deleteContains(double mx, double my) {
		if (isActive()) {
			return false;
		}
		float dx = x + width - DELETE_SIZE - 6f;
		float dy = y + (height - DELETE_SIZE) / 2f;
		return mx >= dx && my >= dy && mx < dx + DELETE_SIZE && my < dy + DELETE_SIZE;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		selection.animateTo(isActive() ? 1f : 0f, ctx.now());
		float sel = selection.value(ctx.now());

		GlassRenderer.glassCard(ctx.g(), x, y, width, height, 9f, t, hovered, sel, a);

		IconTextures.draw(ctx.g(), Icons.FOLDER, x + 12, y + (height - 15f) / 2f, 15f,
				Colors.fade(Colors.lerp(t.textSecondary(), t.accent(), sel), a));
		N4yText.draw(ctx.g(), N4yText.ellipsize(name, true, 1f, width - 100f), x + 34, y + height / 2f - 9f, 1f,
				Colors.fade(t.textPrimary(), a), true);
		String state = Component.translatable(sel > 0.5f ? "n4y.profiles.active" : "n4y.profiles.use").getString();
		N4yText.draw(ctx.g(), state, x + 34, y + height / 2f + 2f, 0.76f,
				Colors.fade(sel > 0.5f ? t.accent() : t.textMuted(), a), false);

		if (!isActive()) {
			float dx = x + width - DELETE_SIZE - 6f;
			float dy = y + (height - DELETE_SIZE) / 2f;
			boolean over = deleteContains(ctx.mouseX(), ctx.mouseY());
			if (over) {
				GlassRenderer.roundedRect(ctx.g(), dx, dy, DELETE_SIZE, DELETE_SIZE, 7f,
						Colors.fade(0x33FF5A6E, a), Colors.fade(0x22FF5A6E, a), GlassRenderer.CORNERS_ALL);
			}
			IconTextures.draw(ctx.g(), Icons.CLOSE, dx + 5f, dy + 5f, 12f,
					Colors.fade(over ? 0xFFFF8A96 : t.textMuted(), a));
		}
	}
}
