package com.n4y.client.module;

/**
 * Switches read by the mixins. Rendering code runs thousands of times per second, so it reads
 * a plain field here instead of looking a module up; the modules write these in
 * {@code onEnable}/{@code onDisable}.
 */
public final class ModuleFlags {
	/** Skips the camera tilt when the player takes damage. */
	public static volatile boolean noHurtCam;
	/** Skips drawing the first-person hands and the item they hold. */
	public static volatile boolean hideHand;
	/** Skips the flames drawn over the screen while burning. */
	public static volatile boolean noFireOverlay;

	private ModuleFlags() {
	}

	/** Clears every flag, so nothing is left on if a module fails to disable itself. */
	public static void reset() {
		noHurtCam = false;
		hideHand = false;
		noFireOverlay = false;
	}
}
