package com.n4y.client.gui.screen.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollStateTest {
	private static final long SECOND = 1_000_000_000L;

	/** Settles the animation so the visible offset equals the target. */
	private static float settled(ScrollState state) {
		state.advance(0L);
		return state.offset(SECOND);
	}

	@Test
	void aListThatFitsCannotScroll() {
		ScrollState state = new ScrollState();
		state.setExtent(0f);
		assertFalse(state.canScroll());
		assertFalse(state.hasOverflow());
		assertFalse(state.scrollBy(40f), "scrolling a list that fits must not consume the input");
		assertEquals(0f, settled(state), 1e-4);
	}

	@Test
	void scrollingStopsAtBothEnds() {
		ScrollState state = new ScrollState();
		state.setExtent(100f);
		assertTrue(state.scrollBy(40f));
		assertEquals(40f, settled(state), 1e-4);

		assertTrue(state.scrollBy(500f));
		assertEquals(100f, settled(state), 1e-4);

		assertTrue(state.scrollBy(-500f));
		assertEquals(0f, settled(state), 1e-4);
	}

	@Test
	void shrinkingTheContentPullsTheTargetBack() {
		ScrollState state = new ScrollState();
		state.setExtent(200f);
		state.scrollBy(180f);
		assertEquals(180f, settled(state), 1e-4);

		// The category filter narrowed and there is far less to scroll through now.
		state.setExtent(50f);
		assertEquals(50f, settled(state), 1e-4, "the list stayed scrolled past its end");
	}

	@Test
	void aNegativeExtentIsTreatedAsNoOverflow() {
		ScrollState state = new ScrollState();
		state.setExtent(-30f);
		assertEquals(0f, state.extent(), 1e-4);
		assertFalse(state.canScroll());
	}

	@Test
	void overflowNeedsMoreThanHalfAUnit() {
		ScrollState state = new ScrollState();
		state.setExtent(0.4f);
		assertTrue(state.canScroll());
		assertFalse(state.hasOverflow(), "a fraction of a pixel must not draw a scrollbar");
		state.setExtent(0.6f);
		assertTrue(state.hasOverflow());
	}

	@Test
	void resetJumpsToTheTopWithoutAnimating() {
		ScrollState state = new ScrollState();
		state.setExtent(100f);
		state.scrollBy(70f);
		settled(state);
		state.reset();
		assertEquals(0f, state.offset(0L), 1e-4);
	}
}
