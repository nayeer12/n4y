package com.n4y.client.test;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.N4yClient;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.screen.N4yMainScreen;
import com.n4y.client.module.Module;
import com.n4y.client.module.impl.combat.AnchorMacroModule;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.KeySetting;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The combat modules in a real singleplayer world. The player stands still and looks at a
 * block two ahead; every module may only act on what that crosshair rests on.
 *
 * <p>Fabric's test input reaches the game through its key callbacks, while the combat modules
 * read the physical key state, so held keys cannot be simulated here. The tests therefore use
 * AutoCrystal with its activate key unbound (which means "always active", as in the original)
 * and trigger AnchorMacro's Place sequence through the same method its key calls.
 */
public final class N4yCombatGameTest implements FabricClientGameTest {
	private int px;
	private int py;
	private int pz;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			context.getInput().resizeWindow(1280, 720);
			context.runOnClient(mc -> mc.options.guiScale().set(2));

			BlockPos feet = context.computeOnClient(mc -> mc.player.blockPosition());
			px = feet.getX();
			py = feet.getY();
			pz = feet.getZ();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamemode creative @a");
			aim(context, world);

			testSmartAutoTotem(context, world);
			testAnchorPlace(context, world);
			testTriggerBot(context, world);
			testAutoCrystal(context, world);
			showCombatInterface(context);

