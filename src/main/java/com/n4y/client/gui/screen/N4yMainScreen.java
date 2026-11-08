package com.n4y.client.gui.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.N4y;
import com.n4y.client.N4yClient;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.render.UiScale;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.gui.widget.SearchBoxWidget;
import com.n4y.client.gui.widget.SidebarItemWidget;
import com.n4y.client.gui.widget.TabButtonWidget;
import com.n4y.client.gui.widget.UiContext;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.ModuleManager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The main n4y interface: header with tabs and search, category sidebar and a scrollable grid
 * of module cards, plus the appearance tab holding the theme picker.
 *
 * <p>The screen owns layout and input; drawing is delegated to the widgets and the Glass
 * renderer, and every control maps to real state (a module or the theme engine).
 */
public final class N4yMainScreen extends Screen {
	/** The sections of the interface, selected by the header tabs. */
	public enum Tab {
		MODULES, APPEARANCE
	}

	private static final float PANEL_W = 480f;
	private static final float PANEL_H = 268f;
	/** Margin kept between the panel and the window edge, in GUI units. */
	private static final float MARGIN = 14f;
	private static final float PANEL_R = 16f;
	private static final float HEADER_H = 52f;
	private static final float PAD = 14f;
	private static final float SIDEBAR_W = 100f;
	private static final float CARD_H = 46f;
	private static final float GAP = 8f;
	private static final float SCROLLBAR_W = 7f;

	/** Kept between openings so the interface comes back where the user left it. */
	private static Tab lastTab = Tab.MODULES;
	private static ModuleCategory lastCategory = ModuleCategory.ALL;

	private final Animation open = new Animation(0f, 300, Easing.OUT_CUBIC);
	private final Animation scroll = new Animation(0f, 180, Easing.OUT_CUBIC);
	private final Map<Module, ModuleCardWidget> cards = new LinkedHashMap<>();
	private final List<SidebarItemWidget> sidebar = new ArrayList<>();
	private final List<TabButtonWidget> tabs = new ArrayList<>();
	private final List<ThemeOption> themeOptions = new ArrayList<>();
	private SearchBoxWidget search;

	private Tab tab = lastTab;
	private ModuleCategory category = lastCategory;
	private float scrollTarget;
	private float maxScroll;
	private float panelX;
	private float panelY;
	/** Uniform zoom that keeps the whole panel on screen, and the open-animation lift. */
	private float ui = 1f;
	private float lift;

	public N4yMainScreen() {
		super(Component.literal(N4y.NAME));
	}

	private ModuleManager modules() {
		return N4yClient.get().modules();
	}

	// ------------------------------------------------------------------ setup

	@Override
	protected void init() {
		open.animateTo(1f);
		cards.clear();
		sidebar.clear();
		tabs.clear();
		themeOptions.clear();

		for (Module module : modules().all()) {
			cards.put(module, new ModuleCardWidget(module));
		}
		for (ModuleCategory c : ModuleCategory.values()) {
			sidebar.add(new SidebarItemWidget(c, () -> category == c));
		}
		tabs.add(new TabButtonWidget(Icons.GRID, Component.translatable("n4y.tab.modules"), () -> tab == Tab.MODULES));
		tabs.add(new TabButtonWidget(Icons.PALETTE, Component.translatable("n4y.tab.appearance"), () -> tab == Tab.APPEARANCE));
		for (Theme t : Themes.ALL) {
			themeOptions.add(new ThemeOption(t));
		}
		if (search == null) {
			search = new SearchBoxWidget(Component.translatable("n4y.search"));
		}
	}

	@Override
	public void removed() {
		lastTab = tab;
		lastCategory = category;
	}

	// ------------------------------------------------------------------ state and automation

	public Tab tab() {
		return tab;
	}

	public ModuleCategory category() {
		return category;
	}

	public String query() {
		return search == null ? "" : search.text();
	}

	public List<TabButtonWidget> tabButtons() {
		return List.copyOf(tabs);
	}

