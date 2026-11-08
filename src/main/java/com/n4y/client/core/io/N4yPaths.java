package com.n4y.client.core.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.n4y.client.N4y;

import net.fabricmc.loader.api.FabricLoader;

/** Every file n4y writes lives under {@code .minecraft/config/n4y/}. */
public final class N4yPaths {
	private N4yPaths() {
	}

	public static Path root() {
		return FabricLoader.getInstance().getConfigDir().resolve(N4y.MOD_ID);
	}

	public static Path profiles() {
		return root().resolve("profiles");
	}

	/** Creates the directory tree; failures are logged, not thrown, so the game still starts. */
	public static void ensureCreated() {
		try {
			Files.createDirectories(profiles());
		} catch (IOException e) {
			N4y.LOGGER.error("Could not create config folder {}", root(), e);
		}
	}
}
