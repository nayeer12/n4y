package com.n4y.client.gui.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.N4y;
import com.n4y.client.N4yClient;
import com.n4y.client.config.ConfigManager;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.render.UiScale;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.theme.Themes;
import com.n4y.client.gui.widget.ButtonWidget;
import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.gui.widget.ProfileRowWidget;
import com.n4y.client.gui.widget.SidebarItemWidget;
import com.n4y.client.gui.widget.TabButtonWidget;
import com.n4y.client.gui.widget.TextFieldWidget;
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
 * The main n4y interface: a header with tabs and search, the module grid with its category
 * sidebar, the profile manager and the theme picker.
 *
 * <p>The screen owns layout and input; drawing is delegated to the widgets and the Glass
 * renderer, and every control maps to real state (a module, a profile or the theme engine).
 */
public final class N4yMainScreen extends Screen {
	/** The sections of the interface, selected by the header tabs. */
	public enum Tab {
		MODULES, HUD, PROFILES, APPEARANCE
	}

	private static final float PANEL_W = 480f;
	private static final float PANEL_H = 268f;
	private static final float PANEL_R = 16f;
	/** Margin kept between the panel and the window edge, in GUI units. */
	private static final float MARGIN = 14f;
	private static final float HEADER_H = 52f;
	private static final float PAD = 14f;
	private static final float SIDEBAR_W = 100f;
	private static final float CARD_H = 46f;
	private static final float ROW_H = 34f;
	private static final float GAP = 8f;
	private static final float SCROLLBAR_W = 7f;
	private static final float FIELD_H = 26f;

	/** Kept between openings so the interface comes back where the user left it. */
	private static Tab lastTab = Tab.MODULES;
	private static ModuleCategory lastCategory = ModuleCategory.ALL;

	private final Animation open = new Animation(0f, 300, Easing.OUT_CUBIC);
	private final Animation scroll = new Animation(0f, 180, Easing.OUT_CUBIC);
	private final Map<Module, ModuleCardWidget> cards = new LinkedHashMap<>();
	private final List<SidebarItemWidget> sidebar = new ArrayList<>();
	private final List<TabButtonWidget> tabs = new ArrayList<>();
	private final List<ThemeOption> themeOptions = new ArrayList<>();
	private final List<ProfileRowWidget> profileRows = new ArrayList<>();
	private TextFieldWidget search;
	private TextFieldWidget profileName;
	private ButtonWidget createProfile;
	private ButtonWidget openStudio;

	private Tab tab = lastTab;
	private ModuleCategory category = lastCategory;
	private Module bindingModule;
	private float scrollTarget;
	private float maxScroll;
	private float panelX;
	private float panelY;
	/** Uniform zoom that keeps the whole panel on screen, and the open-animation lift. */
	private float ui = 1f;
	private float lift;
	private List<Module> visibleCache;
	private String visibleQuery = "";
	private ModuleCategory visibleCategory;

	public N4yMainScreen() {
		super(Component.literal(N4y.NAME));
	}

	private ModuleManager modules() {
		return N4yClient.get().modules();
	}