	public List<SidebarItemWidget> categoryItems() {
		return List.copyOf(sidebar);
	}

	public List<N4yWidget> themeCards() {
		return List.copyOf(themeOptions);
	}

	public SearchBoxWidget searchBox() {
		return search;
	}

	public Optional<ModuleCardWidget> cardOf(Module module) {
		return Optional.ofNullable(cards.get(module));
	}

	/** The modules currently listed, after the category filter and the search query. */
	public List<Module> listedModules() {
		return visibleModules();
	}

	/**
	 * Converts a point of this screen's own (zoom-corrected) space into window pixels, which is
	 * what the cursor works in. Used by the in-game tests to drive real clicks.
	 */
	public double[] toWindowPixels(float virtualX, float virtualY) {
		double guiScale = minecraft.getWindow().getGuiScale();
		return new double[] {virtualX * ui * guiScale, (virtualY + lift) * ui * guiScale};
	}

	/** Window-pixel centre of {@code widget}, ready for {@code setCursorPos}. */
	public double[] centreOf(N4yWidget widget) {
		return toWindowPixels(widget.x() + widget.width() / 2f, widget.y() + widget.height() / 2f);
	}

	// ------------------------------------------------------------------ layout

	private List<Module> visibleModules() {
		return modules().filter(category, search == null ? "" : search.text());
	}

	/** Virtual (zoom-corrected) cursor position used for hit testing. */
	private float virtualX(double mouseX) {
		return (float) (mouseX / ui);
	}

	private float virtualY(double mouseY) {
		return (float) (mouseY / ui) - lift;
	}

