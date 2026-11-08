package com.n4y.client.gui.render;

import com.n4y.client.gui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Drawing primitives of the Liquid Glass look: rounded rectangles, gradients, edge highlights,
 * soft shadows and glows. Callers pass GUI coordinates as floats; drawing happens in physical
 * pixels so edges stay crisp and animations can move by sub-GUI-pixel amounts.
 *
 * <p>Pure presentation: never reads or changes module state.
 */
public final class GlassRenderer {
	public static final int CORNERS_ALL = 0b11;
	public static final int CORNERS_TOP = 0b01;
	public static final int CORNERS_BOTTOM = 0b10;
	/** Bands used to shade a rounded corner that sits on a gradient. */
	private static final int MAX_GRADIENT_BANDS = 8;
	/** Channel difference below which a gradient is drawn as a flat colour. */
	private static final int FLAT_TOLERANCE = 6;

	private GlassRenderer() {
	}

	private static float scale() {
		return UiScale.physical();
	}

	private static void push(GuiGraphics g, float s) {
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
	}

	// ---------------------------------------------------------------- shapes

	public static void roundedRect(GuiGraphics g, float x, float y, float w, float h, float r, int color) {
		roundedRect(g, x, y, w, h, r, color, color, CORNERS_ALL);
	}

	/** Rounded rectangle filled with a vertical gradient. */
	public static void roundedRect(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom, int corners) {
		if (w <= 0 || h <= 0 || ((top | bottom) >>> 24) == 0) {
			return;
		}
		float s = scale();
		int x0 = Math.round(x * s), y0 = Math.round(y * s);
		int x1 = Math.round((x + w) * s), y1 = Math.round((y + h) * s);
		int pw = x1 - x0, ph = y1 - y0;
		int pr = Math.max(0, Math.min(Math.round(r * s), Math.min(pw, ph) / 2));
		int rt = (corners & CORNERS_TOP) != 0 ? pr : 0;
		int rb = (corners & CORNERS_BOTTOM) != 0 ? pr : 0;
		push(g, s);
		ShapeTextures.Mask mask = pr > 0 ? ShapeTextures.fill(pr) : null;
		if (rt > 0) {
			cornerGradient(g, mask, x0, y0, 0, 0, top, bottom, 0, ph);
			cornerGradient(g, mask, x1 - pr, y0, pr + 1, 0, top, bottom, 0, ph);
			g.fillGradient(x0 + pr, y0, x1 - pr, y0 + rt, top, at(top, bottom, rt, ph));
		}
		g.fillGradient(x0, y0 + rt, x1, y1 - rb, at(top, bottom, rt, ph), at(top, bottom, ph - rb, ph));
		if (rb > 0) {
			cornerGradient(g, mask, x0, y1 - pr, 0, pr + 1, top, bottom, ph - pr, ph);
			cornerGradient(g, mask, x1 - pr, y1 - pr, pr + 1, pr + 1, top, bottom, ph - pr, ph);
			g.fillGradient(x0 + pr, y1 - rb, x1 - pr, y1, at(top, bottom, ph - rb, ph), bottom);
		}
		g.pose().popMatrix();
	}

	/** Rounded outline; {@code thickness} in GUI units (at least one physical pixel). */
	public static void border(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, int top, int bottom) {
		if (w <= 0 || h <= 0 || ((top | bottom) >>> 24) == 0) {
			return;
		}
		float s = scale();
		int x0 = Math.round(x * s), y0 = Math.round(y * s);
		int x1 = Math.round((x + w) * s), y1 = Math.round((y + h) * s);
		int pw = x1 - x0, ph = y1 - y0;
		int t = Math.max(1, Math.round(thickness * s));
		int pr = Math.max(t, Math.min(Math.round(r * s), Math.min(pw, ph) / 2));
		push(g, s);
		ShapeTextures.Mask mask = ShapeTextures.ring(pr, t);
		cornerGradient(g, mask, x0, y0, 0, 0, top, bottom, 0, ph);
		cornerGradient(g, mask, x1 - pr, y0, pr + 1, 0, top, bottom, 0, ph);
		cornerGradient(g, mask, x0, y1 - pr, 0, pr + 1, top, bottom, ph - pr, ph);
		cornerGradient(g, mask, x1 - pr, y1 - pr, pr + 1, pr + 1, top, bottom, ph - pr, ph);
		g.fill(x0 + pr, y0, x1 - pr, y0 + t, top);
		g.fill(x0 + pr, y1 - t, x1 - pr, y1, bottom);
		int sideTop = at(top, bottom, pr, ph), sideBottom = at(top, bottom, ph - pr, ph);
		g.fillGradient(x0, y0 + pr, x0 + t, y1 - pr, sideTop, sideBottom);
		g.fillGradient(x1 - t, y0 + pr, x1, y1 - pr, sideTop, sideBottom);
		g.pose().popMatrix();
	}

