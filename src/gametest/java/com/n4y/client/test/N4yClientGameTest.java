package com.n4y.client.test;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.screen.N4yMainScreen;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * End-to-end test of the client inside a real singleplayer world. Everything is driven through
 * the actual cursor and keyboard, so hit testing, the interface zoom, search, scrolling and the
 * modules' effect on the game are all exercised the way a player would.
 */
public final class N4yClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();

			context.getInput().resizeWindow(1280, 720);
			context.runOnClient(mc -> mc.options.guiScale().set(2));
			context.waitTicks(3);

			context.setScreen(N4yMainScreen::new);
			context.waitTicks(20);
			context.takeScreenshot("n4y-01-modules");

			testCategories(context);
			testSearch(context);
			testToggleAndScroll(context);
			testAppearance(context);
			testOptionsRestored(context);

			context.setScreen(() -> null);
			context.waitTicks(5);
		}
	}

	// ---------------------------------------------------------------- steps

	private void testCategories(ClientGameTestContext context) {
		click(context, screen(context).categoryItems().stream()
				.filter(i -> i.category() == ModuleCategory.VISUAL).findFirst().orElseThrow());
		context.waitTicks(6);
		check(context, s -> s.category() == ModuleCategory.VISUAL, "clicking Visual did not select that category");
		check(context, s -> s.listedModules().stream().allMatch(m -> m.category() == ModuleCategory.VISUAL),
				"the Visual category listed modules from other categories");
		context.takeScreenshot("n4y-02-category-visual");

		click(context, screen(context).categoryItems().get(0));
		context.waitTicks(4);
		check(context, s -> s.category() == ModuleCategory.ALL, "could not go back to every category");
	}

	private void testSearch(ClientGameTestContext context) {
		click(context, screen(context).searchBox());
		context.waitTicks(2);
		context.getInput().typeChars("zoom");
		context.waitTicks(6);
		check(context, s -> s.query().equals("zoom"), "the search box did not receive the typed text");
		check(context, s -> s.listedModules().size() == 1 && s.listedModules().get(0).id().equals("zoom"),
				"searching for \"zoom\" did not filter the list down to the Zoom module");
		context.takeScreenshot("n4y-03-search");

		for (int i = 0; i < 4; i++) {
			context.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
		}
		context.waitTicks(4);
		check(context, s -> s.query().isEmpty(), "backspace did not clear the query");
		check(context, s -> s.listedModules().size() == N4yClient.get().modules().all().size(),
				"clearing the query did not bring every module back");
	}

	private void testToggleAndScroll(ClientGameTestContext context) {
		Module first = context.computeOnClient(mc -> screen(mc).listedModules().get(0));
		click(context, context.computeOnClient(mc -> screen(mc).cardOf(first).orElseThrow()));
		context.waitTicks(8);
		if (!context.computeOnClient(mc -> first.isEnabled())) {
			throw new AssertionError("Clicking the card did not enable " + first.id());
		}

		context.runOnClient(mc -> {
			N4yClient.get().modules().byId("zoom").orElseThrow().setEnabled(true);
			N4yClient.get().modules().byId("hide_hud").orElseThrow().setEnabled(true);
		});
		context.waitTicks(8);
		context.takeScreenshot("n4y-04-modules-active");

		context.runOnClient(mc -> {
			if (mc.options.fov().get() != 30) {
				throw new AssertionError("Zoom did not change the FOV, got " + mc.options.fov().get());
			}
			if (!mc.options.hideGui) {
				throw new AssertionError("Hide HUD did not hide the vanilla interface");
			}
		});

		context.getInput().scroll(0, -3);
		context.waitTicks(6);
		context.takeScreenshot("n4y-05-scrolled");
	}

	private void testAppearance(ClientGameTestContext context) {
		click(context, screen(context).tabButtons().get(1));
		context.waitTicks(8);
		check(context, s -> s.tab() == N4yMainScreen.Tab.APPEARANCE, "clicking the second tab did not open Appearance");
		context.takeScreenshot("n4y-06-appearance");

		List<Theme> themes = Themes.ALL;
		for (int i = 0; i < themes.size(); i++) {
			N4yWidget card = context.computeOnClient(mc -> screen(mc).themeCards().get(themes.indexOf(themes.get(0))));
			final int index = i;
			click(context, context.computeOnClient(mc -> screen(mc).themeCards().get(index)));
			context.waitTicks(10);
			Theme expected = themes.get(i);
			context.runOnClient(mc -> {
				String selected = N4yClient.get().themes().selected().id();
				if (!selected.equals(expected.id())) {
					throw new AssertionError("Clicking the " + expected.id() + " card selected " + selected);
				}
			});
			context.takeScreenshot("n4y-07-theme-" + expected.id());
			if (card == null) {
				throw new AssertionError("missing theme card");
			}
		}

		context.runOnClient(mc -> N4yClient.get().themes().select(Themes.PURPLE));
		click(context, screen(context).tabButtons().get(0));
		context.waitTicks(6);
		check(context, s -> s.tab() == N4yMainScreen.Tab.MODULES, "could not go back to the Modules tab");
	}

	private void testOptionsRestored(ClientGameTestContext context) {
		context.runOnClient(mc -> N4yClient.get().modules().disableAll(mc));
		context.waitTicks(6);
		context.takeScreenshot("n4y-08-modules-off");
		context.runOnClient(mc -> {
			if (mc.options.fov().get() == 30) {
				throw new AssertionError("FOV was not restored after disabling Zoom");
			}
			if (mc.options.hideGui) {
				throw new AssertionError("The vanilla HUD was not restored after disabling Hide HUD");
			}
			for (Module module : N4yClient.get().modules().all()) {
				if (module.isEnabled()) {
					throw new AssertionError("Module still enabled after disableAll: " + module.id());
				}
			}
		});
	}

	// ---------------------------------------------------------------- helpers

	private static N4yMainScreen screen(net.minecraft.client.Minecraft mc) {
		if (mc.screen instanceof N4yMainScreen main) {
			return main;
		}
		throw new AssertionError("The n4y screen is not open, found " + mc.screen);
	}

	private N4yMainScreen screen(ClientGameTestContext context) {
		return context.computeOnClient(N4yClientGameTest::screen);
	}

	/** Moves the real cursor onto {@code widget} and clicks it. */
	private void click(ClientGameTestContext context, N4yWidget widget) {
		double[] pixels = context.computeOnClient(mc -> screen(mc).centreOf(widget));
		TestInput input = context.getInput();
		input.setCursorPos(pixels[0], pixels[1]);
		context.waitTick();
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private void check(ClientGameTestContext context, java.util.function.Predicate<N4yMainScreen> condition, String message) {
		context.runOnClient(mc -> {
			if (!condition.test(screen(mc))) {
				throw new AssertionError(message);
			}
		});
	}
}
