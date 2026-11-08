package com.n4y.client.combat;

/**
 * A countdown in client ticks. Modules keep one per phase instead of sharing a single counter,
 * so a wait in one branch cannot leak into another, and changing a delay while the clock is
 * running can never leave it stuck.
 */
public final class Clock {
	private int remaining;

	/** Counts down one tick; call once per tick. */
	public void tick() {
		if (remaining > 0) {
			remaining--;
		}
	}

	public boolean ready() {
		return remaining <= 0;
	}

	public void arm(int ticks) {
		remaining = Math.max(0, ticks);
	}

	public void clear() {
		remaining = 0;
	}
}
