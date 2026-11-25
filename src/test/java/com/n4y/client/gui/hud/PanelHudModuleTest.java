package com.n4y.client.gui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;

class PanelHudModuleTest {
	/** Test double: counts refreshes and writes a line we control. */
	private static final class Panel extends PanelHudModule {
		int refreshes;
		String value = "alpha";
		RuntimeException failWith;

		Panel(long refreshMillis) {
			super("probe", ModuleCategory.MISC, Icons.DOTS,
					HudPlacement.of(HudAnchor.TOP_LEFT, 0f, 0f), 2, refreshMillis);
		}

		@Override
		protected void refresh(Minecraft mc) {
			refreshes++;
			if (failWith != null) {
				throw failWith;
			}
			StringBuilder out = line();
			out.append(value);
			set(0, out);
			set(1, "fixed");
		}
	}

	@Test
	void contentIsBuiltOnceWhileItIsStillFresh() {
		Panel panel = new Panel(60_000L);
		for (int i = 0; i < 5; i++) {
			panel.refreshIfStale(null);
		}
		assertEquals(1, panel.refreshes, "the panel rebuilt its text while it was still current");
		assertEquals("alpha", panel.line(0));
		assertEquals("fixed", panel.line(1));
	}

	@Test
	void aZeroIntervalAlwaysRebuilds() {
		Panel panel = new Panel(0L);
		panel.refreshIfStale(null);
		panel.refreshIfStale(null);
		panel.refreshIfStale(null);
		assertEquals(3, panel.refreshes);
	}

	@Test
	void unchangedTextIsNotRebuiltIntoANewString() {
		Panel panel = new Panel(0L);
		panel.refreshIfStale(null);
		String first = panel.line(0);
		panel.refreshIfStale(null);
		assertSame(first, panel.line(0), "the same value produced a second identical string");
	}

	@Test
	void changedTextIsPickedUp() {
		Panel panel = new Panel(0L);
		panel.refreshIfStale(null);
		panel.value = "beta";
		panel.refreshIfStale(null);
		assertEquals("beta", panel.line(0));
	}

	@Test
	void aFailingRefreshIsContainedAndBlanksTheLines() {
		Panel panel = new Panel(0L);
		panel.refreshIfStale(null);
		assertEquals("alpha", panel.line(0));

		panel.failWith = new IllegalStateException("boom");
		panel.refreshIfStale(null);

		assertEquals("", panel.line(0), "a broken element must not keep showing stale text");
		assertEquals("", panel.line(1));
	}

	@Test
	void heightFollowsTheLineCount() {
		assertEquals(HudPanel.height(2), new Panel(0L).height(null), 1e-4);
	}
}
