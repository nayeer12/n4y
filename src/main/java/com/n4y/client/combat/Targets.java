package com.n4y.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** What the player is looking at, and the conditions a hit has to meet. */
public final class Targets {
	private Targets() {
	}

	/** The block under the crosshair, or null when the player is not looking at one. */
	public static BlockHitResult blockUnderCrosshair(Minecraft mc) {
		return mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit : null;
	}

	/** The entity under the crosshair, or null. */
	public static Entity entityUnderCrosshair(Minecraft mc) {
		return mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
	}

	/**
	 * The seven conditions vanilla checks in {@code Player.canCriticalAttack}. Modules use this
	 * instead of only looking at the fall distance, so "only criticals" means what the game
	 * means by a critical.
	 */
	public static boolean wouldCrit(LocalPlayer player, Entity target) {
		return player.fallDistance > 0.0
				&& !player.onGround()
				&& !player.onClimbable()
				&& !player.isInWater()
				&& !player.isMobilityRestricted()
				&& !player.isPassenger()
				&& target instanceof LivingEntity
				&& !player.isSprinting();
	}

	/**
	 * True when the target is blocking but its shield cannot cover a hit coming from
	 * {@code attacker}. Vanilla blocks within 90° of where the target looks, so anything
	 * behind that line lands.
	 */
	public static boolean isShieldFacingAway(Player target, Player attacker) {
		Vec3 look = target.getViewVector(1.0F);
		Vec3 toAttacker = attacker.position().subtract(target.position());
		double dot = look.x * toAttacker.x + look.z * toAttacker.z;
		return dot < 0.0;
	}

	/** True when the shield of {@code target} would stop a hit from {@code attacker}. */
	public static boolean isProtectedByShield(Entity target, Player attacker) {
		return target instanceof Player player && player.isBlocking() && !isShieldFacingAway(player, attacker);
	}

	/**
	 * True when a dead or dying player is within {@code radius} blocks, which is what the
	 * original "stop on kill" looked for.
	 */
	public static boolean deadPlayerNearby(Minecraft mc, double radius) {
		if (mc.level == null || mc.player == null) {
			return false;
		}
		double radiusSqr = radius * radius;
		for (Player player : mc.level.players()) {
			if (player != mc.player && player.distanceToSqr(mc.player) <= radiusSqr
					&& (!player.isAlive() || player.deathTime > 0)) {
				return true;
			}
		}
		return false;
	}
}
