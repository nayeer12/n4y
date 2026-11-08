package com.n4y.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The crystal logic of AutoCrystal, as a component of its own so AutoHitCrystal can use the
 * same logic without reaching into another module's callbacks.
 *
 * <p>One step looks only at the crosshair: it places a crystal on the obsidian or bedrock under
 * it, and hits the crystal or slime under it. Placing and breaking each have their own delay
 * and their own chance, rolled independently, and the routine runs at most once per tick even
 * when two modules drive it.
 */
public final class CrystalRoutine {
	private final Interactions interactions;
	private final Clock placeClock = new Clock();
	private final Clock breakClock = new Clock();
	private int lastTick = Integer.MIN_VALUE;

	public CrystalRoutine(Interactions interactions) {
		this.interactions = interactions;
	}

	/** Forgets pending waits, as when the activate key is released. */
	public void reset() {
		placeClock.clear();
		breakClock.clear();
	}

	/**
	 * Runs one step with the given timing.
	 *
	 * @param placeDelay  ticks to wait after a placement
	 * @param breakDelay  ticks to wait after a hit
	 * @param placeChance percent chance to place on an eligible tick
	 * @param breakChance percent chance to hit on an eligible tick
	 */
	public void step(Minecraft mc, int placeDelay, int breakDelay, int placeChance, int breakChance) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		int tick = mc.player.tickCount;
		if (tick == lastTick) {
			return;
		}
		lastTick = tick;
		placeClock.tick();
		breakClock.tick();

		BlockHitResult block = Targets.blockUnderCrosshair(mc);
		if (block != null && placeClock.ready()
				&& Crystals.canPlaceOn(mc.level, block.getBlockPos())
				&& Chance.roll(placeChance)
				&& interactions.useOn(mc, block)) {
			placeClock.arm(placeDelay);
		}

		Entity target = Targets.entityUnderCrosshair(mc);
		if (target != null && Crystals.isBreakable(target) && breakClock.ready() && Chance.roll(breakChance)) {
			interactions.attack(mc, target, true);
			breakClock.arm(breakDelay);
		}
	}
}
