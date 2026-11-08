package com.n4y.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static identity of n4y Client: ids, names and the shared logger.
 * Holds no mutable state.
 */
public final class N4y {
	public static final String MOD_ID = "n4y";
	public static final String NAME = "n4y Client";
	public static final String AUTHOR = "4xyns";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private N4y() {
	}

	/** Version read from fabric.mod.json (filled in by Gradle from gradle.properties). */
	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
	}
}
