package com.n4y.client.gui.screen.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.n4y.client.N4yClient;
import com.n4y.client.config.ConfigManager;
import com.n4y.client.config.Profile;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.widget.ButtonWidget;
import com.n4y.client.gui.widget.ProfileRowWidget;
import com.n4y.client.gui.widget.TextFieldWidget;
import com.n4y.client.gui.widget.UiContext;

import net.minecraft.network.chat.Component;

/** The saved profiles: switch to one, delete one, or create one from the bar at the bottom. */
public final class ProfilesView implements PanelView {
	private static final Component HINT = Component.translatable("n4y.keybind.hint");
	private static final float ROW_GAP = 6f;
	private static final float CREATE_W = 78f;

	private final PanelHost host;
	private final List<ProfileRowWidget> rows = new ArrayList<>();
	private final TextFieldWidget name =
			new TextFieldWidget(null, Component.translatable("n4y.profiles.name"), 24);
	private final ButtonWidget create;
	private final ScrollState scroll = new ScrollState();
	/** Kept from the last layout so a click cannot reach a row scrolled out of sight. */
	private float listTop;
	private float listBottom;

	public ProfilesView(PanelHost host) {
		this.host = host;
		this.create = new ButtonWidget(Component.translatable("n4y.profiles.create"), true,
				() -> Profile.isValidName(name.text()) && !config().exists(name.text()));
		refresh();
	}

	private static ConfigManager config() {
		return N4yClient.get().config();
	}

	/** Rebuilds the rows from what is on disk. */
	public void refresh() {
		rows.clear();
		for (String profile : config().profileNames()) {
			rows.add(new ProfileRowWidget(profile, () -> config().activeProfileName().equalsIgnoreCase(profile)));
		}
	}

	public List<ProfileRowWidget> rows() {
		return List.copyOf(rows);
	}

	public TextFieldWidget nameBox() {
		return name;
	}

	public ButtonWidget createButton() {
		return create;
	}

	@Override
	public ScrollState scroll() {
		return scroll;
	}

	@Override
	public Optional<TextFieldWidget> focusedField() {
		return name.isFocused() ? Optional.of(name) : Optional.empty();
	}

	@Override
	public void onShown() {
		refresh();
		name.setFocused(false);
		scroll.reset();
	}

	/** Creates the profile in the name box, if that name may be used. Also bound to Enter. */
	public boolean createFromNameBox() {
		if (!create.isUsable() || !config().createProfile(name.text())) {
			return false;
		}
		name.clear();
		refresh();
		scroll.reset();
		return true;
	}

	@Override
	public void layout(PanelGeometry geom, float scrollOffset) {
		scroll.setExtent(rows.size() * PanelGeometry.ROW_H + Math.max(0, rows.size() - 1) * ROW_GAP
				- geom.profileListH());
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).bounds(geom.listX(), geom.bodyY() + i * (PanelGeometry.ROW_H + ROW_GAP) - scrollOffset,
					geom.listW(), PanelGeometry.ROW_H);
		}
		float barY = geom.bodyY() + geom.bodyH() - PanelGeometry.FIELD_H;
		name.bounds(geom.listX(), barY,
				PanelGeometry.WIDTH - PanelGeometry.PAD * 2 - CREATE_W - PanelGeometry.GAP, PanelGeometry.FIELD_H);
		create.bounds(geom.x() + PanelGeometry.WIDTH - PanelGeometry.PAD - CREATE_W, barY,
				CREATE_W, PanelGeometry.FIELD_H);
		listTop = geom.bodyY();
		listBottom = listTop + geom.profileListH();
	}

	@Override
	public void render(UiContext ctx, PanelGeometry geom) {
		float listX = geom.listX();
		float listW = geom.listW();
		float listH = geom.profileListH();
		float top = geom.bodyY();

		ctx.g().enableScissor(Math.round(listX), Math.round(top), Math.round(listX + listW), Math.round(top + listH));
		for (int i = 0; i < rows.size(); i++) {
			ProfileRowWidget row = rows.get(i);
			if (row.y() + row.height() >= top - 2 && row.y() <= top + listH + 2) {
				row.render(ctx);
			}
		}
		ctx.g().disableScissor();
		PanelScrollbar.render(ctx, scroll, listX, top, listW, listH);

		N4yText.draw(ctx.g(), N4yText.resolve(HINT), listX, top + listH + 6f, 0.76f,
				Colors.fade(ctx.theme().textMuted(), ctx.alpha()), false);
		name.render(ctx);
		create.render(ctx);
	}

	@Override
	public boolean click(float mx, float my, boolean right) {
		if (name.contains(mx, my)) {
			name.setFocused(true);
			return true;
		}
		name.setFocused(false);
		if (create.contains(mx, my)) {
			createFromNameBox();
			return true;
		}
		if (my >= listTop && my <= listBottom) {
			for (int i = 0; i < rows.size(); i++) {
				ProfileRowWidget row = rows.get(i);
				if (!row.contains(mx, my)) {
					continue;
				}
				if (row.deleteContains(mx, my)) {
					if (config().deleteProfile(row.profileName())) {
						refresh();
					}
				} else if (!row.isActive() && config().switchProfile(row.profileName())) {
					refresh();
					host.onProfileSwitched(row.profileName());
				}
				return true;
			}
		}
		return false;
	}
}
