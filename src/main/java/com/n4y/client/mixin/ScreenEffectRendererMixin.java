package com.n4y.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.n4y.client.module.ModuleFlags;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Lets the No Fire Overlay module clear the flames that cover the screen while burning. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
	private static void n4y$noFireOverlay(PoseStack poseStack, MultiBufferSource bufferSource,
			TextureAtlasSprite sprite, CallbackInfo ci) {
		if (ModuleFlags.noFireOverlay) {
			ci.cancel();
		}
	}
}
