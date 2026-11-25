package com.n4y.client.gui.hud;

import java.util.ArrayList;
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
 * Glass toasts in the top right corner: what was turned on, which profile was loaded. They slide
 * in, hold, then slide out, and never block anything the player is doing.
 *
 * <p>Drawing a toast used to resolve its components to strings and trim them to fit on every
 * frame, and copying the queue into a list to take the first few allocated two objects per frame
 * whether or not anything was on screen. Here the text is resolved once, on the first frame the
 * toast is drawn, and the queue is a list walked by index. Toasts also expire from the front:
 * they are added in order and all live equally long, so the ones that are too old are always at
 * the head, and the rest need not be looked at.
 */
public final class NotificationCenter {
	private static final int MAX_VISIBLE = 4;
	private static final float WIDTH = 150f;
	private static final float HEIGHT = 34f;
	private static final float MARGIN = 6f;
	private static final long SLIDE_MILLIS = 220L;
	private static final float TEXT_WIDTH = WIDTH - 38f;
	private static final float TITLE_SCALE = 0.9f;
	private static final float SUBTITLE_SCALE = 0.74f;

	/**
	 * A queued toast and its laid out text. The text is resolved on first use, not when the
	 * toast is pushed: pushing happens on the client thread and must not touch the font.
	 */
	private static final class Toast {
		private final Notification notification;
		private String title;
		private String subtitle;

		Toast(Notification notification) {
			this.notification = notification;
		}

		void layOut() {
			if (title != null) {
				return;
			}
			title = N4yText.ellipsize(notification.title().getString(), true, TITLE_SCALE, TEXT_WIDTH);
			subtitle = N4yText.ellipsize(notification.subtitle().getString(), false, SUBTITLE_SCALE, TEXT_WIDTH);
		}
	}

	private final List<Toast> queue = new ArrayList<>();

	public void push(Notification notification) {
		if (notification == null) {
			return;
		}
		queue.add(new Toast(notification));
		while (queue.size() > MAX_VISIBLE * 2) {
			queue.remove(0);
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

	/**
	 * Drops the toasts that have run out of time.
	 *
	 * <p>They are added in order and all live equally long, so a stale toast is always at the
	 * head: the moment one is still in time, so is everything behind it. Scanning the whole queue
	 * every frame, as a {@code removeIf} does, also allocated an iterator to find nothing.
	 */
	void dropExpired(long now) {
		while (!queue.isEmpty() && queue.get(0).notification.expired(now)) {
			queue.remove(0);
		}
	}

	/** Draws the toasts and drops the ones that have run out of time. */
	public void render(GuiGraphics g, Theme theme, float screenWidth, long now) {
		dropExpired(now);
		int shown = Math.min(MAX_VISIBLE, queue.size());
		float y = MARGIN;
		for (int i = 0; i < shown; i++) {
			Toast toast = queue.get(i);
			float age = toast.notification.age(now);
			float appear = Easing.OUT_CUBIC.apply(Math.min(1f, age / SLIDE_MILLIS));
			float leave = Math.min(1f, Math.max(0f, (Notification.LIFETIME - age) / (float) SLIDE_MILLIS));
			float progress = Math.min(appear, Easing.OUT_CUBIC.apply(leave));
			float x = screenWidth - MARGIN - WIDTH + (1f - progress) * (WIDTH + MARGIN);
			draw(g, theme, toast, x, y, progress);
			y += HEIGHT + 5f;
		}
	}

	private void draw(GuiGraphics g, Theme theme, Toast toast, float x, float y, float alpha) {
		Notification notification = toast.notification;
		GlassRenderer.shadow(g, x, y + 1f, WIDTH, HEIGHT, 9f, 9, Colors.fade(0x70000000, alpha));
		GlassRenderer.roundedRect(g, x, y, WIDTH, HEIGHT, 9f,
				Colors.fade(Colors.shade(theme.panelTop(), 0.05f), alpha), Colors.fade(theme.panelBottom(), alpha),
				GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(g, x, y, WIDTH, HEIGHT, 9f, 0.6f,
				Colors.fade(notification.accented() ? theme.accent() : theme.borderTop(), alpha * 0.8f),
				Colors.fade(theme.borderBottom(), alpha));

		int iconColor = Colors.fade(notification.accented() ? theme.accent() : theme.textSecondary(), alpha);
		IconTextures.draw(g, notification.icon(), x + 10, y + (HEIGHT - 15f) / 2f, 15f, iconColor);

		toast.layOut();
		if (toast.subtitle.isEmpty()) {
			N4yText.drawMiddle(g, toast.title, x + 32, y + HEIGHT / 2f, TITLE_SCALE,
					Colors.fade(theme.textPrimary(), alpha), true);
		} else {
			N4yText.draw(g, toast.title, x + 32, y + 8f, TITLE_SCALE, Colors.fade(theme.textPrimary(), alpha), true);
			N4yText.draw(g, toast.subtitle, x + 32, y + 19.5f, SUBTITLE_SCALE,
					Colors.fade(theme.textMuted(), alpha), false);
		}
	}
}
