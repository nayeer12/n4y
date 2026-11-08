package com.n4y.client.gui.render;

import com.n4y.client.N4y;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Text in the n4y typography (Inter, bundled under OFL). Glyphs missing from Inter fall back
 * to the vanilla font through the font definition, so any language still renders.
 */
public final class N4yText {
	public static final FontDescription REGULAR =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "inter_regular"));
	public static final FontDescription SEMIBOLD =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "inter_semibold"));

	private N4yText() {
	}

	public static Component of(String text, boolean bold) {
		FontDescription font = bold ? SEMIBOLD : REGULAR;
		return Component.literal(text).withStyle(style -> style.withFont(font));
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static float width(String text, boolean bold, float scale) {
		return font().width(of(text, bold)) * scale;
	}

	public static float height(float scale) {
		return font().lineHeight * scale;
	}

	/** Draws at sub-pixel position and arbitrary scale; {@code color} must include alpha. */
	public static void draw(GuiGraphics g, String text, float x, float y, float scale, int color, boolean bold) {
		if ((color >>> 24) == 0 || text.isEmpty()) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.drawString(font(), of(text, bold), 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** Trims {@code text} with an ellipsis so it fits in {@code maxWidth} GUI units. */
	public static String ellipsize(String text, boolean bold, float scale, float maxWidth) {
		if (width(text, bold, scale) <= maxWidth) {
			return text;
		}
		String dots = "…";
		int end = text.length();
		while (end > 0 && width(text.substring(0, end) + dots, bold, scale) > maxWidth) {
			end--;
		}
		return text.substring(0, end) + dots;
	}
}
