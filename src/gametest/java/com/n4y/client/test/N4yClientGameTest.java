package com.n4y.client.test;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.screen.GlassPreviewScreen;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * In-game smoke test: creates a singleplayer world, opens the n4y interface and captures a
 * screenshot per theme into run/screenshots for visual review.
 */
public final class N4yClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			context.setScreen(GlassPreviewScreen::new);
			context.waitTicks(20);
			for (Theme theme : Themes.ALL) {
				context.runOnClient(mc -> N4yClient.get().themes().selectInstant(theme));
				context.waitTicks(2);
				context.takeScreenshot("n4y-glass-" + theme.id());
			}
			context.runOnClient(mc -> N4yClient.get().themes().selectInstant(Themes.PURPLE));
			context.setScreen(() -> null);
		}
	}
}
