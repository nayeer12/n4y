package com.n4y.client.gui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThemeEngineTest {
	@Test
	void defaultIsPurpleAndFourThemesExist() {
		assertEquals(Themes.PURPLE, new ThemeEngine().selected());
		assertEquals(4, Themes.ALL.size());
		assertTrue(Themes.byId("green").isPresent());
	}

	@Test
	void crossFadesThenSettles() {
		ThemeEngine engine = new ThemeEngine();
		int[] changes = {0};
		engine.onChange(() -> changes[0]++);
		engine.select(Themes.BLUE, 0L);
		Theme mid = engine.current(175_000_000L);
		assertNotEquals(Themes.PURPLE.accent(), mid.accent());
		assertNotEquals(Themes.BLUE.accent(), mid.accent());
		assertEquals(Themes.BLUE.accent(), engine.current(1_000_000_000L).accent());
		assertEquals(1, changes[0]);
	}
}