			context.runOnClient(mc -> N4yClient.get().modules().disableAll(mc));
			context.setScreen(() -> null);
			context.waitTicks(5);
		}
	}

	/** Stands the player on the block centre, facing south, looking down at the block two ahead. */
	private void aim(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("tp @a " + (px + 0.5) + " " + py + " " + (pz + 0.5) + " 0 35");
		context.waitTicks(10);
	}

	private BlockPos ground() {
		return new BlockPos(px, py - 1, pz + 2);
	}

	private BlockPos above() {
		return new BlockPos(px, py, pz + 2);
	}

	private static Module module(String id) {
		return N4yClient.get().modules().byId(id).orElseThrow(() -> new AssertionError("Missing module " + id));
	}

	private void resetArena(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("clear @a");
		world.getServer().runCommand("kill @e[type=!player]");
		// A killed mob drops its loot only when its death animation ends, right on the arena;
		// clear again once that has happened, or the drops would block the next placement.
		context.waitTicks(25);
		world.getServer().runCommand("kill @e[type=!player]");
		world.getServer().runCommand("setblock " + px + " " + (py - 1) + " " + (pz + 2) + " stone");
		world.getServer().runCommand("setblock " + px + " " + py + " " + (pz + 2) + " air");
		aim(context, world);
	}

	// ---------------------------------------------------------------- Smart AutoTotem

	private void testSmartAutoTotem(ClientGameTestContext context, TestSingleplayerContext world) {
		resetArena(context, world);
		// Survival: in creative, vanilla swaps the inventory for the creative menu, which is not
		// the inventory a Crystal PvP player opens and which Smart AutoTotem leaves alone.
		world.getServer().runCommand("gamemode survival @a");
		world.getServer().runCommand("item replace entity @a inventory.5 with totem_of_undying");
		context.runOnClient(mc -> module("auto_totem").setEnabled(true));
		context.waitTicks(20);
		// The rule that matters most: with the inventory closed, nothing moves.
		context.runOnClient(mc -> {
			if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				throw new AssertionError("Smart AutoTotem moved a totem while the inventory was closed");
			}
		});

		context.runOnClient(mc -> mc.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(20);
		context.takeScreenshot("n4y-combat-01-autototem");
		context.runOnClient(mc -> {
			if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				throw new AssertionError("Smart AutoTotem did not refill the off hand with the inventory open");
			}
			module("auto_totem").setEnabled(false);
			mc.setScreen(null);
		});
		world.getServer().runCommand("gamemode creative @a");
		context.waitTicks(5);
	}

	// ---------------------------------------------------------------- AnchorMacro Place

	private void testAnchorPlace(ClientGameTestContext context, TestSingleplayerContext world) {
		resetArena(context, world);
		world.getServer().runCommand("item replace entity @a hotbar.1 with respawn_anchor 4");
		world.getServer().runCommand("item replace entity @a hotbar.2 with glowstone 16");
		world.getServer().runCommand("item replace entity @a hotbar.3 with totem_of_undying");
		context.waitTicks(5);
		context.runOnClient(mc -> {
			AnchorMacroModule anchor = (AnchorMacroModule) module("anchor_macro");
			anchor.setEnabled(true);
			anchor.requestPlace(mc);
		});
		context.waitTicks(60);
		context.takeScreenshot("n4y-combat-02-anchor-place");
		context.runOnClient(mc -> {
			BlockState state = mc.level.getBlockState(above());
			if (!state.is(Blocks.RESPAWN_ANCHOR)) {
				throw new AssertionError("Place did not put a respawn anchor on the aimed block, found " + state);
			}
			if (state.getValue(RespawnAnchorBlock.CHARGE) < 1) {
				throw new AssertionError("Place did not charge the anchor");
			}
			if (!mc.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
				throw new AssertionError("Place did not finish with the totem in hand, holding "
						+ mc.player.getMainHandItem());
			}
			AnchorMacroModule anchor = (AnchorMacroModule) module("anchor_macro");
			if (!anchor.ownedAnchors().contains(above())) {
				throw new AssertionError("The anchor it placed was not recorded as its own");
			}
			anchor.setEnabled(false);
		});
		// A charged anchor explodes outside the Nether: remove it rather than leave it armed.
		world.getServer().runCommand("setblock " + px + " " + py + " " + (pz + 2) + " air");
		context.waitTicks(5);
	}

	// ---------------------------------------------------------------- TriggerBot

	private void testTriggerBot(ClientGameTestContext context, TestSingleplayerContext world) {
		resetArena(context, world);
		world.getServer().runCommand("item replace entity @a hotbar.0 with wooden_sword");
		world.getServer().runCommand("summon pig " + (px + 0.5) + " " + py + " " + (pz + 2.5) + " {NoAI:1b,Silent:1b}");
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.player.getInventory().setSelectedSlot(0);
			Module trigger = module("trigger_bot");
			((BooleanSetting) trigger.setting("all_entities").orElseThrow()).set(true);
			trigger.setEnabled(true);
		});
		context.waitTicks(30);
		context.takeScreenshot("n4y-combat-03-triggerbot");
		context.runOnClient(mc -> {
			List<Pig> pigs = mc.level.getEntitiesOfClass(Pig.class, new AABB(above()).inflate(2));
			boolean hurt = pigs.isEmpty() || pigs.get(0).getHealth() < pigs.get(0).getMaxHealth();
			if (!hurt) {
				throw new AssertionError("TriggerBot did not hit the pig under the crosshair");
			}
			Module trigger = module("trigger_bot");
			trigger.setEnabled(false);
			trigger.resetSettings();
		});
	}

	// ---------------------------------------------------------------- AutoCrystal

	private void testAutoCrystal(ClientGameTestContext context, TestSingleplayerContext world) {
		resetArena(context, world);
		world.getServer().runCommand("setblock " + px + " " + (py - 1) + " " + (pz + 2) + " obsidian");
		world.getServer().runCommand("item replace entity @a hotbar.0 with end_crystal 64");
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.player.getInventory().setSelectedSlot(0);
			Module crystal = module("auto_crystal");
			((KeySetting) crystal.setting("activate_key").orElseThrow()).set(InputBinding.UNBOUND);
			crystal.setEnabled(true);
		});

		boolean placed = false;
		boolean broken = false;
		for (int tick = 0; tick < 80 && !(placed && broken); tick++) {
			context.waitTick();
			int crystals = context.computeOnClient(N4yCombatGameTest::countCrystals);
			if (crystals > 0) {
				if (!placed) {
					context.takeScreenshot("n4y-combat-04-crystal-placed");
				}
				placed = true;
			} else if (placed) {
				broken = true;
			}
		}
		final boolean wasPlaced = placed;
		final boolean wasBroken = broken;
		context.runOnClient(mc -> {
			Module crystal = module("auto_crystal");
			crystal.setEnabled(false);
			crystal.resetSettings();
			if (!wasPlaced) {
				throw new AssertionError("AutoCrystal did not place a crystal on the obsidian under the crosshair");
			}
			if (!wasBroken) {
				throw new AssertionError("AutoCrystal did not break the crystal under the crosshair");
			}
		});
	}

	private static int countCrystals(Minecraft mc) {
		return mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(6)).size();
	}

	// ---------------------------------------------------------------- interface

	private void showCombatInterface(ClientGameTestContext context) {
		context.setScreen(N4yMainScreen::new);
		context.waitTicks(15);
		double[] pixels = context.computeOnClient(mc -> {
			N4yMainScreen screen = (N4yMainScreen) mc.screen;
			var combatItem = screen.categoryItems().stream()
					.filter(item -> item.category() == com.n4y.client.module.ModuleCategory.COMBAT).findFirst().orElseThrow();
			return screen.centreOf(combatItem);
		});
		context.getInput().setCursorPos(pixels[0], pixels[1]);
		context.waitTick();
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		context.takeScreenshot("n4y-combat-05-category");

		context.runOnClient(mc -> ((N4yMainScreen) mc.screen).openDetail(module("trigger_bot")));
		context.waitTicks(10);
		context.takeScreenshot("n4y-combat-06-triggerbot-settings");
		context.runOnClient(mc -> ((N4yMainScreen) mc.screen).openDetail(module("anchor_macro")));
		context.waitTicks(10);
		context.takeScreenshot("n4y-combat-07-anchor-settings");
	}
}
