package com.n4y.client.gui.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimationTest {
	private static final long MS = 1_000_000L;

	@Test
	void linearProgress() {
		Animation a = new Animation(0f, 100, Easing.LINEAR);
		a.animateTo(10f, 0L);
		assertEquals(0f, a.value(0L), 1e-4);
		assertEquals(5f, a.value(50 * MS), 1e-4);
		assertEquals(10f, a.value(500 * MS), 1e-4);
		assertTrue(a.isRunning(50 * MS));
		assertFalse(a.isRunning(100 * MS));
	}

	@Test
	void retargetStartsFromVisualValue() {
		Animation a = new Animation(0f, 100, Easing.LINEAR);
		a.animateTo(10f, 0L);
		a.animateTo(0f, 50 * MS);
		assertEquals(5f, a.value(50 * MS), 1e-4);
		assertEquals(3.75f, a.value(75 * MS), 1e-4); // full duration after retarget
	}

	@Test
	void easingsHitEndpoints() {
		for (Easing e : new Easing[] {Easing.LINEAR, Easing.OUT_CUBIC, Easing.IN_OUT_CUBIC, Easing.OUT_QUINT, Easing.OUT_BACK}) {
			assertEquals(0f, e.apply(0f), 1e-4);
			assertEquals(1f, e.apply(1f), 1e-4);
		}
	}
}
