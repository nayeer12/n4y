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
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.render.UiScale;
import com.n4y.client.gui.screen.panel.AppearanceView;
import com.n4y.client.gui.screen.panel.HudView;
import com.n4y.client.gui.screen.panel.ModuleDetailView;
import com.n4y.client.gui.screen.panel.ModulesView;
import com.n4y.client.gui.screen.panel.PanelGeometry;
import com.n4y.client.gui.screen.panel.PanelHost;
import com.n4y.client.gui.screen.panel.PanelView;
import com.n4y.client.gui.screen.panel.ProfilesView;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.widget.ButtonWidget;
import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.gui.widget.ProfileRowWidget;
import com.n4y.client.gui.widget.SettingRowWidget;
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
 * The main n4y interface: a header with tabs and search, and whichever section is showing.
 *
 * <p>The screen owns what spans the whole panel — the glass frame, the header, the tabs, the
 * open animation, where the cursor is and which section is visible — and nothing else. Each
 * section is a {@link PanelView} that holds its own widgets, its own scroll position and its own
 * layout, drawing and click handling. Before that, those three things lived in three separate
 * {@code switch} statements here, so changing one tab meant finding its pieces among all of them.
 *
 * <p>Binding a key to a module stays here, because it is completed by the next keystroke anywhere
 * and can be started from a card in two different sections.
 */
public final class N4yMainScreen extends Screen implements PanelHost {
	/** The sections of the interface, selected by the header tabs. */
	public enum Tab {
		MODULES, HUD, PROFILES, APPEARANCE
	}

	private static final Component PROFILE_SWITCHED = Component.translatable("n4y.notify.profile");
	private static final float TAB_W = 32f;
	private static final float TAB_H = 26f;
	private static final float TAB_GAP = 5f;
	private static final float LOGO = 24f;
	/** How far the panel slides up as it opens, in GUI units. */
	private static final float OPEN_LIFT = 10f;

	/** Kept between openings so the interface comes back where the user left it. */
	private static Tab lastTab = Tab.MODULES;
	private static ModuleCategory lastCategory = ModuleCategory.ALL;

	private final Animation open = new Animation(0f, 300, Easing.OUT_CUBIC);
	private final PanelGeometry geom = new PanelGeometry();
	private final Map<Module, ModuleCardWidget> cards = new LinkedHashMap<>();
	private final List<TabButtonWidget> tabs = new ArrayList<>();

	private final ModulesView modulesView = new ModulesView(this, lastCategory);
	private final HudView hudView = new HudView(this);
	private final ProfilesView profilesView = new ProfilesView(this);
	private final AppearanceView appearanceView = new AppearanceView();
	private final ModuleDetailView detailView = new ModuleDetailView(this);

	private Tab tab = lastTab;
	private Module bindingModule;
	/** Offset of the whole panel while it slides in. */
	private float lift;
	private String headerSubtitle;
	private String headerProfile;
	private int headerEnabled = -1;

	public N4yMainScreen() {
		super(Component.literal(N4y.NAME));
	}

	private static ModuleManager modules() {
		return N4yClient.get().modules();
	}

	private static ConfigManager config() {
		return N4yClient.get().config();
	}

	/** The section showing now: a module's settings take the place of the selected tab. */
	private PanelView currentView() {
		if (detailView.isOpen()) {
			return detailView;
		}
		return switch (tab) {
			case MODULES -> modulesView;
			case HUD -> hudView;
			case PROFILES -> profilesView;
			case APPEARANCE -> appearanceView;
		};
	}

	// ------------------------------------------------------------------ setup

