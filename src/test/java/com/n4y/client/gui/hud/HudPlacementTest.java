package com.n4y.client.gui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudPlacementTest {
	private static final float SCREEN_W = 854f;
	private static final float SCREEN_H = 480f;
	private static final float W = 100f;
	private static final float H = 30f;

	@Test
	void anchorsResolveToTheRightCorner() {
		assertEquals(0f, HudAnchor.TOP_LEFT.originX(SCREEN_W, W), 1e-4);
		assertEquals(0f, HudAnchor.TOP_LEFT.originY(SCREEN_H, H), 1e-4);
		assertEquals(SCREEN_W - W, HudAnchor.BOTTOM_RIGHT.originX(SCREEN_W, W), 1e-4);
		assertEquals(SCREEN_H - H, HudAnchor.BOTTOM_RIGHT.originY(SCREEN_H, H), 1e-4);
		assertEquals((SCREEN_W - W) / 2f, HudAnchor.CENTER.originX(SCREEN_W, W), 1e-4);
	}

	@Test
	void droppingNearACornerAnchorsToIt() {
		HudPlacement placement = HudPlacement.fromScreen(SCREEN_W - W - 6f, SCREEN_H - H - 6f, W, H, SCREEN_W, SCREEN_H);
		assertEquals(HudAnchor.BOTTOM_RIGHT, placement.anchor());
		assertEquals(-6f, placement.x(), 1e-4);
		assertEquals(-6f, placement.y(), 1e-4);
	}

	@Test
	void aPlacementKeepsItsDistanceToTheEdgeOnAnotherResolution() {
		HudPlacement placement = HudPlacement.fromScreen(SCREEN_W - W - 10f, 10f, W, H, SCREEN_W, SCREEN_H);
		assertEquals(HudAnchor.TOP_RIGHT, placement.anchor());
		assertEquals(1920f - W - 10f, placement.screenX(1920f, W), 1e-4);
		assertEquals(10f, placement.screenY(1080f, H), 1e-4);
	}
}
