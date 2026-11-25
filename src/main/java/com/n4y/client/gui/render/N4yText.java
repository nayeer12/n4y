package com.n4y.client.gui.render;

import java.util.LinkedHashMap;
import java.util.Map;

import com.n4y.client.N4y;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Text in the n4y typography (Inter, bundled under OFL). Glyphs missing from Inter fall back to
 * the vanilla font through the font definition, so any language still renders.
 *
 * <p>Two things here are tuned for the render path. The styles are built once instead of per
 * call: {@code withStyle(style -> ...)} ran the lambda and allocated a fresh {@link Style} every
 * time text was measured or drawn, which on a screen full of labels is thousands of short-lived
 * objects per second. And measuring is memoised, because widths are asked for far more often
 * than they change: a label is measured to lay it out, again to centre it, and again on the next
 * frame, always with the same answer.
 *
 * <p>Render thread only.
 */
public final class N4yText {
	public static final FontDescription REGULAR =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "inter_regular"));
	public static final FontDescription SEMIBOLD =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(N4y.MOD_ID, "inter_semibold"));

	private static final Style REGULAR_STYLE = Style.EMPTY.withFont(REGULAR);
	private static final Style SEMIBOLD_STYLE = Style.EMPTY.withFont(SEMIBOLD);

	private static final String ELLIPSIS = "…";
	/** Bounded so changing text (coordinates, timers) cannot grow the cache without end. */
	private static final int MAX_MEASURED = 256;

	/** Bumped whenever measurements become stale, so callers can drop their own cached layout. */
	private static int metricsGeneration;

	/** One label: the component handed to the font and the width it measured. */
	private record Measured(Component component, float width) {
	}

	/**
	 * Resolved text per component, keyed by equality and bounded.
	 *
	 * <p>Equality rather than identity so that a caller which rebuilds an equal component each
	 * frame still hits the cache, and bounded because a render path must not be able to grow a
	 * map without end — the two together mean no call site can turn this into a leak.
	 */
	private static final Map<Component, String> RESOLVED = new LinkedHashMap<>(MAX_MEASURED * 2, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Component, String> eldest) {
			return size() > MAX_MEASURED;
		}
	};

	private static final Map<String, Measured> REGULAR_LABELS = labelCache();
	private static final Map<String, Measured> SEMIBOLD_LABELS = labelCache();

	/** One trimmed label, remembered so the search is not repeated on every frame. */
	private record Trim(String text, boolean bold, float scale, float maxWidth, boolean fromStart) {
	}

	private static final Map<Trim, String> TRIMMED = new LinkedHashMap<>(MAX_MEASURED * 2, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Trim, String> eldest) {
			return size() > MAX_MEASURED;
		}
	};

	private N4yText() {
	}

	private static Map<String, Measured> labelCache() {
		return new LinkedHashMap<>(MAX_MEASURED * 2, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Measured> eldest) {
				return size() > MAX_MEASURED;
			}
		};
	}

	/**
	 * Drops the measured widths. Call it when the font or the language changes, because the same
	 * string can then be a different width.
	 */
	public static void invalidateMetrics() {
		metricsGeneration++;
		REGULAR_LABELS.clear();
		SEMIBOLD_LABELS.clear();
		TRIMMED.clear();
		RESOLVED.clear();
	}

	/**
	 * Changes when the measured widths were thrown away. Anything that caches a width or a laid
	 * out line compares this and recomputes when it differs, so a change of language or font does
	 * not leave a panel sized for the old text.
	 */
	public static int metricsGeneration() {
		return metricsGeneration;
	}

	/**
	 * A fresh component in the n4y font. Callers that keep or further style the result want this;
	 * drawing and measuring inside this class use the shared, cached one instead.
	 */
	/**
	 * The text of {@code component}, resolved once per component.
	 *
	 * <p>{@code Component.getString()} walks the component and builds a new string every call, so
	 * a label drawn every frame was being translated and rebuilt every frame. Cleared by
	 * {@link #invalidateMetrics()}, which runs when the resource packs, and therefore the
	 * language, reload.
	 */
	public static String resolve(Component component) {
		if (component == null) {
			return "";
		}
		String cached = RESOLVED.get(component);
		if (cached != null) {
			return cached;
		}
		String text = component.getString();
		RESOLVED.put(component, text);
		return text;
	}

	public static Component of(String text, boolean bold) {
		return Component.literal(text).setStyle(bold ? SEMIBOLD_STYLE : REGULAR_STYLE);
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	/**
	 * The component and width for {@code text}, built once per distinct string. Holding the
	 * component as well as the width means drawing a label that was drawn before allocates
	 * nothing: the same line of text is otherwise wrapped in a new component every frame.
	 */
	private static Measured measured(String text, boolean bold) {
		Map<String, Measured> cache = bold ? SEMIBOLD_LABELS : REGULAR_LABELS;
		Measured cached = cache.get(text);
		if (cached != null) {
			return cached;
		}
		Component component = of(text, bold);
		Measured built = new Measured(component, font().width(component));
		cache.put(text, built);
		return built;
	}

	/** Width in GUI units at scale 1, measured once per distinct string. */
	private static float unscaledWidth(String text, boolean bold) {
		return text.isEmpty() ? 0f : measured(text, bold).width();
	}

	/**
	 * Measures without caching. The trimming search probes a handful of substrings that are
	 * thrown away immediately; keeping them would push the labels that are measured every frame
	 * out of the cache.
	 */
	private static float measureRaw(String text, boolean bold) {
		return text.isEmpty() ? 0f : font().width(of(text, bold));
	}

	public static float width(String text, boolean bold, float scale) {
		return unscaledWidth(text, bold) * scale;
	}

	public static float height(float scale) {
		return font().lineHeight * scale;
	}

	/** Draws at sub-pixel position and arbitrary scale; {@code color} must include alpha. */
	public static void draw(GuiGraphics g, String text, float x, float y, float scale, int color, boolean bold) {
		if ((color >>> 24) == 0 || text == null || text.isEmpty()) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.drawString(font(), measured(text, bold).component(), 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** Draws with the text vertically centred on {@code centerY}. */
	public static void drawMiddle(GuiGraphics g, String text, float x, float centerY, float scale, int color, boolean bold) {
		draw(g, text, x, centerY - 4.5f * scale, scale, color, bold);
	}

	/** Draws centred on {@code centerX} and {@code centerY}. */
	public static void drawCentered(GuiGraphics g, String text, float centerX, float centerY, float scale, int color, boolean bold) {
		draw(g, text, centerX - width(text, bold, scale) / 2f, centerY - 4.5f * scale, scale, color, bold);
	}

	/**
	 * Trims {@code text} with an ellipsis so it fits in {@code maxWidth} GUI units.
	 *
	 * <p>The cut is found by halving the range instead of walking back one character at a time,
	 * so a long label costs about eight measurements rather than one per character it has to
	 * drop. A cut never lands between the two halves of a surrogate pair.
	 */
	public static String ellipsize(String text, boolean bold, float scale, float maxWidth) {
		return trim(text, bold, scale, maxWidth, false);
	}

	/** Trims from the left, keeping the end visible: what a text field shows while typing. */
	public static String ellipsizeStart(String text, boolean bold, float scale, float maxWidth) {
		return trim(text, bold, scale, maxWidth, true);
	}

	private static String trim(String text, boolean bold, float scale, float maxWidth, boolean fromStart) {
		if (text == null || text.isEmpty() || width(text, bold, scale) <= maxWidth) {
			return text;
		}
		Trim key = new Trim(text, bold, scale, maxWidth, fromStart);
		String cached = TRIMMED.get(key);
		if (cached != null) {
			return cached;
		}
		String result = search(text, bold, scale, maxWidth, fromStart);
		TRIMMED.put(key, result);
		return result;
	}

	private static String search(String text, boolean bold, float scale, float maxWidth, boolean fromStart) {
		float budget = maxWidth - width(ELLIPSIS, bold, scale);
		if (budget <= 0f) {
			return ELLIPSIS;
		}
		int low = 0;
		int high = text.length();
		if (!fromStart) {
			// Largest prefix that still fits: the width grows with the cut, so halve the range.
			while (low < high) {
				int mid = (low + high + 1) >>> 1;
				if (measureRaw(text.substring(0, mid), bold) * scale <= budget) {
					low = mid;
				} else {
					high = mid - 1;
				}
			}
			int end = low;
			if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
				end--;
			}
			return text.substring(0, end) + ELLIPSIS;
		}
		// Smallest start whose remainder fits: the suffix gets shorter as the start grows.
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (measureRaw(text.substring(mid), bold) * scale <= budget) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		int start = low;
		if (start < text.length() && Character.isLowSurrogate(text.charAt(start))) {
			start++;
		}
		return ELLIPSIS + text.substring(start);
	}
}
