package com.n4y.client.gui.screen.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.gui.widget.SidebarItemWidget;
import com.n4y.client.gui.widget.TextFieldWidget;
import com.n4y.client.gui.widget.UiContext;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;

import net.minecraft.network.chat.Component;

/** The module grid with its category sidebar and the search box in the header. */
public final class ModulesView implements PanelView {
	private static final Component EMPTY_CATEGORY = Component.translatable("n4y.empty.category");
	private static final float SIDEBAR_INSET = 6f;
	private static final float ITEM_GAP = 2.5f;
	private static final float ITEM_MAX_H = 23f;

	private final PanelHost host;
	private final List<SidebarItemWidget> sidebar = new ArrayList<>();
	private final TextFieldWidget search = TextFieldWidget.search(Component.translatable("n4y.search"));
	private final ScrollState scroll = new ScrollState();

	private ModuleCategory category;
	/** Filtering resolves and compares strings, so the result is kept until the inputs change. */
	private List<Module> visible;
	private String visibleQuery = "";
	private ModuleCategory visibleCategory;
	private float listTop;
	private float listBottom;

	public ModulesView(PanelHost host, ModuleCategory initialCategory) {
		this.host = host;
		this.category = initialCategory == null ? ModuleCategory.ALL : initialCategory;
		for (ModuleCategory value : ModuleCategory.values()) {
			sidebar.add(new SidebarItemWidget(value, () -> category == value));
		}
	}

	public TextFieldWidget search() {
		return search;
	}

	public List<SidebarItemWidget> sidebarItems() {
		return List.copyOf(sidebar);
	}

	public ModuleCategory category() {
		return category;
	}

	public String query() {
		return search.text();
	}

	/** The modules listed now, after the category filter and the search query. */
	public List<Module> listedModules() {
		String query = search.text();
		if (visible == null || !query.equals(visibleQuery) || category != visibleCategory) {
			visibleQuery = query;
			visibleCategory = category;
			visible = N4yClient.get().modules().filter(category, query);
		}
		return visible;
	}

	/** Called when the search text changed, so the list starts from the top again. */
	public void onQueryChanged() {
		scroll.reset();
	}

	@Override
	public ScrollState scroll() {
		return scroll;
	}

	@Override
	public Optional<TextFieldWidget> focusedField() {
		return search.isFocused() ? Optional.of(search) : Optional.empty();
	}

	@Override
	public void onShown() {
		search.setFocused(false);
		scroll.reset();
	}

	/** Places the search box, which lives in the header rather than the body. */
	public void layoutSearch(PanelGeometry geom) {
		float searchW = 118f;
		search.bounds(geom.x() + PanelGeometry.WIDTH - PanelGeometry.PAD - searchW, geom.y() + 13f,
				searchW, PanelGeometry.FIELD_H);
	}

	@Override
	public void layout(PanelGeometry geom, float scrollOffset) {
		float itemH = Math.min(ITEM_MAX_H,
				(geom.bodyH() - 16f - ITEM_GAP * (sidebar.size() - 1)) / sidebar.size());
		float itemY = geom.bodyY() + 8f;
		for (int i = 0; i < sidebar.size(); i++) {
			sidebar.get(i).bounds(geom.x() + PanelGeometry.PAD + SIDEBAR_INSET, itemY,
					PanelGeometry.SIDEBAR_W - SIDEBAR_INSET * 2, itemH);
			itemY += itemH + ITEM_GAP;
		}

		List<Module> modules = listedModules();
		float cardW = (geom.contentW() - PanelGeometry.GAP) / 2f;
		int rows = (modules.size() + 1) / 2;
		scroll.setExtent(rows * PanelGeometry.CARD_H + Math.max(0, rows - 1) * PanelGeometry.GAP - geom.bodyH());
		for (int i = 0; i < modules.size(); i++) {
			ModuleCardWidget card = host.card(modules.get(i));
			if (card != null) {
				card.bounds(geom.contentX() + (i % 2) * (cardW + PanelGeometry.GAP),
						geom.bodyY() + (i / 2) * (PanelGeometry.CARD_H + PanelGeometry.GAP) - scrollOffset,
						cardW, PanelGeometry.CARD_H);
			}
		}
		listTop = geom.bodyY();
		listBottom = listTop + geom.bodyH();
	}

	@Override
	public void render(UiContext ctx, PanelGeometry geom) {
		renderSidebar(ctx, geom);

		List<Module> modules = listedModules();
		float cx = geom.contentX();
		float cw = geom.contentW();
		float top = geom.bodyY();
		float ch = geom.bodyH();

		if (modules.isEmpty()) {
			Component message = search.isEmpty()
					? EMPTY_CATEGORY
					: Component.translatable("n4y.empty.search", search.text());
			N4yText.drawCentered(ctx.g(), N4yText.resolve(message), cx + cw / 2f, top + ch / 2f, 0.85f,
					Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
			return;
		}

		ctx.g().enableScissor(Math.round(cx), Math.round(top), Math.round(cx + cw), Math.round(top + ch));
		for (int i = 0; i < modules.size(); i++) {
			ModuleCardWidget card = host.card(modules.get(i));
			if (card != null && card.y() + card.height() >= top - 2 && card.y() <= top + ch + 2) {
				card.render(ctx);
			}
		}
		ctx.g().disableScissor();
		PanelScrollbar.render(ctx, scroll, cx, top, cw, ch);
	}

	private void renderSidebar(UiContext ctx, PanelGeometry geom) {
		float x = geom.x() + PanelGeometry.PAD;
		float y = geom.bodyY();
		float h = geom.bodyH();
		GlassRenderer.roundedRect(ctx.g(), x, y, PanelGeometry.SIDEBAR_W, h, 11f,
				Colors.fade(Colors.shade(ctx.theme().surface(), 0.03f), ctx.alpha() * 0.75f),
				Colors.fade(ctx.theme().surface(), ctx.alpha() * 0.55f), GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), x, y, PanelGeometry.SIDEBAR_W, h, 11f, 0.5f,
				Colors.fade(ctx.theme().borderTop(), ctx.alpha() * 0.5f),
				Colors.fade(ctx.theme().borderBottom(), ctx.alpha()));
		for (int i = 0; i < sidebar.size(); i++) {
			sidebar.get(i).render(ctx);
		}
	}

	@Override
	public boolean click(float mx, float my, boolean right) {
		if (search.contains(mx, my)) {
			search.setFocused(!right);
			return true;
		}
		search.setFocused(false);
		for (int i = 0; i < sidebar.size(); i++) {
			SidebarItemWidget item = sidebar.get(i);
			if (item.contains(mx, my)) {
				if (category != item.category()) {
					category = item.category();
					scroll.reset();
				}
				return true;
			}
		}
		return PanelCards.click(listedModules(), host, mx, my, right, listTop, listBottom);
	}
}
