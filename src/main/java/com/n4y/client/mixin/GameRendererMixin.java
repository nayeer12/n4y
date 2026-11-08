package com.n4y.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.renderer.GameRenderer;

/** Lets the No Hurt Cam module drop the camera shake when the player is damaged. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void n4y$noHurtCam(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		if (ModuleFlags.noHurtCam) {
			ci.cancel();
		}
	}
}
