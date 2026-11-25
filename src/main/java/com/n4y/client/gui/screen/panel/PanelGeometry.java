package com.n4y.client.gui.screen.panel;

/**
 * Where the panel sits in the window and the rectangles inside it.
 *
 * <p>The panel is a fixed design size that is scaled down to fit small windows, so every
 * coordinate the interface works in is derived from two numbers: the scale and the panel's
 * corner. Those derivations — where the body starts, how wide a list is once the scrollbar has
 * its gutter — were written inline wherever they were needed, which is how the same expression
 * came to appear in both the layout pass and the render pass of four different tabs.
 *
 * <p>One instance is kept and moved each frame rather than rebuilt, so this costs no allocation.
 */
public final class PanelGeometry {
	public static final float WIDTH = 480f;
	public static final float HEIGHT = 268f;
	public static final float RADIUS = 16f;
	/** Kept between the panel and the window edge, in GUI units. */
	public static final float MARGIN = 14f;
	public static final float HEADER_H = 52f;
	public static final float PAD = 14f;
	public static final float SIDEBAR_W = 100f;
	public static final float CARD_H = 46f;
	public static final float ROW_H = 34f;
	public static final float GAP = 8f;
	public static final float SCROLLBAR_W = 7f;
	public static final float FIELD_H = 26f;
	public static final float DETAIL_HEADER_H = 40f;

	private float x;
	private float y;
	private float scale = 1f;

	/**
	 * Centres the panel in a window of {@code windowW} × {@code windowH} GUI units, shrinking it
	 * uniformly if it would not otherwise fit with its margin. The panel is never enlarged.
	 */
	public void centreIn(int windowW, int windowH) {
		scale = Math.min(1f, Math.min((windowW - MARGIN * 2f) / WIDTH, (windowH - MARGIN * 2f) / HEIGHT));
		if (!(scale > 0f)) {
			// A window too small to show anything must still give finite coordinates.
			scale = 1f;
		}
		x = (windowW / scale - WIDTH) / 2f;
		y = (windowH / scale - HEIGHT) / 2f;
	}

	/** Uniform zoom applied to the whole panel so it fits the window. */
	public float scale() {
		return scale;
	}

	public float x() {
		return x;
	}

	public float y() {
		return y;
	}

	/** Top of the area below the header, where the tabs draw their content. */
	public float bodyY() {
		return y + HEADER_H;
	}

	public float bodyH() {
		return HEIGHT - HEADER_H - PAD;
	}

	/** Left edge of the module grid, which sits to the right of the category sidebar. */
	public float contentX() {
		return x + PAD + SIDEBAR_W + GAP;
	}

	public float contentW() {
		return WIDTH - (contentX() - x) - PAD - SCROLLBAR_W;
	}

	/** Left edge of a full-width list: every tab except the module grid. */
	public float listX() {
		return x + PAD;
	}

	/** Width of a full-width list, leaving the scrollbar its gutter. */
	public float listW() {
		return WIDTH - PAD * 2 - SCROLLBAR_W;
	}

	/** Height of the HUD list, leaving room for the hint and the studio button below it. */
	public float hudListH() {
		return bodyH() - FIELD_H - 10f;
	}

	/** Height of the profile list, leaving room for the creation bar below it. */
	public float profileListH() {
		return bodyH() - FIELD_H - 22f;
	}

	/** Height of the settings list, between the module header and the reset button. */
	public float detailListH() {
		return bodyH() - DETAIL_HEADER_H - FIELD_H - 6f;
	}

	public float detailListTop() {
		return bodyY() + DETAIL_HEADER_H;
	}
}
