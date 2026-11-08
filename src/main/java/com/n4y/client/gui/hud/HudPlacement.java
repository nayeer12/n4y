package com.n4y.client.gui.hud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where an on-screen element sits: an anchor plus an offset in GUI units from that anchor,
 * so a layout made on one screen still looks right on another.
 */
public record HudPlacement(HudAnchor anchor, float x, float y) {
	public static final Codec<HudPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			HudAnchor.CODEC.optionalFieldOf("anchor").xmap(
					value -> value.orElse(HudAnchor.TOP_LEFT), java.util.Optional::of).forGetter(HudPlacement::anchor),
			Codec.FLOAT.optionalFieldOf("x").xmap(value -> value.orElse(0f), java.util.Optional::of).forGetter(HudPlacement::x),
			Codec.FLOAT.optionalFieldOf("y").xmap(value -> value.orElse(0f), java.util.Optional::of).forGetter(HudPlacement::y)
	).apply(instance, HudPlacement::new));

	public static HudPlacement of(HudAnchor anchor, float x, float y) {
		return new HudPlacement(anchor, x, y);
	}

	public float screenX(float screenWidth, float width) {
		return anchor.originX(screenWidth, width) + x;
	}

	public float screenY(float screenHeight, float height) {
		return anchor.originY(screenHeight, height) + y;
	}

	/** Re-anchors to wherever the element was dropped, keeping it exactly where it looks. */
	public static HudPlacement fromScreen(float screenX, float screenY, float width, float height,
			float screenWidth, float screenHeight) {
		HudAnchor anchor = HudAnchor.nearest(screenX, screenY, width, height, screenWidth, screenHeight);
		return new HudPlacement(anchor,
				screenX - anchor.originX(screenWidth, width),
				screenY - anchor.originY(screenHeight, height));
	}
}
