package com.n4y.client.gui.screen.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.IconTextures;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.gui.widget.ButtonWidget;
import com.n4y.client.gui.widget.N4yWidget;
import com.n4y.client.gui.widget.SettingRowWidget;
import com.n4y.client.gui.widget.ToggleSwitch;
import com.n4y.client.gui.widget.UiContext;
import com.n4y.client.module.Module;
import com.n4y.client.module.setting.KeySetting;
import com.n4y.client.module.setting.Setting;

import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * One module's settings, shown in place of whichever section was open.
 *
 * <p>It owns the binding of a key to a single setting, because that is only ever in flight while
 * this panel is showing. Binding a key to the module itself belongs to the screen: that can be
 * started from a card in two other sections.
 */
public final class ModuleDetailView implements PanelView {
	private static final Component PRESS_A_KEY = Component.translatable("n4y.keybind.press");
	private static final Component NO_SETTINGS = Component.translatable("n4y.settings.none");
	private static final Component RESET = Component.translatable("n4y.settings.reset");
	/** The switch and the key chip in the header share this band. */
	private static final float CONTROL_Y = 13f;
	private static final float CONTROL_H = 14f;
	private static final float TOGGLE_W = 26f;
	private static final float CHIP_MAX_W = 96f;
	private static final float RESET_W = 110f;
	private static final float ROW_INSET = 6f;
	private static final float ROW_GAP = 2f;

	private final PanelHost host;
	private final List<SettingRowWidget> rows = new ArrayList<>();
	private final ToggleSwitch toggle = new ToggleSwitch();
	private final ButtonWidget reset;
	private final ScrollState scroll = new ScrollState();
	private final N4yWidget backButton = new N4yWidget() {
		@Override
		protected void draw(UiContext ctx, float hovered) {
			int color = Colors.fade(Colors.lerp(ctx.theme().textSecondary(), ctx.theme().textPrimary(), hovered),
					ctx.alpha());
			if (hovered > 0.02f) {
				GlassRenderer.roundedRect(ctx.g(), x(), y(), width(), height(), 7f,
						Colors.fade(ctx.theme().surface(), ctx.alpha() * hovered * 0.8f));
			}
			IconTextures.draw(ctx.g(), com.n4y.client.gui.icon.Icons.ARROW_LEFT,
					x() + (width() - 13f) / 2f, y() + (height() - 13f) / 2f, 13f, color);
		}
	};

	private Module module;
	private KeySetting bindingSetting;
	private SettingRowWidget draggingRow;
	private float chipX;
	private float chipW;
	private float toggleLeft;
	private float controlTop;
	/** Kept from the last layout so a click cannot reach a row scrolled out of sight. */
	private float listTop;
	private float listBottom;

	public ModuleDetailView(PanelHost host) {
		this.host = host;
		this.reset = new ButtonWidget(RESET, false, this::anySettingChanged);
	}

	/** Indexed rather than streamed: the button asks this while drawing itself, every frame. */
	private boolean anySettingChanged() {
		if (module == null) {
			return false;
		}
		List<Setting<?>> settings = module.settings();
		for (int i = 0; i < settings.size(); i++) {
			if (!settings.get(i).isDefault()) {
				return true;
			}
		}
		return false;
	}

	public boolean isOpen() {
		return module != null;
	}

	public Optional<Module> module() {
		return Optional.ofNullable(module);
	}

	public List<SettingRowWidget> rows() {
		return List.copyOf(rows);
	}

	/** Shows {@code target}'s settings. */
	public void open(Module target) {
		module = target;
		bindingSetting = null;
		draggingRow = null;
		rows.clear();
		for (Setting<?> setting : target.settings()) {
			rows.add(new SettingRowWidget(setting, () -> bindingSetting == setting));
		}
		toggle.setInstant(target.isEnabled());
		scroll.reset();
	}

	public void close() {
		module = null;
		bindingSetting = null;
		draggingRow = null;
		rows.clear();
		scroll.reset();
	}

	@Override
	public ScrollState scroll() {
		return scroll;
	}

