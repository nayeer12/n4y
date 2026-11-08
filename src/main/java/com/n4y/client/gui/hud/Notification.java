package com.n4y.client.gui.hud;

import com.n4y.client.gui.icon.Icon;

import net.minecraft.network.chat.Component;

/**
 * A message shown briefly in the corner.
 *
 * @param icon     small line icon on the left
 * @param title    main line
 * @param subtitle second line, may be empty
 * @param accented true to tint it with the theme accent (something was turned on)
 * @param bornAt   nanosecond timestamp used for the slide and fade
 */
public record Notification(Icon icon, Component title, Component subtitle, boolean accented, long bornAt) {
	/** How long it stays on screen, in milliseconds. */
	public static final long LIFETIME = 2600L;

	public static Notification of(Icon icon, Component title, Component subtitle, boolean accented) {
		return new Notification(icon, title, subtitle, accented, System.nanoTime());
	}

	public float age(long now) {
		return (now - bornAt) / 1_000_000f;
	}

	public boolean expired(long now) {
		return age(now) > LIFETIME;
	}
}
