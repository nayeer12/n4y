package com.n4y.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Minecraft;

/**
 * Read-only view of the delay vanilla imposes between two uses of an item, which is private
 * and has no getter. Combat modules read it so they never act faster than the game allows;
 * nothing writes to it.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("rightClickDelay")
	int n4y$rightClickDelay();
}
