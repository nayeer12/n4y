package com.n4y.client.module.impl;

import com.n4y.client.N4yClient;
import com.n4y.client.core.Numbers;
import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.hud.PanelHudModule;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** How long this session has been running, the active profile and how much is turned on. */
public final class SessionStatsModule extends PanelHudModule {
	/** The clock only shows whole seconds, so there is nothing to gain from going faster. */
	private static final long REFRESH_MILLIS = 250L;

	public SessionStatsModule() {
		super("session_stats", ModuleCategory.MISC, Icons.CLOCK,
				HudPlacement.of(HudAnchor.BOTTOM_LEFT, 6f, -6f), 2, REFRESH_MILLIS);
	}

	@Override
	protected void refresh(Minecraft mc) {
		N4yClient client = N4yClient.get();
		if (client == null) {
			set(0, "");
			set(1, "");
			return;
		}
		StringBuilder clock = line();
		Numbers.appendClock(clock, client.sessionSeconds());
		set(0, Component.translatable("n4y.hud.session", clock.toString()).getString());
		// enabledCount() counts in place; enabled() would build a stream and a list to be sized.
		set(1, Component.translatable("n4y.hud.profile",
				client.config() == null ? "" : client.config().activeProfileName(),
				client.modules().enabledCount()).getString());
	}
}
