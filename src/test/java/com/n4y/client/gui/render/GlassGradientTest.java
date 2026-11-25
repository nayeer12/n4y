package com.n4y.client.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;

/**
 * How many blits a rounded corner costs. The answer used to depend on the gradient across the
 * whole shape, which for a tall panel is many times the gradient across one of its corners.
 */
class GlassGradientTest {
	private static final Theme THEME = Themes.PURPLE;
	/** Physical pixels per GUI unit at the common GUI scale of 2. */
	private static final int SCALE = 2;

	/**
	 * Blits for the four corners of a shape, the way {@code roundedRect} and {@code border}
	 * place them: the top pair at the start of the gradient, the bottom pair at its end.
	 */
	private static int cornerBlits(int top, int bottom, int cornerPx, int heightPx) {
		return 2 * GlassRenderer.gradientBands(top, bottom, 0, cornerPx, heightPx)
				+ 2 * GlassRenderer.gradientBands(top, bottom, heightPx - cornerPx, cornerPx, heightPx);
	}

	@Test
	void aFlatColourIsOneBand() {
		int colour = 0xFF203040;
		assertEquals(1, GlassRenderer.gradientBands(colour, colour, 0, 32, 536));
	}

	@Test
	void theMainPanelFillNeedsOneBandPerCorner() {
		// The real window: 480x268 GUI units, 16 unit corners, panelTop to panelBottom.
		int cornerPx = 16 * SCALE;
		int heightPx = 268 * SCALE;
		int delta = Colors.maxChannelDelta(THEME.panelTop(), THEME.panelBottom());
		assertTrue(delta > GlassRenderer.FLAT_TOLERANCE,
				"this test is pointless unless the panel really has a visible gradient, got " + delta);

		assertEquals(4, cornerBlits(THEME.panelTop(), THEME.panelBottom(), cornerPx, heightPx),
				"a corner covering a sixteenth of the panel does not need a gradient of its own");
	}

	@Test
	void aModuleCardFillNeedsOneBandPerCorner() {
		// glassCard fills with shade(fill, 0.04f) to fill, over a 46 unit card with 9 unit corners.
		int fill = Colors.fade(THEME.surface(), 1f);
		int top = Colors.shade(fill, 0.04f);
		assertEquals(4, cornerBlits(top, fill, 9 * SCALE, 46 * SCALE));
	}

	@Test
	void aCardBorderKeepsItsGradientButNeedsFewerBands() {
		// The edge highlight runs from a bright top to a nearly clear bottom: a real gradient.
		int top = THEME.borderTop();
		int bottom = THEME.borderBottom();
		int cornerPx = 9 * SCALE;
		int heightPx = 46 * SCALE;
		int bands = GlassRenderer.gradientBands(top, bottom, 0, cornerPx, heightPx);
		assertTrue(bands > 1, "a strong edge gradient must still be drawn as a gradient");
		assertTrue(bands < GlassRenderer.MAX_GRADIENT_BANDS,
				"it should need fewer than the old fixed " + GlassRenderer.MAX_GRADIENT_BANDS + ", got " + bands);
	}

	@Test
	void aCornerSpanningTheWholeShapeStillGetsTheFullGradient() {
		// A pill: the corner is half the height, so the whole gradient passes through it.
		int bands = GlassRenderer.gradientBands(0xFF000000, 0xFFFFFFFF, 0, 20, 40);
		assertEquals(GlassRenderer.MAX_GRADIENT_BANDS, bands);
	}

	@Test
	void aCornerOnePixelTallIsNeverBanded() {
		assertEquals(1, GlassRenderer.gradientBands(0xFF000000, 0xFFFFFFFF, 0, 1, 40));
		assertEquals(1, GlassRenderer.gradientBands(0xFF000000, 0xFFFFFFFF, 0, 0, 40));
	}

	@Test
	void theStepBetweenBandsStaysInvisible() {
		// Across a sweep of shapes and gradients, either the band count is capped or each step
		// is within the tolerance the renderer already treats as flat.
		for (int heightPx = 8; heightPx <= 600; heightPx += 37) {
			for (int cornerPx = 1; cornerPx <= heightPx / 2; cornerPx += 5) {
				for (int alpha = 0; alpha <= 255; alpha += 51) {
					int top = Colors.argb(alpha, 20, 40, 60);
					int bottom = Colors.argb(255, 200, 180, 160);
					int bands = GlassRenderer.gradientBands(top, bottom, 0, cornerPx, heightPx);
					assertTrue(bands >= 1 && bands <= Math.min(cornerPx, GlassRenderer.MAX_GRADIENT_BANDS),
							"band count " + bands + " out of range for corner " + cornerPx);

					int delta = Colors.maxChannelDelta(top, bottom) * cornerPx / heightPx;
					if (bands < Math.min(cornerPx, GlassRenderer.MAX_GRADIENT_BANDS)) {
						assertTrue(delta / bands <= GlassRenderer.FLAT_TOLERANCE,
								"uncapped bands must keep each step invisible: delta " + delta + " over " + bands);
					}
				}
			}
		}
	}

	@Test
	void everyThemeBenefitsOnTheMainPanel() {
		for (Theme theme : Themes.ALL) {
			assertEquals(4, cornerBlits(theme.panelTop(), theme.panelBottom(), 16 * SCALE, 268 * SCALE),
					"theme " + theme.id() + " still bands its panel corners");
		}
	}
}
