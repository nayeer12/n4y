package com.n4y.client.gui.theme;

/**
 * Full color palette of one Glass theme. All values are ARGB.
 *
 * @param accent         main accent (active toggles, selection, logo)
 * @param accentGlow     translucent accent used for glows behind active elements
 * @param backdropTop    tint over the blurred world, top of the screen
 * @param backdropBottom tint over the blurred world, bottom of the screen
 * @param panelTop       main glass window fill, top
 * @param panelBottom    main glass window fill, bottom
 * @param surface        cards and inner surfaces
 * @param surfaceHover   cards while hovered
 * @param selection      selected sidebar entry / tab
 * @param borderTop      glass edge highlight at the top (light comes from above)
 * @param borderBottom   glass edge at the bottom
 * @param textPrimary    titles
 * @param textSecondary  descriptions
 * @param textMuted      placeholders, versions
 * @param toggleOff      track of a disabled toggle
 */
public record Theme(
		String id,
		String displayName,
		int accent,
		int accentGlow,
		int backdropTop,
		int backdropBottom,
		int panelTop,
		int panelBottom,
		int surface,
		int surfaceHover,
		int selection,
		int borderTop,
		int borderBottom,
		int textPrimary,
		int textSecondary,
		int textMuted,
		int toggleOff) {

	/** Interpolates every color, used for animated theme switches. */
	public static Theme lerp(Theme a, Theme b, float t) {
		if (t <= 0f) {
			return a;
		}
		if (t >= 1f) {
			return b;
		}
		return new Theme(b.id, b.displayName,
				c(a.accent, b.accent, t), c(a.accentGlow, b.accentGlow, t),
				c(a.backdropTop, b.backdropTop, t), c(a.backdropBottom, b.backdropBottom, t),
				c(a.panelTop, b.panelTop, t), c(a.panelBottom, b.panelBottom, t),
				c(a.surface, b.surface, t), c(a.surfaceHover, b.surfaceHover, t),
				c(a.selection, b.selection, t),
				c(a.borderTop, b.borderTop, t), c(a.borderBottom, b.borderBottom, t),
				c(a.textPrimary, b.textPrimary, t), c(a.textSecondary, b.textSecondary, t),
				c(a.textMuted, b.textMuted, t), c(a.toggleOff, b.toggleOff, t));
	}

	private static int c(int x, int y, float t) {
		return com.n4y.client.gui.render.Colors.lerp(x, y, t);
	}
}
