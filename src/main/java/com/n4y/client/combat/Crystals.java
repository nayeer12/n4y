package com.n4y.client.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The rules an end crystal follows, taken from {@code EndCrystalItem.useOn} rather than
 * guessed: the block underneath must be obsidian or bedrock, the space above it must be empty,
 * and nothing may be standing in the two blocks the crystal would occupy.
 *
 * <p>The client only knows about entities it has been told about, so a placement can still be
 * refused by the server; the modules treat a refusal as a normal outcome.
 */
public final class Crystals {
	private Crystals() {
	}

	/** True when {@code support} is a block a crystal can stand on. */
	public static boolean isSupport(Level level, BlockPos support) {
		BlockState state = level.getBlockState(support);
		return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
	}

	/** True when a crystal placed on {@code support} would be accepted. */
	public static boolean canPlaceOn(Level level, BlockPos support) {
		if (!isSupport(level, support)) {
			return false;
		}
		BlockPos above = support.above();
		if (!level.isEmptyBlock(above)) {
			return false;
		}
		AABB space = new AABB(above.getX(), above.getY(), above.getZ(),
				above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
		return level.getEntities((Entity) null, space, entity -> true).isEmpty();
	}

	/** The entities AutoCrystal is allowed to hit: crystals, and slimes as in the original. */
	public static boolean isBreakable(Entity entity) {
		return entity instanceof EndCrystal || entity instanceof Slime;
	}
}
