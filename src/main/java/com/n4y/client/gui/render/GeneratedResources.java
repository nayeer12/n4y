package com.n4y.client.gui.render;

import com.n4y.client.N4y;
import com.n4y.client.gui.icon.IconTextures;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Ties everything the client generates at runtime — shape masks, icon masks, measured text — to
 * the game's resource reload.
 *
 * <p>These caches are keyed by pixel size and by string, and the answers behind both change when
 * the resource packs do: the font is a resource, so the same label is a different width, and the
 * textures the client registered are stale. Nothing used to drop them, so pressing F3+T left a
 * client holding textures nobody would ask for again and panel widths measured for a font that
 * was no longer loaded.
 */
public final class GeneratedResources {
	private GeneratedResources() {
	}

	/**
	 * @param alsoInvalidate anything outside the render layer that caches resolved text, such as
	 *                       the module search index, which translations feed
	 */
	public static void registerReloadListener(Runnable alsoInvalidate) {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
				Identifier.fromNamespaceAndPath(N4y.MOD_ID, "generated"),
				new SimpleResourceReloader<Void>() {
					@Override
					protected Void prepare(PreparableReloadListener.SharedState state) {
						// Nothing to read off-thread: the masks are generated, not loaded.
						return null;
					}

					@Override
					protected void apply(Void prepared, PreparableReloadListener.SharedState state) {
						// Runs on the render thread, which is where textures may be released.
						ShapeTextures.clear();
						IconTextures.clear();
						N4yText.invalidateMetrics();
						if (alsoInvalidate != null) {
							alsoInvalidate.run();
						}
					}
				});
	}
}
