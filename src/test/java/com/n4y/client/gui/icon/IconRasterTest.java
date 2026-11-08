package com.n4y.client.gui.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IconRasterTest {
	@Test
	void segmentDistanceClampsToEnds() {
		assertEquals(0f, IconRaster.segmentDistance(0, 0, 0, 0, 10, 0), 1e-4);
		assertEquals(3f, IconRaster.segmentDistance(5, 3, 0, 0, 10, 0), 1e-4);
		assertEquals(5f, IconRaster.segmentDistance(-5, 0, 0, 0, 10, 0), 1e-4);
	}

	@Test
	void strokeIsCoveredAndBackgroundIsEmpty() {
		Icon cross = Icon.of("t").line(2, 12, 22, 12).build();
		int size = 24;
		assertTrue(IconRaster.coverage(cross, 12, 11, size) > 0.9f, "pixel on the stroke is nearly solid");
		assertEquals(0f, IconRaster.coverage(cross, 12, 2, size), 1e-3); // far above it
	}

	@Test
	void everyIconDrawsSomethingAtSmallSize() {
		Icon[] all = {Icons.GRID, Icons.SWORDS, Icons.RUN, Icons.EYE, Icons.PERSON, Icons.CUBE,
				Icons.DOTS, Icons.SEARCH, Icons.PALETTE, Icons.ZOOM, Icons.ARROW_UP, Icons.ARROW_DOWN,
				Icons.CAMERA, Icons.LAYOUT, Icons.WAVE};
		for (Icon icon : all) {
			int size = 14;
			float sum = 0f;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float c = IconRaster.coverage(icon, x, y, size);
					assertTrue(c >= 0f && c <= 1f, icon.key() + " coverage out of range");
					sum += c;
				}
			}
			assertTrue(sum > 8f, icon.key() + " is nearly empty at 14px (" + sum + ")");
		}
	}
}
