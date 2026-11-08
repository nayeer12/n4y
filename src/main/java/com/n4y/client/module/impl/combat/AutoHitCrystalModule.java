package com.n4y.client.module.impl.combat;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.combat.Chance;
import com.n4y.client.combat.Clock;
import com.n4y.client.combat.CombatContext;
import com.n4y.client.combat.Crystals;
import com.n4y.client.combat.Inventories;
import com.n4y.client.combat.Targets;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hit-crystal in one key: places obsidian on the block under the crosshair, switches to an end
 * crystal, then hands over to the shared crystal routine, the same logic AutoCrystal uses.
 *
 * <p>Every slot change is a normal, visible selection. Nothing outside the crosshair is
 * touched. The crystal phase uses AutoCrystal's delays and chances, as the original did.
 */
public final class AutoHitCrystalModule extends Module {
	private final KeySetting activateKey = new KeySetting("activate_key", InputBinding.mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
	private final BooleanSetting checkPlace = new BooleanSetting("check_place", false);
	private final IntSetting switchDelay = new IntSetting("switch_delay", 0, 20, 0, 1);
	private final IntSetting switchChance = new IntSetting("switch_chance", 0, 100, 100, 1);
	private final IntSetting placeDelay = new IntSetting("place_delay", 0, 20, 0, 1);
	private final IntSetting placeChance = new IntSetting("place_chance", 0, 100, 100, 1);
	private final BooleanSetting workWithTotem = new BooleanSetting("work_with_totem", false);
	private final BooleanSetting workWithCrystal = new BooleanSetting("work_with_crystal", false);
	private final BooleanSetting swordSwap = new BooleanSetting("sword_swap", true);

	private final CombatContext combat;
	private final AutoCrystalModule autoCrystal;
	private final Clock switchClock = new Clock();
	private final Clock placeClock = new Clock();
	private boolean active;
	private boolean crystalling;

	public AutoHitCrystalModule(CombatContext combat, AutoCrystalModule autoCrystal) {
		super("auto_hit_crystal", ModuleCategory.COMBAT, Icons.SWORDS);
		this.combat = combat;
		this.autoCrystal = autoCrystal;
		addSettings(activateKey, checkPlace, switchDelay, switchChance, placeDelay, placeChance,
				workWithTotem, workWithCrystal, swordSwap);
	}

	private void reset() {
		switchClock.arm(switchDelay.value());
		placeClock.arm(placeDelay.value());
		active = false;
		crystalling = false;
	}

	@Override
	protected void onEnable(Minecraft mc) {
		reset();
	}

	@Override
	protected void onDisable(Minecraft mc) {
		reset();
	}

	private boolean startsSequence(ItemStack held) {
		return held.is(ItemTags.SWORDS)
				|| workWithTotem.value() && held.is(Items.TOTEM_OF_UNDYING)
				|| workWithCrystal.value() && held.is(Items.END_CRYSTAL);
	}

	@Override
	public void onTick(Minecraft mc) {
		switchClock.tick();
		placeClock.tick();
		if (mc.player == null || mc.level == null || mc.screen != null) {
			return;
		}
		if (!activateKey.isBound() || !activateKey.binding().isDown(mc.getWindow())) {
			if (active) {
				reset();
			}
			return;
		}

		BlockHitResult hit = Targets.blockUnderCrosshair(mc);
		if (!active) {
			if (checkPlace.value() && hit != null && !canPlaceObsidian(mc.level, hit)) {
				return;
			}
			if (!startsSequence(mc.player.getMainHandItem())) {
				return;
			}
			// Sword Swap: already aiming at obsidian or bedrock, so go straight to the crystal.
			if (swordSwap.value() && hit != null) {
				crystalling = Crystals.isSupport(mc.level, hit.getBlockPos());
			}
			active = true;
		}

		if (!crystalling) {
			placeObsidian(mc, hit);
		}
		if (crystalling) {
			crystalPhase(mc);
		}
	}

	private void placeObsidian(Minecraft mc, BlockHitResult hit) {
		if (hit == null) {
			return;
		}
		BlockState target = mc.level.getBlockState(hit.getBlockPos());
		if (target.is(Blocks.OBSIDIAN)) {
			return;
		}
		// Never put a block against a charged anchor: the click would detonate it.
		if (target.is(Blocks.RESPAWN_ANCHOR) && target.getValue(RespawnAnchorBlock.CHARGE) > 0) {
			return;
		}
		if (!mc.player.getMainHandItem().is(Items.OBSIDIAN)) {
			if (switchClock.ready() && Chance.roll(switchChance.value())
					&& Inventories.selectFromHotbar(mc, Items.OBSIDIAN)) {
				switchClock.arm(switchDelay.value());
			}
			return;
		}
		if (placeClock.ready() && Chance.roll(placeChance.value()) && combat.interactions().useOn(mc, hit)) {
			placeClock.arm(placeDelay.value());
			switchClock.arm(switchDelay.value());
			crystalling = true;
		}
	}

	private void crystalPhase(Minecraft mc) {
		if (!mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
			if (switchClock.ready() && Chance.roll(switchChance.value())
					&& Inventories.selectFromHotbar(mc, Items.END_CRYSTAL)) {
				switchClock.arm(switchDelay.value());
			}
			return;
		}
		// When AutoCrystal is on it already runs this routine on its own key; driving it twice
		// in the same tick is harmless (it guards itself) but there is no need to.
		if (!autoCrystal.isEnabled()) {
			combat.crystals().step(mc, autoCrystal.placeDelay(), autoCrystal.breakDelay(),
					autoCrystal.placeChance(), autoCrystal.breakChance());
		}
	}

	/** Check Place: the obsidian would go on the clicked face, into free, replaceable space. */
	static boolean canPlaceObsidian(Level level, BlockHitResult hit) {
		BlockPos clicked = hit.getBlockPos();
		BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		if (!level.getBlockState(target).canBeReplaced()) {
			return false;
		}
		return level.getEntities((Entity) null, new AABB(target), entity -> entity.blocksBuilding).isEmpty();
	}
}