	/** Soft drop shadow or glow around a rounded rect; {@code blur} in GUI units. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float r, float blur, int color) {
		if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
			return;
		}
		float s = scale();
		int x0 = Math.round(x * s), y0 = Math.round(y * s);
		int x1 = Math.round((x + w) * s), y1 = Math.round((y + h) * s);
		int pw = x1 - x0, ph = y1 - y0;
		int pr = Math.max(0, Math.min(Math.round(r * s), Math.min(pw, ph) / 2));
		int pb = Math.max(1, Math.round(blur * s));
		ShapeTextures.Mask m = ShapeTextures.shadow(pr, pb);
		int k = m.corner();
		int dx0 = x0 - pb, dy0 = y0 - pb, dx1 = x1 + pb, dy1 = y1 + pb;
		push(g, s);
		corner(g, m, dx0, dy0, 0, 0, color);
		corner(g, m, dx1 - k, dy0, k + 1, 0, color);
		corner(g, m, dx0, dy1 - k, 0, k + 1, color);
		corner(g, m, dx1 - k, dy1 - k, k + 1, k + 1, color);
		int midW = dx1 - dx0 - 2 * k, midH = dy1 - dy0 - 2 * k;
		if (midW > 0) {
			strip(g, m, dx0 + k, dy0, midW, k, k, 0, 1, k, color);
			strip(g, m, dx0 + k, dy1 - k, midW, k, k, k + 1, 1, k, color);
		}
		if (midH > 0) {
			strip(g, m, dx0, dy0 + k, k, midH, 0, k, k, 1, color);
			strip(g, m, dx1 - k, dy0 + k, k, midH, k + 1, k, k, 1, color);
		}
		if (midW > 0 && midH > 0) {
			g.fill(dx0 + k, dy0 + k, dx1 - k, dy1 - k, color);
		}
		g.pose().popMatrix();
	}

	// ---------------------------------------------------------------- composites

	/** The main Liquid Glass window: depth shadow, tinted fill, top sheen and lit edge. */
	public static void glassPanel(GuiGraphics g, float x, float y, float w, float h, float r, Theme t, float alpha) {
		shadow(g, x, y + 4, w, h, r, 22, Colors.fade(0x70000000, alpha));
		roundedRect(g, x, y, w, h, r, Colors.fade(t.panelTop(), alpha), Colors.fade(t.panelBottom(), alpha), CORNERS_ALL);
		roundedRect(g, x, y, w, Math.min(h, 46), r, Colors.fade(0x1CFFFFFF, alpha), 0x00FFFFFF, CORNERS_TOP);
		border(g, x, y, w, h, r, 0.75f, Colors.fade(t.borderTop(), alpha), Colors.fade(t.borderBottom(), alpha));
	}

	/**
	 * Inner glass surface (cards, sidebar). {@code hover} and {@code glow} are 0..1 animation
	 * values supplied by the widget.
	 */
	public static void glassCard(GuiGraphics g, float x, float y, float w, float h, float r, Theme t, float hover, float glow, float alpha) {
		if (glow > 0.01f) {
			shadow(g, x, y, w, h, r, 10, Colors.fade(t.accentGlow(), glow * alpha * 0.55f));
		}
		int fill = Colors.fade(Colors.lerp(t.surface(), t.surfaceHover(), hover), alpha);
		roundedRect(g, x, y, w, h, r, Colors.shade(fill, 0.04f), fill, CORNERS_ALL);
		int edge = Colors.fade(t.borderTop(), (0.45f + 0.35f * hover) * alpha);
		border(g, x, y, w, h, r, 0.5f, edge, Colors.fade(t.borderBottom(), alpha));
	}

	/** Full-screen tint drawn over the (blurred) world behind the client. */
	public static void backdrop(GuiGraphics g, int width, int height, Theme t, float alpha) {
		g.fillGradient(0, 0, width, height, Colors.fade(t.backdropTop(), alpha), Colors.fade(t.backdropBottom(), alpha));
	}

	// ---------------------------------------------------------------- internals

	private static int at(int top, int bottom, float py, int height) {
		return height <= 0 ? top : Colors.lerp(top, bottom, py / height);
	}

	private static void corner(GuiGraphics g, ShapeTextures.Mask m, int x, int y, int u, int v, int color) {
		int k = m.corner();
		if (k <= 0) {
			return;
		}
		g.blit(RenderPipelines.GUI_TEXTURED, m.id(), x, y, u, v, k, k, k, k, m.size(), m.size(), color);
	}

	/**
	 * Corner tinted with the gradient. A flat colour, or one the eye cannot tell from flat, is
	 * a single blit; a real gradient is drawn in a few horizontal bands instead of one blit per
	 * pixel row, which keeps a window full of cards at a handful of draw calls.
	 */
	private static void cornerGradient(GuiGraphics g, ShapeTextures.Mask m, int x, int y, int u, int v,
			int top, int bottom, int localY, int height) {
		int k = m.corner();
		if (k <= 0) {
			return;
		}
		if (isFlat(top, bottom)) {
			corner(g, m, x, y, u, v, Colors.lerp(top, bottom, 0.5f));
			return;
		}
		int bands = Math.min(k, MAX_GRADIENT_BANDS);
		for (int band = 0; band < bands; band++) {
			int from = band * k / bands;
			int to = (band + 1) * k / bands;
			if (to <= from) {
				continue;
			}
			int c = at(top, bottom, localY + (from + to) / 2f, height);
			g.blit(RenderPipelines.GUI_TEXTURED, m.id(), x, y + from, u, v + from, k, to - from, k, to - from, m.size(), m.size(), c);
		}
	}

	/** True when two colours are close enough that a gradient between them is invisible. */
	private static boolean isFlat(int a, int b) {
		return Math.abs(Colors.alpha(a) - Colors.alpha(b)) <= FLAT_TOLERANCE
				&& Math.abs(Colors.red(a) - Colors.red(b)) <= FLAT_TOLERANCE
				&& Math.abs(Colors.green(a) - Colors.green(b)) <= FLAT_TOLERANCE
				&& Math.abs(Colors.blue(a) - Colors.blue(b)) <= FLAT_TOLERANCE;
	}

	private static void strip(GuiGraphics g, ShapeTextures.Mask m, int x, int y, int w, int h, int u, int v, int rw, int rh, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, m.id(), x, y, u, v, w, h, rw, rh, m.size(), m.size(), color);
	}
}
