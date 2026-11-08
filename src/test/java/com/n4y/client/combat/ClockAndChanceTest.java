package com.n4y.client.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClockAndChanceTest {
	@Test
	void clockCountsDownAndNeverGetsStuck() {
		Clock clock = new Clock();
		assertTrue(clock.ready());
		clock.arm(2);
		assertFalse(clock.ready());
		clock.tick();
		assertFalse(clock.ready());
		clock.tick();
		assertTrue(clock.ready());
		clock.tick();
		assertTrue(clock.ready(), "an extra tick must not wrap around");
		clock.arm(-5);
		assertTrue(clock.ready(), "a negative delay is no delay");
	}

	@Test
	void chanceEndsAreExact() {
		for (int i = 0; i < 500; i++) {
			assertTrue(Chance.roll(100));
			assertFalse(Chance.roll(0));
		}
	}

	@Test
	void chanceRollsAreIndependentAndRoughlyFair() {
		int hits = 0;
		int rolls = 20_000;
		for (int i = 0; i < rolls; i++) {
			if (Chance.roll(30)) {
				hits++;
			}
		}
		double rate = hits / (double) rolls;
		assertEquals(0.30, rate, 0.03, "30% chance landed at " + rate);
	}
}