	private ConfigManager config() {
		return N4yClient.get().config();
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
			cards.put(module, new ModuleCardWidget(module, () -> bindingModule == module));
		}
		for (ModuleCategory c : ModuleCategory.values()) {
			sidebar.add(new SidebarItemWidget(c, () -> category == c));
		}
		tabs.add(new TabButtonWidget(Icons.GRID, Component.translatable("n4y.tab.modules"), () -> tab == Tab.MODULES));
		tabs.add(new TabButtonWidget(Icons.LAYOUT, Component.translatable("n4y.tab.hud"), () -> tab == Tab.HUD));
		tabs.add(new TabButtonWidget(Icons.FOLDER, Component.translatable("n4y.tab.profiles"), () -> tab == Tab.PROFILES));
		tabs.add(new TabButtonWidget(Icons.PALETTE, Component.translatable("n4y.tab.appearance"), () -> tab == Tab.APPEARANCE));
		for (Theme t : Themes.ALL) {
			themeOptions.add(new ThemeOption(t));
		}
		if (search == null) {
			search = TextFieldWidget.search(Component.translatable("n4y.search"));
		}
		if (openStudio == null) {
			openStudio = new ButtonWidget(Component.translatable("n4y.hud.studio.open"), true, () -> true);
		}
		if (profileName == null) {
			profileName = new TextFieldWidget(null, Component.translatable("n4y.profiles.name"), 24);
			createProfile = new ButtonWidget(Component.translatable("n4y.profiles.create"), true,
					() -> com.n4y.client.config.Profile.isValidName(profileName.text()) && !config().exists(profileName.text()));
		}
		refreshProfiles();
	}

	private void refreshProfiles() {
		profileRows.clear();
		for (String name : config().profileNames()) {
			profileRows.add(new ProfileRowWidget(name, () -> config().activeProfileName().equalsIgnoreCase(name)));
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

	public List<ProfileRowWidget> profileRowWidgets() {
		return List.copyOf(profileRows);
	}

	public TextFieldWidget searchBox() {
		return search;
	}

	public TextFieldWidget profileNameBox() {
		return profileName;
	}

	public ButtonWidget createProfileButton() {
		return createProfile;
	}

	/** The module waiting for a key press, if the user is binding one. */
	public Optional<Module> bindingModule() {
		return Optional.ofNullable(bindingModule);
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

	/** Virtual (zoom-corrected) cursor position used for hit testing. */
	private float virtualX(double mouseX) {
		return (float) (mouseX / ui);
	}

	private float virtualY(double mouseY) {
		return (float) (mouseY / ui) - lift;
	}

	/** Filtering allocates and compares strings, so it runs once per frame, not once per use. */
	private List<Module> visibleModules() {
		String query = search == null ? "" : search.text();
		if (visibleCache == null || !query.equals(visibleQuery) || category != visibleCategory) {
			visibleQuery = query;
			visibleCategory = category;
			visibleCache = modules().filter(category, query);
		}
		return visibleCache;
	}

	private float bodyY() {
		return panelY + HEADER_H;
	}

	private float bodyH() {
		return PANEL_H - HEADER_H - PAD;
	}

	private float contentX() {
		return panelX + PAD + SIDEBAR_W + GAP;
	}

	private float contentW() {
		return PANEL_W - (contentX() - panelX) - PAD - SCROLLBAR_W;
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
		search.bounds(panelX + PANEL_W - PAD - 118f, panelY + 13f, 118f, FIELD_H);

		float itemGap = 2.5f;
		float itemH = Math.min(23f, (bodyH() - 16f - itemGap * (sidebar.size() - 1)) / sidebar.size());
		float iy = bodyY() + 8f;
		for (SidebarItemWidget item : sidebar) {
			item.bounds(panelX + PAD + 6f, iy, SIDEBAR_W - 12f, itemH);
			iy += itemH + itemGap;
		}

		float offset = scroll.value(System.nanoTime());
		switch (tab) {
			case MODULES -> {
				List<Module> visible = visibleModules();
				float cardW = (contentW() - GAP) / 2f;
				int rows = (visible.size() + 1) / 2;
				maxScroll = Math.max(0f, rows * CARD_H + Math.max(0, rows - 1) * GAP - bodyH());
				scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll));
				for (int i = 0; i < visible.size(); i++) {
					ModuleCardWidget card = cards.get(visible.get(i));
					card.bounds(contentX() + (i % 2) * (cardW + GAP), bodyY() + (i / 2) * (CARD_H + GAP) - offset, cardW, CARD_H);
				}
			}
			case HUD -> {
				List<HudModule> elements = N4yClient.get().hud().elements();
				float listW = PANEL_W - PAD * 2 - SCROLLBAR_W;
				float cardW = (listW - GAP) / 2f;
				int rows = (elements.size() + 1) / 2;
				float listH = bodyH() - FIELD_H - 10f;
				maxScroll = Math.max(0f, rows * CARD_H + Math.max(0, rows - 1) * GAP - listH);
				scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll));
				for (int i = 0; i < elements.size(); i++) {
					ModuleCardWidget card = cards.get(elements.get(i));
					if (card != null) {
						card.bounds(panelX + PAD + (i % 2) * (cardW + GAP), bodyY() + (i / 2) * (CARD_H + GAP) - offset, cardW, CARD_H);
					}
				}
				openStudio.bounds(panelX + PANEL_W - PAD - 116f, bodyY() + bodyH() - FIELD_H, 116f, FIELD_H);
			}
			case PROFILES -> {
				float listW = PANEL_W - PAD * 2 - SCROLLBAR_W;
				float listH = profileListHeight();
				maxScroll = Math.max(0f, profileRows.size() * ROW_H + Math.max(0, profileRows.size() - 1) * 6f - listH);
				scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll));
				for (int i = 0; i < profileRows.size(); i++) {
					profileRows.get(i).bounds(panelX + PAD, bodyY() + i * (ROW_H + 6f) - offset, listW, ROW_H);
				}
				float barY = bodyY() + bodyH() - FIELD_H;
				float buttonW = 78f;
				profileName.bounds(panelX + PAD, barY, PANEL_W - PAD * 2 - buttonW - GAP, FIELD_H);
				createProfile.bounds(panelX + PANEL_W - PAD - buttonW, barY, buttonW, FIELD_H);
			}
			case APPEARANCE -> {
				maxScroll = 0f;
				float ow = (PANEL_W - PAD * 2 - GAP) / 2f;
				float oh = 56f;
				for (int i = 0; i < themeOptions.size(); i++) {
					themeOptions.get(i).bounds(panelX + PAD + (i % 2) * (ow + GAP), bodyY() + 6f + (i / 2) * (oh + GAP), ow, oh);
				}
			}
			default -> {
			}
		}
	}

	/** Height of the scrollable profile list, leaving room for the creation bar below it. */
	private float profileListHeight() {
		return bodyH() - FIELD_H - 22f;
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

		switch (tab) {
			case MODULES -> {
				renderSidebar(ctx);
				renderModules(ctx, virtualY(mouseY));
			}
			case HUD -> renderHud(ctx, virtualY(mouseY));
			case PROFILES -> renderProfiles(ctx, virtualY(mouseY));
			case APPEARANCE -> renderAppearance(ctx);
			default -> {
			}
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
		N4yClient.get().notifications().render(g, t, width, now);
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
		String sub = "v" + N4y.version() + " · " + config().activeProfileName() + " · "
				+ Component.translatable("n4y.header.active", modules().enabled().size()).getString();
		N4yText.draw(g, N4yText.ellipsize(sub, false, 0.78f, 150f), panelX + PAD + 32f, panelY + 27.5f, 0.78f, Colors.fade(t.textMuted(), a), false);

		if (tab == Tab.MODULES) {
			search.render(ctx);
		}
	}

	private void renderSidebar(UiContext ctx) {
		GlassRenderer.roundedRect(ctx.g(), panelX + PAD, bodyY(), SIDEBAR_W, bodyH(), 11f,
				Colors.fade(Colors.shade(ctx.theme().surface(), 0.03f), ctx.alpha() * 0.75f),
				Colors.fade(ctx.theme().surface(), ctx.alpha() * 0.55f), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), panelX + PAD, bodyY(), SIDEBAR_W, bodyH(), 11f, 0.5f,
				Colors.fade(ctx.theme().borderTop(), ctx.alpha() * 0.5f), Colors.fade(ctx.theme().borderBottom(), ctx.alpha()));
		for (SidebarItemWidget item : sidebar) {
			item.render(ctx);
		}
	}

	private void renderModules(UiContext ctx, float mouseY) {
		List<Module> visible = visibleModules();
		float cx = contentX();
		float cw = contentW();

		if (visible.isEmpty()) {
			Component message = search.isEmpty()
					? Component.translatable("n4y.empty.category")
					: Component.translatable("n4y.empty.search", search.text());
			N4yText.drawCentered(ctx.g(), message.getString(), cx + cw / 2f, bodyY() + bodyH() / 2f, 0.85f,
					Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
			return;
		}

		UiContext inner = new UiContext(ctx.g(), ctx.theme(), ctx.now(), ctx.alpha(), ctx.mouseX(), mouseY);
		ctx.g().enableScissor(Math.round(cx), Math.round(bodyY()), Math.round(cx + cw), Math.round(bodyY() + bodyH()));
		for (Module module : visible) {
			ModuleCardWidget card = cards.get(module);
			if (card.y() + card.height() >= bodyY() - 2 && card.y() <= bodyY() + bodyH() + 2) {
				card.render(inner);
			}
		}
		ctx.g().disableScissor();
		renderScrollHints(ctx, cx, bodyY(), cw, bodyH());
	}

	private void renderHud(UiContext ctx, float mouseY) {
		List<HudModule> elements = N4yClient.get().hud().elements();
		float listX = panelX + PAD;
		float listW = PANEL_W - PAD * 2 - SCROLLBAR_W;
		float listH = bodyH() - FIELD_H - 10f;

		UiContext inner = new UiContext(ctx.g(), ctx.theme(), ctx.now(), ctx.alpha(), ctx.mouseX(), mouseY);
		ctx.g().enableScissor(Math.round(listX), Math.round(bodyY()), Math.round(listX + listW), Math.round(bodyY() + listH));
		for (HudModule element : elements) {
			ModuleCardWidget card = cards.get(element);
			if (card != null && card.y() + card.height() >= bodyY() - 2 && card.y() <= bodyY() + listH + 2) {
				card.render(inner);
			}
		}
		ctx.g().disableScissor();
		renderScrollHints(ctx, listX, bodyY(), listW, listH);

		N4yText.drawMiddle(ctx.g(), Component.translatable("n4y.hud.hint").getString(),
				listX, bodyY() + bodyH() - FIELD_H / 2f, 0.78f, Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		openStudio.render(ctx);
	}

	private void renderProfiles(UiContext ctx, float mouseY) {
		float listX = panelX + PAD;
		float listW = PANEL_W - PAD * 2 - SCROLLBAR_W;
		float listH = profileListHeight();

		UiContext inner = new UiContext(ctx.g(), ctx.theme(), ctx.now(), ctx.alpha(), ctx.mouseX(), mouseY);
		ctx.g().enableScissor(Math.round(listX), Math.round(bodyY()), Math.round(listX + listW), Math.round(bodyY() + listH));
		for (ProfileRowWidget row : profileRows) {
			if (row.y() + row.height() >= bodyY() - 2 && row.y() <= bodyY() + listH + 2) {
				row.render(inner);
			}
		}
		ctx.g().disableScissor();
		renderScrollHints(ctx, listX, bodyY(), listW, listH);

		N4yText.draw(ctx.g(), Component.translatable("n4y.keybind.hint").getString(),
				listX, bodyY() + listH + 6f, 0.76f, Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		profileName.render(ctx);
		createProfile.render(ctx);
	}

	private void renderAppearance(UiContext ctx) {
		N4yText.draw(ctx.g(), Component.translatable("n4y.appearance.hint").getString(),
				panelX + PAD, panelY + PANEL_H - PAD - 10f, 0.78f, Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		for (ThemeOption option : themeOptions) {
			option.render(ctx);
		}
	}

	/** Fades the clipped rows into the glass and draws the scrollbar. */
	private void renderScrollHints(UiContext ctx, float cx, float cy, float cw, float ch) {
		if (maxScroll <= 0.5f) {
			return;
		}
		float offset = scroll.value(ctx.now());
		int edge = Colors.fade(ctx.theme().panelBottom(), ctx.alpha() * 0.9f);
		if (offset > 1f) {
			ctx.g().fillGradient(Math.round(cx), Math.round(cy), Math.round(cx + cw), Math.round(cy + 10), edge, Colors.fade(edge, 0f));
		}
		if (offset < maxScroll - 1f) {
			ctx.g().fillGradient(Math.round(cx), Math.round(cy + ch - 12), Math.round(cx + cw), Math.round(cy + ch), Colors.fade(edge, 0f), edge);
		}
		float trackX = cx + cw + 3f;
		float barH = Math.max(18f, ch * (ch / (ch + maxScroll)));
		float barY = cy + (ch - barH) * (offset / maxScroll);
		GlassRenderer.roundedRect(ctx.g(), trackX, cy, 2.5f, ch, 1.25f, Colors.fade(0x18FFFFFF, ctx.alpha()));
		GlassRenderer.roundedRect(ctx.g(), trackX, barY, 2.5f, barH, 1.25f, Colors.fade(ctx.theme().accent(), ctx.alpha() * 0.8f));
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = virtualX(event.x());
		float my = virtualY(event.y());
		boolean right = event.button() == 1;
		if (event.button() != 0 && !right) {
			return super.mouseClicked(event, doubleClick);
		}

		if (bindingModule != null) {
			bindingModule = null;
			if (!right) {
				return true;
			}
		}

		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(mx, my)) {
				setTab(Tab.values()[i]);
				return true;
			}
		}

		return switch (tab) {
			case MODULES -> clickModules(mx, my, right);
			case HUD -> clickHud(mx, my, right);
			case PROFILES -> clickProfiles(mx, my);
			case APPEARANCE -> clickAppearance(mx, my);
		};
	}

	private boolean clickModules(float mx, float my, boolean right) {
		if (search.contains(mx, my)) {
			search.setFocused(!right);
			return true;
		}
		search.setFocused(false);
		for (SidebarItemWidget item : sidebar) {
			if (item.contains(mx, my)) {
				if (category != item.category()) {
					category = item.category();
					resetScroll();
				}
				return true;
			}
		}
		if (my >= bodyY() && my <= bodyY() + bodyH()) {
			for (Module module : visibleModules()) {
				ModuleCardWidget card = cards.get(module);
				if (!card.contains(mx, my)) {
					continue;
				}
				if (right || card.chipContains(mx, my)) {
					bindingModule = module;
				} else {
					card.toggle(System.nanoTime());
				}
				return true;
			}
		}
		return false;
	}

	private boolean clickHud(float mx, float my, boolean right) {
		if (openStudio.contains(mx, my)) {
			minecraft.setScreen(new HudStudioScreen(this));
			return true;
		}
		float listH = bodyH() - FIELD_H - 10f;
		if (my >= bodyY() && my <= bodyY() + listH) {
			for (HudModule element : N4yClient.get().hud().elements()) {
				ModuleCardWidget card = cards.get(element);
				if (card == null || !card.contains(mx, my)) {
					continue;
				}
				if (right || card.chipContains(mx, my)) {
					bindingModule = element;
				} else {
					card.toggle(System.nanoTime());
				}
				return true;
			}
		}
		return false;
	}

	private boolean clickProfiles(float mx, float my) {
		if (profileName.contains(mx, my)) {
			profileName.setFocused(true);
			return true;
		}
		profileName.setFocused(false);
		if (createProfile.contains(mx, my)) {
			if (createProfile.isUsable() && config().createProfile(profileName.text())) {
				profileName.clear();
				refreshProfiles();
				resetScroll();
			}
			return true;
		}
		float listBottom = bodyY() + profileListHeight();
		if (my >= bodyY() && my <= listBottom) {
			for (ProfileRowWidget row : profileRows) {
				if (!row.contains(mx, my)) {
					continue;
				}
				if (row.deleteContains(mx, my)) {
					if (config().deleteProfile(row.profileName())) {
						refreshProfiles();
					}
				} else if (!row.isActive() && config().switchProfile(row.profileName())) {
					refreshProfiles();
					syncCards();
					N4yClient.get().notifications().push(Icons.FOLDER,
							Component.translatable("n4y.notify.profile"), Component.literal(row.profileName()), true);
				}
				return true;
			}
		}
		return false;
	}

	private boolean clickAppearance(float mx, float my) {
		for (ThemeOption option : themeOptions) {
			if (option.contains(mx, my)) {
				N4yClient.get().themes().select(option.theme());
				return true;
			}
		}
		return false;
	}

	/** Brings every switch in line with the module states after a profile is loaded. */
	private void syncCards() {
		long now = System.nanoTime();
		for (ModuleCardWidget card : cards.values()) {
			card.syncTo(now);
		}
	}

	private void setTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		bindingModule = null;
		search.setFocused(false);
		profileName.setFocused(false);
		if (next == Tab.PROFILES) {
			refreshProfiles();
		}
		resetScroll();
	}

	private void resetScroll() {
		scrollTarget = 0f;
		scroll.set(0f);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll > 0f) {
			scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget - (float) scrollY * 28f));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		// Quick search: typing anywhere in the module list starts filtering.
		if (tab == Tab.MODULES && focusedField() == null && bindingModule == null
				&& !Character.isISOControl(event.codepoint())) {
			search.setFocused(true);
		}
		TextFieldWidget field = focusedField();
		if (field != null && field.append(event.codepoint())) {
			if (field == search) {
				resetScroll();
			}
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (bindingModule != null) {
			bindKey(event.key());
			return true;
		}
		TextFieldWidget field = focusedField();
		if (field != null) {
			switch (event.key()) {
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (field.backspace((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) && field == search) {
						resetScroll();
					}
					return true;
				}
				case GLFW.GLFW_KEY_ESCAPE -> {
					if (!field.isEmpty()) {
						field.clear();
					} else {
						field.setFocused(false);
					}
					return true;
				}
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					if (field == profileName && createProfile.isUsable() && config().createProfile(profileName.text())) {
						profileName.clear();
						refreshProfiles();
					}
					field.setFocused(false);
					return true;
				}
				default -> {
					// fall through to the vanilla handling below
				}
			}
		}
		return super.keyPressed(event);
	}

	/** Escape clears the bind, any other key becomes the module's shortcut. */
	private void bindKey(int key) {
		Module module = bindingModule;
		bindingModule = null;
		if (module == null) {
			return;
		}
		module.setKey(key == GLFW.GLFW_KEY_ESCAPE ? Module.UNBOUND : key);
	}

	private TextFieldWidget focusedField() {
		if (tab == Tab.MODULES && search.isFocused()) {
			return search;
		}
		if (tab == Tab.PROFILES && profileName.isFocused()) {
			return profileName;
		}
		return null;
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
