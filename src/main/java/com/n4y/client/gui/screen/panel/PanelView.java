package com.n4y.client.gui.screen.panel;

import java.util.Optional;

import com.n4y.client.gui.widget.TextFieldWidget;
import com.n4y.client.gui.widget.UiContext;

/**
 * One section of the main panel: the module grid, the HUD list, the profiles, the appearance
 * picker or a module's settings.
 *
 * <p>Each section used to have its layout, its drawing and its click handling spread across three
 * {@code switch} statements in the screen, so adding or changing one meant finding its three
 * pieces among all the others. A section now holds its own widgets, its own scroll position and
 * those three methods together, and the screen only decides which one is showing.
 */
public interface PanelView {
	/**
	 * Places this section's widgets.
	 *
	 * @param scrollOffset how far the list is scrolled, already eased, in GUI units
	 */
	void layout(PanelGeometry geom, float scrollOffset);

	/** Draws this section. The context carries the cursor in panel space, as clicks do. */
	void render(UiContext ctx, PanelGeometry geom);

	/**
	 * Handles a click in panel coordinates.
	 *
	 * @param right true for the secondary button, which starts a key binding
	 * @return true when the click was consumed
	 */
	boolean click(float mx, float my, boolean right);

	/** This section's own scroll position, so switching tabs does not disturb the others. */
	ScrollState scroll();

	/** The text field taking keystrokes, if this section has a focused one. */
	default Optional<TextFieldWidget> focusedField() {
		return Optional.empty();
	}

	/** Called when this section becomes the visible one. */
	default void onShown() {
	}
}
