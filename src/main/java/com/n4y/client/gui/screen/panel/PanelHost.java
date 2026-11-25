package com.n4y.client.gui.screen.panel;

import com.n4y.client.module.Module;
import com.n4y.client.gui.widget.ModuleCardWidget;

/**
 * What a {@link PanelView} asks of the screen that owns it.
 *
 * <p>Deliberately narrow. The cards are shared because the same module appears in more than one
 * section, and binding a key is screen-wide state because the next keystroke anywhere completes
 * it — everything else a section needs, it owns.
 */
public interface PanelHost {
	/** The card for {@code module}, or {@code null} when it has none. */
	ModuleCardWidget card(Module module);

	/** Opens {@code module}'s settings in place of the current section. */
	void openDetail(Module module);

	/** Starts waiting for a key or mouse button to bind to {@code module}. */
	void startBinding(Module module);

	/** True while {@code module} is the one waiting for a key, which its chip shows. */
	boolean isBinding(Module module);

	/** Leaves the settings panel and goes back to the section that was showing. */
	void closeDetail();

	/** Leaves the panel for the HUD Studio. */
	void openHudStudio();

	/** A different profile was loaded, so module states and cards have changed underneath. */
	void onProfileSwitched(String profileName);
}
