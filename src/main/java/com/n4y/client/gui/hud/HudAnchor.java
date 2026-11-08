package com.n4y.client.gui.hud;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Corner or edge an on-screen element is measured from, so it stays put at any resolution. */
public enum HudAnchor implements StringRepresentable {
	TOP_LEFT("top_left", 0f, 0f),
	TOP_CENTER("top_center", 0.5f, 0f),
	TOP_RIGHT("top_right", 1f, 0f),
	MIDDLE_LEFT("middle_left", 0f, 0.5f),
	CENTER("center", 0.5f, 0.5f),
	MIDDLE_RIGHT("middle_right", 1f, 0.5f),
	BOTTOM_LEFT("bottom_left", 0f, 1f),
	BOTTOM_CENTER("bottom_center", 0.5f, 1f),
	BOTTOM_RIGHT("bottom_right", 1f, 1f);

	public static final Codec<HudAnchor> CODEC = StringRepresentable.fromEnum(HudAnchor::values);

	private final String id;
	private final float fx;
	private final float fy;

	HudAnchor(String id, float fx, float fy) {
		this.id = id;
		this.fx = fx;
		this.fy = fy;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	public Component displayName() {
		return Component.translatable("n4y.anchor." + id);
	}

	/** Left edge of an element of {@code width} anchored on a screen of {@code screenWidth}. */
	public float originX(float screenWidth, float width) {
		return (screenWidth - width) * fx;
	}

	public float originY(float screenHeight, float height) {
		return (screenHeight - height) * fy;
	}

	/** The anchor whose origin is closest to {@code (x, y)}, used when dropping an element. */
	public static HudAnchor nearest(float x, float y, float width, float height, float screenWidth, float screenHeight) {
		HudAnchor best = TOP_LEFT;
		float bestDistance = Float.MAX_VALUE;
		for (HudAnchor anchor : values()) {
			float dx = x - anchor.originX(screenWidth, width);
			float dy = y - anchor.originY(screenHeight, height);
			float distance = dx * dx + dy * dy;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = anchor;
			}
		}
		return best;
	}
}
