package com.n4y.client.combat;

import java.util.concurrent.ThreadLocalRandom;

/** Percent chances. Every call is a fresh, independent roll. */
public final class Chance {
	private Chance() {
	}

	/** True with probability {@code percent}/100; 100 always passes and 0 never does. */
	public static boolean roll(int percent) {
		if (percent >= 100) {
			return true;
		}
		if (percent <= 0) {
			return false;
		}
		return ThreadLocalRandom.current().nextInt(100) < percent;
	}
}
