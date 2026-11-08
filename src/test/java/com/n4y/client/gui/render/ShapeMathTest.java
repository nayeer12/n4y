package com.n4y.client.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShapeMathTest {
	@Test
	void fillIsSolidInsideAndEmptyInCorner() {
		int r = 8;
		assertEquals(1f, ShapeMath.fill(r, r, r), 1e-4);      // centre
		assertEquals(0f, ShapeMath.fill(0, 0, r), 1e-4);      // outer corner
		assertEquals(1f, ShapeMath.fill(r, 0, r), 1e-4);      // straight top edge is fully covered
		float edge = ShapeMath.fill(2, 2, r);                  // on the arc: partial coverage
		assertTrue(edge > 0f && edge < 1f, "anti-aliased arc, got " + edge);
	}

	@Test
	void ringIsHollow() {
		int r = 8;
		assertEquals(0f, ShapeMath.ring(r, r, r, 1), 1e-4);
		assertEquals(1f, ShapeMath.ring(r, 0, r, 1), 1e-4);
	}

	@Test
	void shadowFallsOffMonotonically() {
		int r = 4, blur = 6;
		float prev = 2f;
		int mid = r + blur;
		for (int y = mid; y >= 0; y--) {
			float v = ShapeMath.shadow(mid, y, r, blur);
			assertTrue(v <= prev + 1e-6, "monotonic");
			prev = v;
		}
		assertEquals(1f, ShapeMath.shadow(mid, mid, r, blur), 1e-4);
		assertTrue(ShapeMath.shadow(mid, 0, r, blur) < 0.05f);
	}
}
