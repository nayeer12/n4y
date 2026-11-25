package com.n4y.client.gui.icon;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.n4y.client.N4y;
import com.n4y.client.core.IntKeyCache;
import com.n4y.client.gui.render.UiScale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Rasterises {@link Icon}s into cached alpha masks at the exact physical pixel size they are
 * drawn at, then blits them tinted. Render-thread only.
 *
 * <p>The cache is indexed by icon identity and then by pixel size, which costs no allocation per
 * lookup. The icons are the constants in {@link Icons}, so identity is the right comparison and
 * also the cheap one: {@link Icon} is a record over lists of coordinate arrays, so hashing one
 * would walk every path in it, and the old key concatenated a string on every draw.
 *
 * <p>Sizes are bounded per icon rather than globally, so the set of live textures is naturally
 * limited and a change of GUI scale retires the sizes it replaced instead of flushing the lot.
 */
public final class IconTextures {
	/** Enough for the handful of sizes a layout uses, plus a scale change in flight. */
	private static final int MAX_SIZES_PER_ICON = 8;

	private static final Map<Icon, IntKeyCache<Identifier>> CACHE = new IdentityHashMap<>();

	private IconTextures() {
	}

	/** Draws {@code icon} in a square of {@code size} GUI units with its top-left at (x, y). */
	public static void draw(GuiGraphics g, Icon icon, float x, float y, float size, int color) {
		if (icon == null || (color >>> 24) == 0 || size <= 0) {
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
		IntKeyCache<Identifier> sizes = CACHE.computeIfAbsent(icon, ignored -> new IntKeyCache<>(MAX_SIZES_PER_ICON));
		Identifier cached = sizes.get(size);
		if (cached != null) {
			return cached;
		}
		Identifier id = rasterise(icon, size);
		sizes.put(size, id, IconTextures::release);
		return id;
	}

	private static Identifier rasterise(Icon icon, int size) {
		NativeImage image = new NativeImage(size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int a = Math.round(IconRaster.coverage(icon, x, y, size) * 255f);
				image.setPixel(x, y, (a << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(N4y.MOD_ID, "dynamic/icon/" + icon.key() + "_" + size);
		Minecraft.getInstance().getTextureManager().register(id, new LinearTexture(id.toString(), image));
		return id;
	}

	private static void release(Identifier id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getTextureManager() != null) {
			mc.getTextureManager().release(id);
		}
	}

	public static void clear() {
		for (IntKeyCache<Identifier> sizes : CACHE.values()) {
			sizes.clear(IconTextures::release);
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
