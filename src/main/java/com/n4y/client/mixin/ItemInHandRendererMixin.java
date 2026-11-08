package com.n4y.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;

/** Lets the Hide Hand module keep the first-person view clear for recording. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void n4y$hideHand(float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
			LocalPlayer player, int light, CallbackInfo ci) {
		if (ModuleFlags.hideHand) {
			ci.cancel();
		}
	}
}
