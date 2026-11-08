package com.n4y.client.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColorsTest {
	@Test
	void channelsRoundTrip() {
		int c = Colors.argb(0x12, 0x34, 0x56, 0x78);
		assertEquals(0x12345678, c);
		assertEquals(0x12, Colors.alpha(c));
		assertEquals(0x78, Colors.blue(c));
	}

	@Test
	void lerpEndsAndMiddle() {
		assertEquals(0xFF000000, Colors.lerp(0xFF000000, 0xFFFFFFFF, 0f));
		assertEquals(0xFFFFFFFF, Colors.lerp(0xFF000000, 0xFFFFFFFF, 1f));
		assertEquals(0xFF808080, Colors.lerp(0xFF000000, 0xFFFFFFFF, 0.5f));
	}

	@Test
	void alphaHelpers() {
		assertEquals(0x80FF0000, Colors.withAlpha(0xFFFF0000, 128 / 255f));
		assertEquals(0x40FF0000, Colors.fade(0x80FF0000, 0.5f));
	}
}