	private void layout() {
		ui = Math.min(1f, Math.min((width - MARGIN * 2f) / PANEL_W, (height - MARGIN * 2f) / PANEL_H));
		panelX = (width / ui - PANEL_W) / 2f;
		panelY = (height / ui - PANEL_H) / 2f;

		float tabW = 32f;
		float tabH = 26f;
		float groupW = tabs.size() * tabW + (tabs.size() - 1) * 5f;
		float tx = panelX + PANEL_W / 2f - groupW / 2f;
		for (TabButtonWidget t : tabs) {
			t.bounds(tx, panelY + 13f, tabW, tabH);
			tx += tabW + 5f;
		}
		search.bounds(panelX + PANEL_W - PAD - 118f, panelY + 13f, 118f, 26f);

		float bodyY = panelY + HEADER_H;
		float bodyH = PANEL_H - HEADER_H - PAD;
		float itemH = 23f;
		float itemGap = 2.5f;
		float iy = bodyY + 8f;
		for (SidebarItemWidget item : sidebar) {
			item.bounds(panelX + PAD + 6f, iy, SIDEBAR_W - 12f, itemH);
			iy += itemH + itemGap;
		}

		float cx = panelX + PAD + SIDEBAR_W + GAP;
		float cw = PANEL_W - (cx - panelX) - PAD - SCROLLBAR_W;
		if (tab == Tab.MODULES) {
			List<Module> visible = visibleModules();
			float cardW = (cw - GAP) / 2f;
			int rows = (visible.size() + 1) / 2;
			maxScroll = Math.max(0f, rows * CARD_H + Math.max(0, rows - 1) * GAP - bodyH);
			scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll));
			float offset = scroll.value(System.nanoTime());
			for (int i = 0; i < visible.size(); i++) {
				ModuleCardWidget card = cards.get(visible.get(i));
				float col = i % 2;
				float row = i / 2;
				card.bounds(cx + col * (cardW + GAP), bodyY + row * (CARD_H + GAP) - offset, cardW, CARD_H);
			}
		} else {
			maxScroll = 0f;
			float ow = (PANEL_W - PAD * 2 - GAP) / 2f;
			float oh = 56f;
			for (int i = 0; i < themeOptions.size(); i++) {
				themeOptions.get(i).bounds(panelX + PAD + (i % 2) * (ow + GAP), bodyY + 6f + (i / 2) * (oh + GAP), ow, oh);
			}
		}
	}

	// ------------------------------------------------------------------ render

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null) {
			renderPanorama(g, delta);
		}
		renderBlurredBackground(g);
		GlassRenderer.backdrop(g, width, height, N4yClient.get().themes().current(), open.value());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		long now = System.nanoTime();
		scroll.animateTo(scrollTarget, now);
		layout();

		Theme t = N4yClient.get().themes().current(now);
		float a = open.value(now);
		lift = (1f - a) * 10f;
		UiContext ctx = new UiContext(g, t, now, a, virtualX(mouseX), virtualY(mouseY) + lift);

		UiScale.set(ui);
		g.pose().pushMatrix();
		g.pose().scale(ui, ui);
		g.pose().translate(0f, lift);

		GlassRenderer.glassPanel(g, panelX, panelY, PANEL_W, PANEL_H, PANEL_R, t, a);
		renderHeader(ctx);

		if (tab == Tab.MODULES) {
			renderSidebar(ctx);
			renderModules(ctx, virtualY(mouseY));
		} else {
			renderAppearance(ctx);
		}

		for (TabButtonWidget button : tabs) {
			button.render(ctx);
		}
		g.pose().popMatrix();
		UiScale.reset();

		for (TabButtonWidget button : tabs) {
			if (button.contains(virtualX(mouseX), virtualY(mouseY))) {
				drawTooltip(g, t, a, button.tooltip().getString(), mouseX, mouseY);
			}
		}
	}

	/** Glass tooltip in the n4y typography, instead of the vanilla pixel box. */
	private void drawTooltip(GuiGraphics g, Theme t, float a, String text, int mouseX, int mouseY) {
		float scale = 0.85f;
		float tw = N4yText.width(text, false, scale) + 14f;
		float th = 18f;
		float tx = Math.min(mouseX + 10f, width - tw - 4f);
		float ty = Math.max(4f, mouseY - th - 6f);
		GlassRenderer.shadow(g, tx, ty + 1f, tw, th, 6f, 8, Colors.fade(0x80000000, a));
		GlassRenderer.roundedRect(g, tx, ty, tw, th, 6f, Colors.fade(Colors.shade(t.panelTop(), 0.06f), a), Colors.fade(t.panelBottom(), a), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(g, tx, ty, tw, th, 6f, 0.5f, Colors.fade(t.borderTop(), a), Colors.fade(t.borderBottom(), a));
		N4yText.drawMiddle(g, text, tx + 7f, ty + th / 2f, scale, Colors.fade(t.textPrimary(), a), false);
	}

	private void renderHeader(UiContext ctx) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		GuiGraphics g = ctx.g();

		GlassRenderer.shadow(g, panelX + PAD, panelY + 13f, 24f, 24f, 8f, 8, Colors.fade(t.accentGlow(), 0.5f * a));
		GlassRenderer.roundedRect(g, panelX + PAD, panelY + 13f, 24f, 24f, 8f,
				Colors.fade(Colors.shade(t.accent(), 0.18f), a), Colors.fade(Colors.shade(t.accent(), -0.3f), a), GlassRenderer.CORNERS_ALL);
		IconTextures.draw(g, Icons.CUBE, panelX + PAD + 5f, panelY + 18f, 14f, Colors.fade(0xFFFFFFFF, a));

		N4yText.draw(g, N4y.NAME, panelX + PAD + 32f, panelY + 14f, 1.15f, Colors.fade(t.textPrimary(), a), true);
		int active = modules().enabled().size();
		String sub = "v" + N4y.version() + " · " + Component.translatable("n4y.header.active", active).getString();
		N4yText.draw(g, sub, panelX + PAD + 32f, panelY + 27.5f, 0.78f, Colors.fade(t.textMuted(), a), false);

		if (tab == Tab.MODULES) {
			search.render(ctx);
		}
	}

	private void renderSidebar(UiContext ctx) {
		float bodyY = panelY + HEADER_H;
		float bodyH = PANEL_H - HEADER_H - PAD;
		GlassRenderer.roundedRect(ctx.g(), panelX + PAD, bodyY, SIDEBAR_W, bodyH, 11f,
				Colors.fade(Colors.shade(ctx.theme().surface(), 0.03f), ctx.alpha() * 0.75f),
				Colors.fade(ctx.theme().surface(), ctx.alpha() * 0.55f), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), panelX + PAD, bodyY, SIDEBAR_W, bodyH, 11f, 0.5f,
				Colors.fade(ctx.theme().borderTop(), ctx.alpha() * 0.5f), Colors.fade(ctx.theme().borderBottom(), ctx.alpha()));
		for (SidebarItemWidget item : sidebar) {
			item.render(ctx);
		}
	}

	private void renderModules(UiContext ctx, float mouseY) {
		List<Module> visible = visibleModules();
		float bodyY = panelY + HEADER_H;
		float bodyH = PANEL_H - HEADER_H - PAD;
		float cx = panelX + PAD + SIDEBAR_W + GAP;
		float cw = PANEL_W - (cx - panelX) - PAD - SCROLLBAR_W;

		if (visible.isEmpty()) {
			Component message = search.isEmpty()
					? Component.translatable("n4y.empty.category")
					: Component.translatable("n4y.empty.search", search.text());
			N4yText.drawCentered(ctx.g(), message.getString(), cx + cw / 2f, bodyY + bodyH / 2f, 0.85f,
					Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
			return;
		}

		ctx.g().enableScissor(Math.round(cx), Math.round(bodyY), Math.round(cx + cw), Math.round(bodyY + bodyH));
		UiContext inner = new UiContext(ctx.g(), ctx.theme(), ctx.now(), ctx.alpha(), ctx.mouseX(), mouseY);
		for (Module module : visible) {
			ModuleCardWidget card = cards.get(module);
			if (card.y() + card.height() >= bodyY - 2 && card.y() <= bodyY + bodyH + 2) {
				card.render(inner);
			}
		}
		ctx.g().disableScissor();

		if (maxScroll > 0.5f) {
			// Fade the clipped rows into the glass instead of cutting them off sharply.
			int fadeTop = Colors.fade(ctx.theme().panelBottom(), ctx.alpha() * 0.9f);
			if (scroll.value(ctx.now()) > 1f) {
				ctx.g().fillGradient(Math.round(cx), Math.round(bodyY), Math.round(cx + cw), Math.round(bodyY + 10), fadeTop, Colors.fade(fadeTop, 0f));
			}
			if (scroll.value(ctx.now()) < maxScroll - 1f) {
				ctx.g().fillGradient(Math.round(cx), Math.round(bodyY + bodyH - 12), Math.round(cx + cw), Math.round(bodyY + bodyH), Colors.fade(fadeTop, 0f), fadeTop);
			}

			float trackX = cx + cw + 3f;
			float ratio = bodyH / (bodyH + maxScroll);
			float barH = Math.max(18f, bodyH * ratio);
			float barY = bodyY + (bodyH - barH) * (scroll.value(ctx.now()) / maxScroll);
			GlassRenderer.roundedRect(ctx.g(), trackX, bodyY, 2.5f, bodyH, 1.25f, Colors.fade(0x18FFFFFF, ctx.alpha()));
			GlassRenderer.roundedRect(ctx.g(), trackX, barY, 2.5f, barH, 1.25f, Colors.fade(ctx.theme().accent(), ctx.alpha() * 0.8f));
		}
	}

	private void renderAppearance(UiContext ctx) {
		N4yText.draw(ctx.g(), Component.translatable("n4y.appearance.hint").getString(),
				panelX + PAD, panelY + PANEL_H - PAD - 10f, 0.78f, Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		for (ThemeOption option : themeOptions) {
			option.render(ctx);
		}
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = virtualX(event.x());
		float my = virtualY(event.y());
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}

		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(mx, my)) {
				setTab(i == 0 ? Tab.MODULES : Tab.APPEARANCE);
				return true;
			}
		}
		if (tab == Tab.MODULES) {
			if (search.contains(mx, my)) {
				search.setFocused(true);
				return true;
			}
			search.setFocused(false);
			for (SidebarItemWidget item : sidebar) {
				if (item.contains(mx, my)) {
					if (category != item.category()) {
						category = item.category();
						scrollTarget = 0f;
						scroll.set(0f);
					}
					return true;
				}
			}
			float bodyY = panelY + HEADER_H;
			float bodyH = PANEL_H - HEADER_H - PAD;
			if (my >= bodyY && my <= bodyY + bodyH) {
				for (Module module : visibleModules()) {
					ModuleCardWidget card = cards.get(module);
					if (card.contains(mx, my)) {
						card.toggle(System.nanoTime());
						return true;
					}
				}
			}
		} else {
			for (ThemeOption option : themeOptions) {
				if (option.contains(mx, my)) {
					N4yClient.get().themes().select(option.theme());
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void setTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		search.setFocused(false);
		scrollTarget = 0f;
		scroll.set(0f);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == Tab.MODULES && maxScroll > 0f) {
			scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget - (float) scrollY * 28f));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (tab == Tab.MODULES && search.append(event.codepoint())) {
			scrollTarget = 0f;
			scroll.set(0f);
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (tab == Tab.MODULES && search.isFocused()) {
			switch (event.key()) {
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (search.backspace((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0)) {
						scrollTarget = 0f;
						scroll.set(0f);
					}
					return true;
				}
				case GLFW.GLFW_KEY_ESCAPE -> {
					if (!search.isEmpty()) {
						search.clear();
					} else {
						search.setFocused(false);
					}
					return true;
				}
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					search.setFocused(false);
					return true;
				}
				default -> {
					// fall through to the vanilla handling below
				}
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ------------------------------------------------------------------ theme card

	/** A selectable theme in the appearance tab. */
	private final class ThemeOption extends N4yWidget {
		private final Theme option;
		private final Animation selection = new Animation(0f, 220, Easing.OUT_CUBIC);

		ThemeOption(Theme option) {
			this.option = option;
			this.selection.set(isSelected() ? 1f : 0f);
		}

		Theme theme() {
			return option;
		}

		private boolean isSelected() {
			return N4yClient.get().themes().selected().id().equals(option.id());
		}

		@Override
		protected void draw(UiContext ctx, float hovered) {
			Theme t = ctx.theme();
			float a = ctx.alpha();
			selection.animateTo(isSelected() ? 1f : 0f, ctx.now());
			float sel = selection.value(ctx.now());

			GlassRenderer.glassCard(ctx.g(), x, y, width, height, 11f, t, hovered, sel, a);

			float r = 9f;
			GlassRenderer.shadow(ctx.g(), x + 14, y + height / 2f - r, r * 2, r * 2, r, 7, Colors.fade(option.accentGlow(), a * 0.8f));
			GlassRenderer.roundedRect(ctx.g(), x + 14, y + height / 2f - r, r * 2, r * 2, r,
					Colors.fade(Colors.shade(option.accent(), 0.2f), a), Colors.fade(option.accent(), a), GlassRenderer.CORNERS_ALL);

			N4yText.draw(ctx.g(), option.displayName().getString(), x + 42, y + height / 2f - 9f, 1.05f, Colors.fade(t.textPrimary(), a), true);
			String state = Component.translatable(sel > 0.5f ? "n4y.appearance.active" : "n4y.appearance.select").getString();
			N4yText.draw(ctx.g(), state, x + 42, y + height / 2f + 2f, 0.76f,
					Colors.fade(sel > 0.5f ? option.accent() : t.textMuted(), a), false);
		}
	}
}