	// ------------------------------------------------------------------ binding a setting's key

	/**
	 * Finishes or cancels a setting's key binding with a mouse button.
	 *
	 * <p>Clicking the row that is waiting binds that button. Clicking anywhere else cancels, and
	 * swallows a primary click so the cancel does not also act on whatever is under the cursor; a
	 * secondary click is allowed through, because that starts a new binding.
	 *
	 * @return true when this click must go no further
	 */
	public boolean consumeBindingClick(float mx, float my, int button, boolean right) {
		if (bindingSetting == null) {
			return false;
		}
		SettingRowWidget row = rowOf(bindingSetting);
		if (row != null && row.contains(mx, my)) {
			bindingSetting.set(InputBinding.mouse(button));
			bindingSetting = null;
			return true;
		}
		bindingSetting = null;
		return !right;
	}

	/** Finishes a setting's key binding; Escape clears it. */
	public boolean consumeBindingKey(int key) {
		if (bindingSetting == null) {
			return false;
		}
		bindingSetting.set(key == GLFW.GLFW_KEY_ESCAPE ? InputBinding.UNBOUND : InputBinding.key(key));
		bindingSetting = null;
		return true;
	}

	private SettingRowWidget rowOf(Setting<?> setting) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).setting() == setting) {
				return rows.get(i);
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ dragging a slider

	public boolean dragTo(float mx) {
		if (draggingRow == null) {
			return false;
		}
		draggingRow.drag(mx);
		return true;
	}

	public boolean releaseDrag() {
		if (draggingRow == null) {
			return false;
		}
		draggingRow.release();
		draggingRow = null;
		return true;
	}

	// ------------------------------------------------------------------ layout

	@Override
	public void layout(PanelGeometry geom, float scrollOffset) {
		controlTop = geom.bodyY() + CONTROL_Y;
		toggleLeft = geom.x() + PanelGeometry.WIDTH - PanelGeometry.PAD - TOGGLE_W;
		backButton.bounds(geom.listX(), geom.bodyY() + 8f, 22f, 22f);
		layoutKeyChip();
		listTop = geom.detailListTop();
		listBottom = listTop + geom.detailListH();

		float rowX = geom.listX() + ROW_INSET;
		float rowW = PanelGeometry.WIDTH - PanelGeometry.PAD * 2 - ROW_INSET * 2 - PanelGeometry.SCROLLBAR_W;
		float total = 0f;
		for (int i = 0; i < rows.size(); i++) {
			SettingRowWidget row = rows.get(i);
			float h = row.preferredHeight();
			row.bounds(rowX, geom.detailListTop() + total - scrollOffset, rowW, h);
			total += h + ROW_GAP;
		}
		scroll.setExtent(total - ROW_GAP - geom.detailListH());
		reset.bounds(geom.x() + PanelGeometry.WIDTH - PanelGeometry.PAD - RESET_W,
				geom.bodyY() + geom.bodyH() - PanelGeometry.FIELD_H, RESET_W, PanelGeometry.FIELD_H);
	}

	/**
	 * Places the key chip. This used to be worked out while drawing it and read back by the click
	 * handler, so before the panel's first frame the chip's hit box was still at the origin.
	 */
	private void layoutKeyChip() {
		chipW = Math.min(CHIP_MAX_W, N4yText.width(keyLabel(), false, 0.78f) + 14f);
		chipX = toggleLeft - 8f - chipW;
	}

	private String keyLabel() {
		if (module == null) {
			return "";
		}
		return host.isBinding(module)
				? N4yText.resolve(PRESS_A_KEY)
				: N4yText.resolve(module.key().displayName());
	}

	private boolean overControlBand(float my) {
		return my >= controlTop && my <= controlTop + CONTROL_H;
	}

	/** True when the cursor is over the module's key chip. */
	public boolean overKeyChip(float mx, float my) {
		return mx >= chipX && mx <= chipX + chipW && overControlBand(my);
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(UiContext ctx, PanelGeometry geom) {
		if (module == null) {
			return;
		}
		Theme t = ctx.theme();
		float a = ctx.alpha();

		backButton.render(ctx);
		IconTextures.draw(ctx.g(), module.icon(), geom.listX() + 30f, geom.bodyY() + 10.5f, 17f,
				Colors.fade(t.textPrimary(), a));
		N4yText.draw(ctx.g(), N4yText.resolve(module.displayName()), geom.listX() + 54f, geom.bodyY() + 8f, 1.05f,
				Colors.fade(t.textPrimary(), a), true);
		N4yText.draw(ctx.g(),
				N4yText.ellipsize(N4yText.resolve(module.description()), false, 0.78f, PanelGeometry.WIDTH - 220f),
				geom.listX() + 54f, geom.bodyY() + 21f, 0.78f, Colors.fade(t.textSecondary(), a), false);

		toggle.set(module.isEnabled(), ctx.now());
		toggle.draw(ctx, toggleLeft, controlTop, TOGGLE_W, CONTROL_H);
		drawKeyChip(ctx, t, a);

		float listX = geom.listX();
		float listW = geom.listW();
		float listTop = geom.detailListTop();
		float listH = geom.detailListH();

		if (rows.isEmpty()) {
			N4yText.drawCentered(ctx.g(), N4yText.resolve(NO_SETTINGS), listX + listW / 2f, listTop + listH / 2f,
					0.85f, Colors.fade(t.textMuted(), a), false);
		} else {
			ctx.g().enableScissor(Math.round(listX), Math.round(listTop),
					Math.round(listX + listW), Math.round(listTop + listH));
			for (int i = 0; i < rows.size(); i++) {
				SettingRowWidget row = rows.get(i);
				if (row.y() + row.height() >= listTop - 2 && row.y() <= listTop + listH + 2) {
					row.render(ctx);
				}
			}
			ctx.g().disableScissor();
			PanelScrollbar.render(ctx, scroll, listX, listTop, listW, listH);
		}
		reset.render(ctx);
	}

	/** The module's own on/off key, shown next to its switch. */
	private void drawKeyChip(UiContext ctx, Theme t, float a) {
		boolean binding = host.isBinding(module);
		String label = keyLabel();
		GlassRenderer.roundedRect(ctx.g(), chipX, controlTop, chipW, CONTROL_H, 5f,
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), binding ? 0.7f : 0f), a * 0.5f),
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), binding ? 0.5f : 0f), a * 0.4f),
				GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), chipX, controlTop, chipW, CONTROL_H, 5f, 0.5f,
				Colors.fade(t.borderTop(), a), Colors.fade(t.borderBottom(), a));
		N4yText.drawCentered(ctx.g(), label, chipX + chipW / 2f, controlTop + CONTROL_H / 2f, 0.78f,
				Colors.fade(module.hasKey() || binding ? t.textPrimary() : t.textMuted(), a), false);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean click(float mx, float my, boolean right) {
		if (module == null) {
			return false;
		}
		if (backButton.contains(mx, my)) {
			host.closeDetail();
			return true;
		}
		if (overToggle(mx, my)) {
			module.toggle();
			toggle.set(module.isEnabled(), System.nanoTime());
			return true;
		}
		if (overKeyChip(mx, my)) {
			host.startBinding(module);
			return true;
		}
		if (reset.contains(mx, my)) {
			if (reset.isUsable()) {
				module.resetSettings();
			}
			return true;
		}
		return clickRows(mx, my);
	}

	private boolean overToggle(float mx, float my) {
		return mx >= toggleLeft && mx <= toggleLeft + TOGGLE_W && overControlBand(my);
	}

	private boolean clickRows(float mx, float my) {
		if (my < listTop || my > listBottom) {
			return false;
		}
		for (int i = 0; i < rows.size(); i++) {
			SettingRowWidget row = rows.get(i);
			if (!row.contains(mx, my)) {
				continue;
			}
			if (row.setting() instanceof KeySetting keySetting) {
				bindingSetting = keySetting;
			} else if (row.click(mx, my)) {
				draggingRow = row.isDragging() ? row : null;
			}
			return true;
		}
		return false;
	}
}