	/**
	 * Called when the screen opens and again on every window resize. Only the layout depends on
	 * the window, so the widgets are built once: rebuilding them threw away hover and toggle
	 * animations every time the window changed size.
	 */
	@Override
	protected void init() {
		open.animateTo(1f);
		buildCards();
		if (tabs.isEmpty()) {
			tabs.add(new TabButtonWidget(Icons.GRID, Component.translatable("n4y.tab.modules"), () -> tab == Tab.MODULES));
			tabs.add(new TabButtonWidget(Icons.LAYOUT, Component.translatable("n4y.tab.hud"), () -> tab == Tab.HUD));
			tabs.add(new TabButtonWidget(Icons.FOLDER, Component.translatable("n4y.tab.profiles"), () -> tab == Tab.PROFILES));
			tabs.add(new TabButtonWidget(Icons.PALETTE, Component.translatable("n4y.tab.appearance"), () -> tab == Tab.APPEARANCE));
		}
	}

	/** One card per module, rebuilt only when the set of modules has actually changed. */
	private void buildCards() {
		List<Module> all = modules().all();
		if (cards.size() == all.size() && cards.keySet().containsAll(all)) {
			return;
		}
		cards.clear();
		for (int i = 0; i < all.size(); i++) {
			Module module = all.get(i);
			cards.put(module, new ModuleCardWidget(module, () -> bindingModule == module));
		}
	}

	@Override
	public void removed() {
		lastTab = tab;
		lastCategory = modulesView.category();
	}

	// ------------------------------------------------------------------ what the sections ask of us

	@Override
	public ModuleCardWidget card(Module module) {
		return cards.get(module);
	}

	@Override
	public void startBinding(Module module) {
		bindingModule = module;
	}

	@Override
	public boolean isBinding(Module module) {
		return bindingModule == module;
	}

	@Override
	public void openHudStudio() {
		minecraft.setScreen(new HudStudioScreen(this));
	}

	@Override
	public void onProfileSwitched(String profileName) {
		syncCards();
		N4yClient.get().notifications().push(Icons.FOLDER, PROFILE_SWITCHED,
				Component.literal(profileName), true);
	}

	/** Brings every switch in line with the module states after a profile is loaded. */
	private void syncCards() {
		long now = System.nanoTime();
		for (ModuleCardWidget card : cards.values()) {
			card.syncTo(now);
		}
	}

	// ------------------------------------------------------------------ state, for the interface tests

	public Tab tab() {
		return tab;
	}

	public ModuleCategory category() {
		return modulesView.category();
	}

	public String query() {
		return modulesView.query();
	}

	public List<TabButtonWidget> tabButtons() {
		return List.copyOf(tabs);
	}

	public List<SidebarItemWidget> categoryItems() {
		return modulesView.sidebarItems();
	}

	public List<N4yWidget> themeCards() {
		return appearanceView.cards();
	}

	public List<ProfileRowWidget> profileRowWidgets() {
		return profilesView.rows();
	}

	public TextFieldWidget searchBox() {
		return modulesView.search();
	}

	public TextFieldWidget profileNameBox() {
		return profilesView.nameBox();
	}

	public ButtonWidget createProfileButton() {
		return profilesView.createButton();
	}

	/** The module waiting for a key press, if the user is binding one. */
	public Optional<Module> bindingModule() {
		return Optional.ofNullable(bindingModule);
	}

	/** The module whose settings panel is open, if any. */
	public Optional<Module> detailModule() {
		return detailView.module();
	}

	public List<SettingRowWidget> settingRows() {
		return detailView.rows();
	}

	public Optional<ModuleCardWidget> cardOf(Module module) {
		return Optional.ofNullable(cards.get(module));
	}

	/** The modules currently listed, after the category filter and the search query. */
	public List<Module> listedModules() {
		return modulesView.listedModules();
	}

	/** Opens the settings panel of {@code module}; used by the interface and the tests. */
	@Override
	public void openDetail(Module module) {
		bindingModule = null;
		detailView.open(module);
	}

	@Override
	public void closeDetail() {
		detailView.close();
	}

