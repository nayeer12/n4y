package com.n4y.client.gui.screen.panel;

import java.util.ArrayList;
import java.util.List;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.gui.widget.UiContext;

import net.minecraft.network.chat.Component;

/** The theme picker: one card per theme, the selected one lit with its own accent. */
public final class AppearanceView implements PanelView {
	private static final Component HINT = Component.translatable("n4y.appearance.hint");
	private static final Component ACTIVE = Component.translatable("n4y.appearance.active");
	private static final Component SELECT = Component.translatable("n4y.appearance.select");
	private static final float OPTION_H = 56f;

	private final List<ThemeOption> options = new ArrayList<>();
	private final ScrollState scroll = new ScrollState();

	public AppearanceView() {
		for (Theme theme : Themes.ALL) {
			options.add(new ThemeOption(theme));
		}
	}

	/** The theme cards, in the order they are shown. Used by the interface tests. */
	public List<N4yWidget> cards() {
		return List.copyOf(options);
	}

	@Override
	public ScrollState scroll() {
		return scroll;
	}

	@Override
	public void layout(PanelGeometry geom, float scrollOffset) {
		// Every theme fits without scrolling, so there is no overflow to report.
		scroll.setExtent(0f);
		float optionW = (PanelGeometry.WIDTH - PanelGeometry.PAD * 2 - PanelGeometry.GAP) / 2f;
		for (int i = 0; i < options.size(); i++) {
			options.get(i).bounds(
					geom.listX() + (i % 2) * (optionW + PanelGeometry.GAP),
					geom.bodyY() + 6f + (i / 2) * (OPTION_H + PanelGeometry.GAP),
					optionW, OPTION_H);
		}
	}

	@Override
	public void render(UiContext ctx, PanelGeometry geom) {
		N4yText.draw(ctx.g(), N4yText.resolve(HINT), geom.listX(),
				geom.y() + PanelGeometry.HEIGHT - PanelGeometry.PAD - 10f, 0.78f,
				Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		for (int i = 0; i < options.size(); i++) {
			options.get(i).render(ctx);
		}
	}

	@Override
	public boolean click(float mx, float my, boolean right) {
		for (int i = 0; i < options.size(); i++) {
			ThemeOption option = options.get(i);
			if (option.contains(mx, my)) {
				N4yClient.get().themes().select(option.theme);
				return true;
			}
		}
		return false;
	}

	/** A selectable theme. Draws itself in its own accent, not the one in force. */
	private static final class ThemeOption extends N4yWidget {
		private final Theme theme;
		private final Animation selection = new Animation(0f, 220, Easing.OUT_CUBIC);

		ThemeOption(Theme theme) {
			this.theme = theme;
			this.selection.set(isSelected() ? 1f : 0f);
		}

		private boolean isSelected() {
			return N4yClient.get().themes().selected().id().equals(theme.id());
		}

		@Override
		protected void draw(UiContext ctx, float hovered) {
			Theme current = ctx.theme();
			float a = ctx.alpha();
			selection.animateTo(isSelected() ? 1f : 0f, ctx.now());
			float sel = selection.value(ctx.now());

			GlassRenderer.glassCard(ctx.g(), x, y, width, height, 11f, current, hovered, sel, a);

			float r = 9f;
			GlassRenderer.shadow(ctx.g(), x + 14, y + height / 2f - r, r * 2, r * 2, r, 7,
					Colors.fade(theme.accentGlow(), a * 0.8f));
			GlassRenderer.roundedRect(ctx.g(), x + 14, y + height / 2f - r, r * 2, r * 2, r,
					Colors.fade(Colors.shade(theme.accent(), 0.2f), a), Colors.fade(theme.accent(), a),
					GlassRenderer.CORNERS_ALL);

			N4yText.draw(ctx.g(), N4yText.resolve(theme.displayName()), x + 42, y + height / 2f - 9f, 1.05f,
					Colors.fade(current.textPrimary(), a), true);
			String state = N4yText.resolve(sel > 0.5f ? ACTIVE : SELECT);
			N4yText.draw(ctx.g(), state, x + 42, y + height / 2f + 2f, 0.76f,
					Colors.fade(sel > 0.5f ? theme.accent() : current.textMuted(), a), false);
		}
	}
}
