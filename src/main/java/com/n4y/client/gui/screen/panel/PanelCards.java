package com.n4y.client.gui.screen.panel;

import java.util.List;

import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.module.Module;

/**
 * What clicking a module card does, shared by the sections that show cards.
 *
 * <p>The module grid and the HUD list had the same three-way decision written out separately:
 * the chevron opens the settings, the key chip or the secondary button starts a binding, and
 * anywhere else switches the module.
 */
final class PanelCards {
	private PanelCards() {
	}

	/**
	 * @param top    first visible row of the list; a click above it is outside
	 * @param bottom last visible row, so a card scrolled out of sight cannot be hit
	 * @return true when a card took the click
	 */
	static boolean click(List<? extends Module> modules, PanelHost host,
			float mx, float my, boolean right, float top, float bottom) {
		if (my < top || my > bottom) {
			return false;
		}
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			ModuleCardWidget card = host.card(module);
			if (card == null || !card.contains(mx, my)) {
				continue;
			}
			if (card.chevronContains(mx, my)) {
				host.openDetail(module);
			} else if (right || card.chipContains(mx, my)) {
				host.startBinding(module);
			} else {
				card.toggle(System.nanoTime());
			}
			return true;
		}
		return false;
	}
}