	/**
	 * Converts a point of this screen's own (zoom-corrected) space into window pixels, which is
	 * what the cursor works in. Used by the in-game tests to drive real clicks.
	 */
	public double[] toWindowPixels(float virtualX, float virtualY) {
		double guiScale = minecraft.getWindow().getGuiScale();
		return new double[] {virtualX * geom.scale() * guiScale, (virtualY + lift) * geom.scale() * guiScale};
	}

	/** Window-pixel centre of {@code widget}, ready for {@code setCursorPos}. */
	public double[] centreOf(N4yWidget widget) {
		return toWindowPixels(widget.x() + widget.width() / 2f, widget.y() + widget.height() / 2f);
	}

	// ------------------------------------------------------------------ layout

	/** Cursor position in panel space, which is what every widget's bounds are in. */
	private float virtualX(double mouseX) {
		return (float) (mouseX / geom.scale());
	}

	private float virtualY(double mouseY) {
		return (float) (mouseY / geom.scale()) - lift;
	}

	private void layoutTabs() {
		float groupW = tabs.size() * TAB_W + (tabs.size() - 1) * TAB_GAP;
		float x = geom.x() + PanelGeometry.WIDTH / 2f - groupW / 2f;
		for (int i = 0; i < tabs.size(); i++) {
			tabs.get(i).bounds(x, geom.y() + 13f, TAB_W, TAB_H);
			x += TAB_W + TAB_GAP;
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
		Theme t = N4yClient.get().themes().current(now);
		float a = open.value(now);
		lift = (1f - a) * OPEN_LIFT;

		geom.centreIn(width, height);
		PanelView view = currentView();
		view.scroll().advance(now);
		layoutTabs();
		modulesView.layoutSearch(geom);
		view.layout(geom, view.scroll().offset(now));

		// One context: the cursor in panel space, the same space clicks are tested in. There
		// used to be two, differing by the open animation's offset, so a widget could highlight
		// in one place while a click on it landed in another.
		UiContext ctx = new UiContext(g, t, now, a, virtualX(mouseX), virtualY(mouseY));

		UiScale.set(geom.scale());
		g.pose().pushMatrix();
		g.pose().scale(geom.scale(), geom.scale());
		g.pose().translate(0f, lift);

		GlassRenderer.glassPanel(g, geom.x(), geom.y(), PanelGeometry.WIDTH, PanelGeometry.HEIGHT,
				PanelGeometry.RADIUS, t, a);
		renderHeader(ctx);
		view.render(ctx, geom);
		for (int i = 0; i < tabs.size(); i++) {
			tabs.get(i).render(ctx);
		}
		g.pose().popMatrix();
		UiScale.reset();

		for (int i = 0; i < tabs.size(); i++) {
			TabButtonWidget button = tabs.get(i);
			if (button.contains(ctx.mouseX(), ctx.mouseY())) {
				drawTooltip(g, t, a, N4yText.resolve(button.tooltip()), mouseX, mouseY);
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
		GlassRenderer.roundedRect(g, tx, ty, tw, th, 6f, Colors.fade(Colors.shade(t.panelTop(), 0.06f), a),
				Colors.fade(t.panelBottom(), a), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(g, tx, ty, tw, th, 6f, 0.5f, Colors.fade(t.borderTop(), a), Colors.fade(t.borderBottom(), a));
		N4yText.drawMiddle(g, text, tx + 7f, ty + th / 2f, scale, Colors.fade(t.textPrimary(), a), false);
	}

	private void renderHeader(UiContext ctx) {
		Theme t = ctx.theme();
		float a = ctx.alpha();
		GuiGraphics g = ctx.g();
		float x = geom.x() + PanelGeometry.PAD;
		float y = geom.y() + 13f;

		GlassRenderer.shadow(g, x, y, LOGO, LOGO, 8f, 8, Colors.fade(t.accentGlow(), 0.5f * a));
		GlassRenderer.roundedRect(g, x, y, LOGO, LOGO, 8f,
				Colors.fade(Colors.shade(t.accent(), 0.18f), a), Colors.fade(Colors.shade(t.accent(), -0.3f), a),
				GlassRenderer.CORNERS_ALL);
		IconTextures.draw(g, Icons.CUBE, x + 5f, y + 5f, 14f, Colors.fade(0xFFFFFFFF, a));

		N4yText.draw(g, N4y.NAME, x + 32f, geom.y() + 14f, 1.15f, Colors.fade(t.textPrimary(), a), true);
		N4yText.draw(g, headerSubtitle(), x + 32f, geom.y() + 27.5f, 0.78f, Colors.fade(t.textMuted(), a), false);

		if (tab == Tab.MODULES && !detailView.isOpen()) {
			modulesView.search().render(ctx);
		}
	}

	/**
	 * The version, profile and active-module line, rebuilt only when it would read differently.
	 * It was being concatenated, translated and trimmed on every frame the panel was open.
	 */
	private String headerSubtitle() {
		String profile = config().activeProfileName();
		int active = modules().enabledCount();
		if (headerSubtitle == null || active != headerEnabled || !profile.equals(headerProfile)) {
			headerProfile = profile;
			headerEnabled = active;
			headerSubtitle = N4yText.ellipsize("v" + N4y.version() + " · " + profile + " · "
					+ Component.translatable("n4y.header.active", active).getString(), false, 0.78f, 150f);
		}
		return headerSubtitle;
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

		// A binding in flight takes the click before anything else can act on it.
		if (detailView.consumeBindingClick(mx, my, event.button(), right)) {
			return true;
		}
		if (bindingModule != null) {
			ModuleCardWidget card = cards.get(bindingModule);
			boolean overChip = detailView.module().orElse(null) == bindingModule && detailView.overKeyChip(mx, my);
			if (overChip || card != null && card.contains(mx, my)) {
				bindKey(InputBinding.mouse(event.button()));
				return true;
			}
			bindingModule = null;
			if (!right) {
				return true;
			}
		}

		if (!detailView.isOpen()) {
			for (int i = 0; i < tabs.size(); i++) {
				if (tabs.get(i).contains(mx, my)) {
					setTab(Tab.values()[i]);
					return true;
				}
			}
		}
		return currentView().click(mx, my, right);
	}

	private void setTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		closeDetail();
		bindingModule = null;
		modulesView.search().setFocused(false);
		profilesView.nameBox().setFocused(false);
		currentView().onShown();
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (detailView.dragTo(virtualX(event.x()))) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (detailView.releaseDrag()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (currentView().scroll().scrollBy(-(float) scrollY * 28f)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		// Quick search: typing anywhere in the module list starts filtering.
		if (currentView() == modulesView && focusedField() == null && bindingModule == null
				&& !Character.isISOControl(event.codepoint())) {
			modulesView.search().setFocused(true);
		}
		TextFieldWidget field = focusedField();
		if (field != null && field.append(event.codepoint())) {
			if (field == modulesView.search()) {
				modulesView.onQueryChanged();
			}
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (detailView.consumeBindingKey(event.key())) {
			return true;
		}
		if (bindingModule != null) {
			bindKey(event.key() == GLFW.GLFW_KEY_ESCAPE ? InputBinding.UNBOUND : InputBinding.key(event.key()));
			return true;
		}
		if (detailView.isOpen() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
			closeDetail();
			return true;
		}
		TextFieldWidget field = focusedField();
		if (field != null) {
			switch (event.key()) {
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (field.backspace((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0)
							&& field == modulesView.search()) {
						modulesView.onQueryChanged();
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
					if (field == profilesView.nameBox()) {
						profilesView.createFromNameBox();
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

	/** Escape clears the bind, any other key or mouse button becomes the module's shortcut. */
	private void bindKey(InputBinding binding) {
		Module module = bindingModule;
		bindingModule = null;
		if (module != null) {
			module.setKey(binding);
		}
	}

	/** The field taking keystrokes, which only the visible section can have. */
	private TextFieldWidget focusedField() {
		return currentView().focusedField().orElse(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
