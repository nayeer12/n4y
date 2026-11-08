package com.n4y.client.gui.render;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.n4y.client.N4y;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Lazily generated, cached alpha masks used as nine-slice sources by {@link GlassRenderer}.
 * Textures are built at exact physical pixel sizes, so corners are anti-aliased and
 * pixel-perfect at every GUI scale. Must be used from the render thread.
 */
public final class ShapeTextures {
	/** A generated mask: {@code corner} pixels of rounded corner on each side of a 1px centre. */
	public record Mask(Identifier id, int size, int corner) {
	}

	private static final int MAX_CACHE = 192;
	private static final Map<String, Mask> CACHE = new HashMap<>();

	private ShapeTextures() {
	}

	public static Mask fill(int r) {
		return get("fill_" + r, 2 * r + 1, r, (x, y) -> ShapeMath.fill(x, y, r));
	}

	public static Mask ring(int r, int thickness) {
		return get("ring_" + r + "_" + thickness, 2 * r + 1, r, (x, y) -> ShapeMath.ring(x, y, r, thickness));
	}

	public static Mask shadow(int r, int blur) {
		return get("shadow_" + r + "_" + blur, 2 * (r + blur) + 1, r + blur, (x, y) -> ShapeMath.shadow(x, y, r, blur));
	}

	@FunctionalInterface
	private interface Coverage {
		float at(int x, int y);
	}

	private static Mask get(String key, int size, int corner, Coverage coverage) {
		Mask cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		if (CACHE.size() >= MAX_CACHE) {
			clear();
		}
		NativeImage image = new NativeImage(size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int a = Math.round(coverage.at(x, y) * 255f);
				image.setPixel(x, y, (a << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(N4y.MOD_ID, "dynamic/shape/" + key);
		Minecraft.getInstance().getTextureManager().register(id, new LinearTexture(id.toString(), image));
		Mask mask = new Mask(id, size, corner);
		CACHE.put(key, mask);
		return mask;
	}

	/** Frees every generated texture (e.g. on resource reload). */
	public static void clear() {
		for (Mask mask : CACHE.values()) {
			Minecraft.getInstance().getTextureManager().release(mask.id());
		}
		CACHE.clear();
	}

	/** Dynamic texture sampled with linear filtering so animated, scaled shapes stay smooth. */
	private static final class LinearTexture extends DynamicTexture {
		LinearTexture(String label, NativeImage image) {
			super(() -> label, image);
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
