package com.n4y.client.gui.widget;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.network.chat.Component;

/** Glass search field with icon, placeholder and a blinking caret while focused. */
public final class SearchBoxWidget extends N4yWidget {
	private static final int MAX_LENGTH = 48;

	private final Component placeholder;
	private final Animation focusAnim = new Animation(0f, 200, Easing.OUT_CUBIC);
	private final StringBuilder text = new StringBuilder();
	private boolean focused;

	public SearchBoxWidget(Component placeholder) {
		this.placeholder = placeholder;
	}

	public String text() {
		return text.toString();
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean value) {
		focused = value;
	}

	public boolean isEmpty() {
		return text.isEmpty();
	}

	public void clear() {
		text.setLength(0);
	}

	/** Appends a typed character; returns true when the query changed. */
	public boolean append(int codepoint) {
		if (!focused || text.length() >= MAX_LENGTH || Character.isISOControl(codepoint)) {
			return false;
		}
		text.appendCodePoint(codepoint);
		return true;
	}

	/** Removes the last character (or the whole word with {@code wholeWord}). */
	public boolean backspace(boolean wholeWord) {
		if (!focused || text.isEmpty()) {
			return false;
		}
		if (wholeWord) {
			int end = text.length();
			while (end > 0 && text.charAt(end - 1) == ' ') {
				end--;
			}
			while (end > 0 && text.charAt(end - 1) != ' ') {
				end--;
			}
			text.setLength(end);
		} else {
			text.deleteCharAt(text.length() - 1);
		}
		return true;
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		focusAnim.animateTo(focused ? 1f : 0f, ctx.now());
		float f = focusAnim.value(ctx.now());
		float r = height / 2f;

		if (f > 0.01f) {
			GlassRenderer.shadow(ctx.g(), x, y, width, height, r, 7, Colors.fade(t.accentGlow(), 0.4f * f * a));
		}
		int fill = Colors.lerp(t.surface(), t.surfaceHover(), Math.max(hovered * 0.6f, f));
		GlassRenderer.roundedRect(ctx.g(), x, y, width, height, r, Colors.fade(Colors.shade(fill, 0.05f), a), Colors.fade(fill, a), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), x, y, width, height, r, 0.5f,
				Colors.fade(Colors.lerp(t.borderTop(), t.accent(), f), a), Colors.fade(t.borderBottom(), a));

		IconTextures.draw(ctx.g(), Icons.SEARCH, x + 8, y + (height - 12f) / 2f, 12f,
				Colors.fade(Colors.lerp(t.textMuted(), t.accent(), f), a));

		float textX = x + 24;
		float textY = y + (height - 7f) / 2f - 0.5f;
		float maxW = width - 24 - 10;
		if (text.isEmpty()) {
			N4yText.draw(ctx.g(), placeholder.getString(), textX, textY, 0.9f, Colors.fade(t.textMuted(), a), false);
		} else {
			String shown = N4yText.ellipsizeStart(text.toString(), false, 0.9f, maxW);
			N4yText.draw(ctx.g(), shown, textX, textY, 0.9f, Colors.fade(t.textPrimary(), a), false);
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				float caretX = textX + Math.min(N4yText.width(shown, false, 0.9f), maxW) + 1f;
				ctx.g().fill(Math.round(caretX), Math.round(y + 6), Math.round(caretX) + 1, Math.round(y + height - 6), Colors.fade(t.accent(), a));
			}
		}
	}
}
