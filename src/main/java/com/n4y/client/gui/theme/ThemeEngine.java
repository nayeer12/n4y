package com.n4y.client.gui.theme;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;

/**
 * Holds the active theme and cross-fades between themes when it changes.
 * Renderers ask {@link #current()} every frame; the returned palette is already interpolated.
 */
public final class ThemeEngine {
	private static final long TRANSITION_MS = 350L;

	private Theme previous = Themes.PURPLE;
	private Theme selected = Themes.PURPLE;
	private final Animation transition = new Animation(1f, TRANSITION_MS, Easing.IN_OUT_CUBIC);
	private Runnable changeListener = () -> { };

	public Theme selected() {
		return selected;
	}

	public Theme current() {
		return current(System.nanoTime());
	}

	public Theme current(long nowNanos) {
		return Theme.lerp(previous, selected, transition.value(nowNanos));
	}

	/** Switches theme with a cross-fade from whatever is on screen right now. */
	public void select(Theme theme) {
		select(theme, System.nanoTime());
	}

	public void select(Theme theme, long nowNanos) {
		if (theme.equals(selected)) {
			return;
		}
		previous = current(nowNanos);
		selected = theme;
		transition.set(0f);
		transition.animateTo(1f, nowNanos);
		changeListener.run();
	}

	/** Switches instantly (used when loading saved preferences). */
	public void selectInstant(Theme theme) {
		previous = theme;
		selected = theme;
		transition.set(1f);
	}

	/** Called after a user-driven change; the config layer hooks in here to persist it. */
	public void onChange(Runnable listener) {
		this.changeListener = listener;
	}
}
