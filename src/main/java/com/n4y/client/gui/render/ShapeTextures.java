package com.n4y.client.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.n4y.client.N4y;
import com.n4y.client.core.IntKeyCache;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Lazily generated, cached alpha masks used as nine-slice sources by {@link GlassRenderer}.
 * Textures are built at exact physical pixel sizes, so corners are anti-aliased and
 * pixel-perfect at every GUI scale.
 *
 * <p>Each shape kind has its own cache keyed by the integers that define it, so a lookup
 * allocates nothing. The previous key was a concatenated string, which meant one throwaway
 * {@code String} per lookup: a window of glass cards asks for a mask several times per card per
 * frame, so that was hundreds of allocations a frame to find something already in memory.
 *
 * <p>A full cache now drops its least recently used mask instead of releasing every mask it
 * holds. Flushing everything was a visible stall: one frame past the limit threw away textures
 * that were still on screen and rebuilt all of them, pixel by pixel, on the next frame.
 *
 * <p>Must be used from the render thread.
 */
public final class ShapeTextures {
	/** A generated mask: {@code corner} pixels of rounded corner on each side of a 1px centre. */
	public record Mask(Identifier id, int size, int corner) {
	}

	/** Per kind, so one kind churning cannot evict the others. */
	private static final int MAX_PER_KIND = 64;
	/** Radius, thickness and blur are physical pixels; this bounds them for key packing. */
	private static final int MAX_DIMENSION = 0xFFF;

	private static final IntKeyCache<Mask> FILLS = new IntKeyCache<>(MAX_PER_KIND);
	private static final IntKeyCache<Mask> RINGS = new IntKeyCache<>(MAX_PER_KIND);
	private static final IntKeyCache<Mask> SHADOWS = new IntKeyCache<>(MAX_PER_KIND);

	private ShapeTextures() {
	}

	public static Mask fill(int r) {
		int radius = clamp(r);
		Mask cached = FILLS.get(radius);
		if (cached != null) {
			return cached;
		}
		return store(FILLS, radius, "fill_" + radius, 2 * radius + 1, radius,
				(x, y) -> ShapeMath.fill(x, y, radius));
	}

	public static Mask ring(int r, int thickness) {
		int radius = clamp(r);
		int t = clamp(thickness);
		int key = pack(radius, t);
		Mask cached = RINGS.get(key);
		if (cached != null) {
			return cached;
		}
		return store(RINGS, key, "ring_" + radius + "_" + t, 2 * radius + 1, radius,
				(x, y) -> ShapeMath.ring(x, y, radius, t));
	}

	public static Mask shadow(int r, int blur) {
		int radius = clamp(r);
		int b = clamp(blur);
		int key = pack(radius, b);
		Mask cached = SHADOWS.get(key);
		if (cached != null) {
			return cached;
		}
		return store(SHADOWS, key, "shadow_" + radius + "_" + b, 2 * (radius + b) + 1, radius + b,
				(x, y) -> ShapeMath.shadow(x, y, radius, b));
	}

	private static int clamp(int value) {
		return Math.max(0, Math.min(value, MAX_DIMENSION));
	}

	private static int pack(int a, int b) {
		return (a << 12) | b;
	}

	@FunctionalInterface
	private interface Coverage {
		float at(int x, int y);
	}

	/**
	 * Generates and stores one mask. Each lookup above checks its cache before coming here, so
	 * that the texture name and the coverage function — a concatenated string and a lambda that
	 * closes over the radius — are only built when a mask really has to be drawn, not on every
	 * one of the hundreds of hits a frame.
	 */
	private static Mask store(IntKeyCache<Mask> cache, int key, String name, int size, int corner, Coverage coverage) {
		Mask mask = new Mask(rasterise(name, size, coverage), size, corner);
		cache.put(key, mask, evicted -> release(evicted.id()));
		return mask;
	}

	private static Identifier rasterise(String name, int size, Coverage coverage) {
		NativeImage image = new NativeImage(size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int a = Math.round(coverage.at(x, y) * 255f);
				image.setPixel(x, y, (a << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(N4y.MOD_ID, "dynamic/shape/" + name);
		Minecraft.getInstance().getTextureManager().register(id, new LinearTexture(id.toString(), image));
		return id;
	}

	private static void release(Identifier id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getTextureManager() != null) {
			mc.getTextureManager().release(id);
		}
	}

	/** Frees every generated texture (e.g. on resource reload). */
	public static void clear() {
		FILLS.clear(mask -> release(mask.id()));
		RINGS.clear(mask -> release(mask.id()));
		SHADOWS.clear(mask -> release(mask.id()));
	}

	/** Dynamic texture sampled with linear filtering so animated, scaled shapes stay smooth. */
	private static final class LinearTexture extends DynamicTexture {
		LinearTexture(String label, NativeImage image) {
			super(() -> label, image);
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
