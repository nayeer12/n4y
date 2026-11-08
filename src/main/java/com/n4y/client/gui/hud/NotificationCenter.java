package com.n4y.client.gui.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Glass toasts in the top right corner: what was turned on, which profile was loaded.
 * They slide in, hold, then slide out, and never block anything the player is doing.
 */
public final class NotificationCenter {
	private static final int MAX_VISIBLE = 4;
	private static final float WIDTH = 150f;
	private static final float HEIGHT = 34f;
	private static final float MARGIN = 6f;
	private static final long SLIDE_MILLIS = 220L;

	private final Deque<Notification> queue = new ArrayDeque<>();

	public void push(Notification notification) {
		queue.addLast(notification);
		while (queue.size() > MAX_VISIBLE * 2) {
			queue.removeFirst();
		}
	}

	public void push(Icon icon, Component title, Component subtitle, boolean accented) {
		push(Notification.of(icon, title, subtitle, accented));
	}

	public void clear() {
		queue.clear();
	}

	public int size() {
		return queue.size();
	}

	/** Draws the toasts and drops the ones that have run out of time. */
	public void render(GuiGraphics g, Theme theme, float screenWidth, long now) {
		queue.removeIf(notification -> notification.expired(now));
		if (queue.isEmpty()) {
			return;
		}
		List<Notification> visible = new ArrayList<>(queue).subList(0, Math.min(MAX_VISIBLE, queue.size()));
		float y = MARGIN;
		for (Notification notification : visible) {
			float age = notification.age(now);
			float appear = Easing.OUT_CUBIC.apply(Math.min(1f, age / SLIDE_MILLIS));
			float leave = Math.min(1f, Math.max(0f, (Notification.LIFETIME - age) / (float) SLIDE_MILLIS));
			float progress = Math.min(appear, Easing.OUT_CUBIC.apply(leave));
			float x = screenWidth - MARGIN - WIDTH + (1f - progress) * (WIDTH + MARGIN);
			draw(g, theme, notification, x, y, progress);
			y += HEIGHT + 5f;
		}
	}

	private void draw(GuiGraphics g, Theme theme, Notification notification, float x, float y, float alpha) {
		GlassRenderer.shadow(g, x, y + 1f, WIDTH, HEIGHT, 9f, 9, Colors.fade(0x70000000, alpha));
		GlassRenderer.roundedRect(g, x, y, WIDTH, HEIGHT, 9f,
				Colors.fade(Colors.shade(theme.panelTop(), 0.05f), alpha), Colors.fade(theme.panelBottom(), alpha),
				GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(g, x, y, WIDTH, HEIGHT, 9f, 0.6f,
				Colors.fade(notification.accented() ? theme.accent() : theme.borderTop(), alpha * 0.8f),
				Colors.fade(theme.borderBottom(), alpha));

		int iconColor = Colors.fade(notification.accented() ? theme.accent() : theme.textSecondary(), alpha);
		IconTextures.draw(g, notification.icon(), x + 10, y + (HEIGHT - 15f) / 2f, 15f, iconColor);

		String title = N4yText.ellipsize(notification.title().getString(), true, 0.9f, WIDTH - 38f);
		String subtitle = N4yText.ellipsize(notification.subtitle().getString(), false, 0.74f, WIDTH - 38f);
		if (subtitle.isEmpty()) {
			N4yText.drawMiddle(g, title, x + 32, y + HEIGHT / 2f, 0.9f, Colors.fade(theme.textPrimary(), alpha), true);
		} else {
			N4yText.draw(g, title, x + 32, y + 8f, 0.9f, Colors.fade(theme.textPrimary(), alpha), true);
			N4yText.draw(g, subtitle, x + 32, y + 19.5f, 0.74f, Colors.fade(theme.textMuted(), alpha), false);
		}
	}
}
