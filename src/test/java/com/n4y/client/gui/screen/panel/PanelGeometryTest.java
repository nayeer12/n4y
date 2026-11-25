package com.n4y.client.gui.screen.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PanelGeometryTest {
	@Test
	void aRoomyWindowShowsThePanelAtItsDesignSizeAndCentred() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(1920, 1080);
		assertEquals(1f, geom.scale(), 1e-4, "a large window must not enlarge the panel");
		assertEquals((1920 - PanelGeometry.WIDTH) / 2f, geom.x(), 1e-3);
		assertEquals((1080 - PanelGeometry.HEIGHT) / 2f, geom.y(), 1e-3);
	}

	@Test
	void aNarrowWindowShrinksThePanelToFitWithItsMargin() {
		PanelGeometry geom = new PanelGeometry();
		int windowW = 320;
		geom.centreIn(windowW, 1080);
		assertTrue(geom.scale() < 1f, "a narrow window must shrink the panel");
		// The margins are window units, not scaled ones: the shrunk panel plus both margins
		// fills the window exactly.
		assertEquals(windowW, PanelGeometry.WIDTH * geom.scale() + PanelGeometry.MARGIN * 2f, 1e-3);
	}

	@Test
	void theShorterSideDecidesTheScale() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(1920, 200);
		assertEquals((200 - PanelGeometry.MARGIN * 2f) / PanelGeometry.HEIGHT, geom.scale(), 1e-4);
	}

	@Test
	void aWindowTooSmallToFitTheMarginsStillGivesFiniteCoordinates() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(10, 10);
		assertTrue(Float.isFinite(geom.scale()) && geom.scale() > 0f);
		assertTrue(Float.isFinite(geom.x()) && Float.isFinite(geom.y()));
	}

	@Test
	void listsLeaveTheScrollbarItsGutterAndStayInsideThePanel() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(1920, 1080);
		assertEquals(geom.x() + PanelGeometry.PAD, geom.listX(), 1e-4);
		assertEquals(PanelGeometry.WIDTH - PanelGeometry.PAD * 2 - PanelGeometry.SCROLLBAR_W, geom.listW(), 1e-4);
		assertTrue(geom.listX() + geom.listW() + PanelGeometry.SCROLLBAR_W <= geom.x() + PanelGeometry.WIDTH + 1e-3,
				"the list and its scrollbar must fit inside the panel");
	}

	@Test
	void theModuleGridStartsAfterTheSidebar() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(1920, 1080);
		assertEquals(geom.x() + PanelGeometry.PAD + PanelGeometry.SIDEBAR_W + PanelGeometry.GAP, geom.contentX(), 1e-4);
		assertTrue(geom.contentX() + geom.contentW() + PanelGeometry.SCROLLBAR_W <= geom.x() + PanelGeometry.WIDTH + 1e-3);
	}

	@Test
	void everyListHeightFitsInsideTheBody() {
		PanelGeometry geom = new PanelGeometry();
		geom.centreIn(1920, 1080);
		for (float h : new float[] {geom.hudListH(), geom.profileListH(), geom.detailListH()}) {
			assertTrue(h > 0f && h <= geom.bodyH(), "list height " + h + " does not fit the body " + geom.bodyH());
		}
	}
}
