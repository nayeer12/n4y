package com.n4y.client.gui.screen.panel;

import java.util.List;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.widget.ButtonWidget;
import com.n4y.client.gui.widget.ModuleCardWidget;
import com.n4y.client.gui.widget.UiContext;

import net.minecraft.network.chat.Component;

/** The on-screen elements: the same cards as the module grid, plus a way into the HUD Studio. */
public final class HudView implements PanelView {
	private static final Component HINT = Component.translatable("n4y.hud.hint");
	private static final float STUDIO_W = 116f;

	private final PanelHost host;
	/** Kept from the last layout so a click can tell which rows were actually visible. */
	private float listTop;
	private float listBottom;
	private final ButtonWidget openStudio =
			new ButtonWidget(Component.translatable("n4y.hud.studio.open"), true, () -> true);
	private final ScrollState scroll = new ScrollState();

	public HudView(PanelHost host) {
		this.host = host;
	}

	private static List<HudModule> elements() {
		return N4yClient.get().hud().elements();
	}

	@Override
	public ScrollState scroll() {
		return scroll;
	}

	@Override
	public void layout(PanelGeometry geom, float scrollOffset) {
		List<HudModule> elements = elements();
		float cardW = (geom.listW() - PanelGeometry.GAP) / 2f;
		int rows = (elements.size() + 1) / 2;
		scroll.setExtent(rows * PanelGeometry.CARD_H + Math.max(0, rows - 1) * PanelGeometry.GAP - geom.hudListH());
		for (int i = 0; i < elements.size(); i++) {
			ModuleCardWidget card = host.card(elements.get(i));
			if (card != null) {
				card.bounds(geom.listX() + (i % 2) * (cardW + PanelGeometry.GAP),
						geom.bodyY() + (i / 2) * (PanelGeometry.CARD_H + PanelGeometry.GAP) - scrollOffset,
						cardW, PanelGeometry.CARD_H);
			}
		}
		openStudio.bounds(geom.x() + PanelGeometry.WIDTH - PanelGeometry.PAD - STUDIO_W,
				geom.bodyY() + geom.bodyH() - PanelGeometry.FIELD_H, STUDIO_W, PanelGeometry.FIELD_H);
		listTop = geom.bodyY();
		listBottom = listTop + geom.hudListH();
	}

	@Override
	public void render(UiContext ctx, PanelGeometry geom) {
		List<HudModule> elements = elements();
		float listX = geom.listX();
		float listW = geom.listW();
		float listH = geom.hudListH();
		float top = geom.bodyY();

		ctx.g().enableScissor(Math.round(listX), Math.round(top), Math.round(listX + listW), Math.round(top + listH));
		for (int i = 0; i < elements.size(); i++) {
			ModuleCardWidget card = host.card(elements.get(i));
			if (card != null && card.y() + card.height() >= top - 2 && card.y() <= top + listH + 2) {
				card.render(ctx);
			}
		}
		ctx.g().disableScissor();
		PanelScrollbar.render(ctx, scroll, listX, top, listW, listH);

		N4yText.drawMiddle(ctx.g(), N4yText.resolve(HINT), listX,
				geom.bodyY() + geom.bodyH() - PanelGeometry.FIELD_H / 2f, 0.78f,
				Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		openStudio.render(ctx);
	}

	@Override
	public boolean click(float mx, float my, boolean right) {
		if (openStudio.contains(mx, my)) {
			host.openHudStudio();
			return true;
		}
		return PanelCards.click(elements(), host, mx, my, right, listTop, listBottom);
	}
}
