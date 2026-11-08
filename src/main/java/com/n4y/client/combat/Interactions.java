package com.n4y.client.combat;

import com.n4y.client.mixin.MinecraftAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The single door every combat module uses to touch the world. Everything goes through the
 * normal client calls, so the server sees exactly what it would see from a player: a real use,
 * a real attack, a real swing.
 *
 * <p>Uses are gated by the same four-tick delay vanilla imposes on itself. The gate reads
 * vanilla's own counter as well as our own, so a module and the player's held right click can
 * never place twice in the window where one placement belongs.
 */
public final class Interactions {
	/** Ticks vanilla waits between two uses; see {@code Minecraft.startUseItem}. */
	public static final int USE_COOLDOWN_TICKS = 4;

	private int ownCooldown;

	/** Counts our own cooldown down; called once per client tick. */
	public void tick() {
		if (ownCooldown > 0) {
			ownCooldown--;
		}
	}

	public void reset() {
		ownCooldown = 0;
	}

	/** Vanilla's own remaining delay between uses. */
	public static int vanillaUseDelay(Minecraft mc) {
		return ((MinecraftAccessor) mc).n4y$rightClickDelay();
	}

	/** True when using an item right now would respect both vanilla's pace and ours. */
	public boolean canUse(Minecraft mc) {
		return ownCooldown == 0
				&& vanillaUseDelay(mc) == 0
				&& mc.player != null
				&& !mc.player.isUsingItem();
	}

	/**
	 * Uses the held item on a block, swinging the hand when the game accepts it, exactly as a
	 * right click does.
	 *
	 * @return true when the interaction went through
	 */
	public boolean useOn(Minecraft mc, BlockHitResult hit) {
		if (!canUse(mc) || mc.gameMode == null || mc.player == null) {
			return false;
		}
		// Taking the slot costs the same wait whether or not the block accepts it, which is
		// what vanilla does, so a failing attempt cannot be retried every tick.
		ownCooldown = USE_COOLDOWN_TICKS;
		InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
		if (result.consumesAction()) {
			mc.player.swing(InteractionHand.MAIN_HAND);
			return true;
		}
		return false;
	}

	/** Starts using the item in {@code hand}, which is how a shield goes up. */
	public boolean startUsing(Minecraft mc, InteractionHand hand) {
		if (mc.gameMode == null || mc.player == null || mc.player.isUsingItem()) {
			return false;
		}
		return mc.gameMode.useItem(mc.player, hand).consumesAction();
	}

	/** Stops using the held item, which is how a shield comes down. */
	public void stopUsing(Minecraft mc) {
		if (mc.gameMode != null && mc.player != null && mc.player.isUsingItem()) {
			mc.gameMode.releaseUsingItem(mc.player);
		}
	}

	/** Attacks an entity the way a left click does. */
	public void attack(Minecraft mc, Entity target, boolean swing) {
		if (mc.gameMode == null || mc.player == null) {
			return;
		}
		mc.gameMode.attack(mc.player, target);
		if (swing) {
			mc.player.swing(InteractionHand.MAIN_HAND);
		}
	}
}
