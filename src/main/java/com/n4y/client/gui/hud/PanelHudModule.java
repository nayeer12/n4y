package com.n4y.client.gui.hud;

import java.util.Arrays;

import com.n4y.client.N4y;
import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An on-screen element drawn as the shared glass card: an icon and a fixed number of text lines.
 *
 * <p>This exists because the three overlays that use that card each repeated the same
 * {@code width}/{@code height}/{@code drawHud} trio, and each of them rebuilt its text inside
 * every one of those methods. The manager measures an element before it draws it, so the text
 * was being rebuilt two or three times per frame — at 200 FPS that is six hundred rounds of
 * {@code String.format}, translation lookups and throwaway arrays per second to show numbers a
 * player cannot read changing more than a few times a second.
 *
 * <p>So the content is rebuilt here at most once per {@link #refreshMillis()}, into a buffer this
 * object owns, and the measured width is remembered until a line actually changes. A subclass
 * only says what its lines contain. Everything else — when to refresh, how wide the card is, how
 * it is drawn, what happens when the game is not ready — lives here, once.
 */
public abstract class PanelHudModule extends HudModule {
	private final String[] lines;
	private final long refreshMillis;
	private final long refreshNanos;
	/** Reused by subclasses to build a line without allocating a formatter. */
	private final StringBuilder scratch = new StringBuilder(48);

	private long lastRefresh;
	private boolean refreshed;
	private float cachedWidth = -1f;
	private int measuredAtGeneration = -1;

	protected PanelHudModule(String id, ModuleCategory category, Icon icon, HudPlacement defaultPlacement,
			int lineCount, long refreshMillis) {
		super(id, category, icon, defaultPlacement);
		if (lineCount <= 0) {
			throw new IllegalArgumentException("a panel needs at least one line, got " + lineCount);
		}
		this.lines = new String[lineCount];
		Arrays.fill(this.lines, "");
		this.refreshMillis = Math.max(0L, refreshMillis);
		this.refreshNanos = this.refreshMillis * 1_000_000L;
	}

	/** How long a built line stays good for. Smaller means smoother numbers and more work. */
	protected final long refreshMillis() {
		return refreshMillis;
	}

	/**
	 * Rebuilds the lines by calling {@link #line()} and {@link #set}. Called on the render
	 * thread, at most once per refresh interval, and never more than once per frame.
	 *
	 * @param mc the game; {@code mc.player} and {@code mc.level} may be null
	 */
	protected abstract void refresh(Minecraft mc);

	/** Clears and returns the shared buffer, ready for a line to be appended into it. */
	protected final StringBuilder line() {
		scratch.setLength(0);
		return scratch;
	}

	/**
	 * Stores what {@link #line()} built as line {@code index}.
	 *
	 * <p>When the text is the same as last time nothing is stored and no string is created, so an
	 * overlay showing a value that is not moving allocates nothing at all.
	 */
	protected final void set(int index, StringBuilder built) {
		String current = lines[index];
		if (current != null && current.contentEquals(built)) {
			return;
		}
		lines[index] = built.toString();
		cachedWidth = -1f;
	}

	/** Stores an already-built string as line {@code index}. */
	protected final void set(int index, String value) {
		String safe = value == null ? "" : value;
		if (safe.equals(lines[index])) {
			return;
		}
		lines[index] = safe;
		cachedWidth = -1f;
	}

	/**
	 * Refreshes the content if it is stale. Every public entry point calls this, so the element
	 * is correct whoever asks it first — the manager, the HUD Studio preview or a test — and is
	 * still only rebuilt once.
	 */
	void refreshIfStale(Minecraft mc) {
		// nanoTime, not currentTimeMillis: the wall clock can step backwards when it is
		// corrected, and an interval measured against it would then stall the overlay.
		long now = System.nanoTime();
		if (refreshed && now - lastRefresh < refreshNanos) {
			return;
		}
		lastRefresh = now;
		refreshed = true;
		try {
			refresh(mc);
		} catch (Throwable t) {
			// An overlay must never be able to take the frame down with it.
			N4y.LOGGER.error("HUD element '{}' failed to refresh", id(), t);
			Arrays.fill(lines, "");
			cachedWidth = -1f;
		}
	}

	/** The text of line {@code index} as it stands, for the layout and for tests. */
	final String line(int index) {
		return lines[index];
	}

	@Override
	public final float width(Minecraft mc) {
		refreshIfStale(mc);
		int generation = N4yText.metricsGeneration();
		if (cachedWidth < 0f || generation != measuredAtGeneration) {
			cachedWidth = HudPanel.width(lines);
			measuredAtGeneration = generation;
		}
		return cachedWidth;
	}

	@Override
	public final float height(Minecraft mc) {
		return HudPanel.height(lines.length);
	}

	@Override
	public final void drawHud(GuiGraphics g, Minecraft mc, Theme theme, float x, float y) {
		HudPanel.draw(g, theme, x, y, width(mc), icon(), lines);
	}
}
