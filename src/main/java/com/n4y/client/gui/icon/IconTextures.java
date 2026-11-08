package com.n4y.client.gui.icon;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.n4y.client.N4y;
import com.n4y.client.gui.render.UiScale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Rasterises {@link Icon}s into cached alpha masks at the exact physical pixel size they are
 * drawn at, then blits them tinted. Render-thread only.
 */
public final class IconTextures {
	private static final int MAX_CACHE = 128;
	private static final Map<String, Identifier> CACHE = new HashMap<>();

	private IconTextures() {
	}

	/** Draws {@code icon} in a square of {@code size} GUI units with its top-left at (x, y). */
	public static void draw(GuiGraphics g, Icon icon, float x, float y, float size, int color) {
		if ((color >>> 24) == 0 || size <= 0) {
			return;
		}
		float scale = UiScale.physical();
		int px = Math.max(4, Math.round(size * scale));
		Identifier id = texture(icon, px);
		g.pose().pushMatrix();
		g.pose().scale(1f / scale, 1f / scale);
		g.blit(RenderPipelines.GUI_TEXTURED, id, Math.round(x * scale), Math.round(y * scale), 0, 0, px, px, px, px, color);
		g.pose().popMatrix();
	}

	private static Identifier texture(Icon icon, int size) {
		String key = icon.key() + "_" + size;
		Identifier cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		if (CACHE.size() >= MAX_CACHE) {
			clear();
		}
		NativeImage image = new NativeImage(size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int a = Math.round(IconRaster.coverage(icon, x, y, size) * 255f);
				image.setPixel(x, y, (a << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(N4y.MOD_ID, "dynamic/icon/" + key);
		Minecraft.getInstance().getTextureManager().register(id, new LinearTexture(id.toString(), image));
		CACHE.put(key, id);
		return id;
	}

	public static void clear() {
		for (Identifier id : CACHE.values()) {
			Minecraft.getInstance().getTextureManager().release(id);
		}
		CACHE.clear();
	}

	private static final class LinearTexture extends DynamicTexture {
		LinearTexture(String label, NativeImage image) {
			super(() -> label, image);
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
