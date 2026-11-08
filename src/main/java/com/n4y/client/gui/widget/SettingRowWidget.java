package com.n4y.client.gui.widget;

import java.util.function.BooleanSupplier;

import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntRangeSetting;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;
import com.n4y.client.module.setting.Setting;

import net.minecraft.network.chat.Component;

/**
 * One line of a module's settings panel: the name, an optional explanation and the control
 * that fits the kind of setting.
 */
public final class SettingRowWidget extends N4yWidget {
	public static final float HEIGHT_PLAIN = 28f;
	public static final float HEIGHT_DESCRIBED = 38f;
	private static final float CONTROL_W = 150f;
	private static final float TOGGLE_W = 24f;
	private static final float TOGGLE_H = 13f;
	private static final float CHIP_H = 14f;

	private final Setting<?> setting;
	private final BooleanSupplier binding;
	private final ToggleSwitch toggle;
	private final SliderWidget slider;
	private final RangeSliderWidget range;

	public SettingRowWidget(Setting<?> setting, BooleanSupplier binding) {
		this.setting = setting;
		this.binding = binding;
		if (setting instanceof BooleanSetting bool) {
			toggle = new ToggleSwitch();
			toggle.setInstant(bool.value());
		} else {
			toggle = null;
		}
		slider = setting instanceof IntSetting number ? new SliderWidget(number) : null;
		range = setting instanceof IntRangeSetting span ? new RangeSliderWidget(span) : null;
	}

	public Setting<?> setting() {
		return setting;
	}

	public float preferredHeight() {
		return setting.hasDescription() ? HEIGHT_DESCRIBED : HEIGHT_PLAIN;
	}

	public boolean isDragging() {
		return slider != null && slider.isDragging() || range != null && range.isDragging();
	}

	@Override
	public void bounds(float x, float y, float width, float height) {
		super.bounds(x, y, width, height);
		float controlX = x + width - CONTROL_W;
		if (slider != null) {
			slider.bounds(controlX, y, CONTROL_W, height);
		}
		if (range != null) {
			range.bounds(controlX, y, CONTROL_W, height);
		}
	}

	/** Handles a click; returns true when this row consumed it. */
	public boolean click(double mx, double my) {
		if (!contains(mx, my)) {
			return false;
		}
		if (setting instanceof BooleanSetting bool) {
			bool.toggle();
			if (toggle != null) {
				toggle.set(bool.value(), System.nanoTime());
			}
			return true;
		}
		if (slider != null) {
			return slider.click(mx, my);
		}
		if (range != null) {
			return range.click(mx, my);
		}
		// Key settings open the binding prompt; the screen owns that state.
		return setting instanceof KeySetting;
	}

	public void drag(double mx) {
		if (slider != null) {
			slider.drag(mx);
		}
		if (range != null) {
			range.drag(mx);
		}
	}

	public void release() {
		if (slider != null) {
			slider.release();
		}
		if (range != null) {
			range.release();
		}
	}

	@Override
	protected void draw(UiContext ctx, float hovered) {
		Theme t = ctx.theme();
		float a = ctx.alpha();

		if (hovered > 0.02f) {
			GlassRenderer.roundedRect(ctx.g(), x - 6, y, width + 12, height, 7f,
					Colors.fade(t.surface(), a * 0.5f * hovered));
		}

		boolean described = setting.hasDescription();
		float nameY = described ? y + height / 2f - 7f : y + height / 2f;
		N4yText.drawMiddle(ctx.g(), setting.displayName().getString(), x, nameY, 0.92f,
				Colors.fade(t.textPrimary(), a), false);
		if (described) {
			N4yText.drawMiddle(ctx.g(), N4yText.ellipsize(setting.description().getString(), false, 0.74f, width - CONTROL_W - 8f),
					x, y + height / 2f + 6f, 0.74f, Colors.fade(t.textMuted(), a), false);
		}

		if (setting instanceof BooleanSetting bool && toggle != null) {
			toggle.set(bool.value(), ctx.now());
			toggle.draw(ctx, x + width - TOGGLE_W, y + (height - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H);
		} else if (slider != null) {
			slider.render(ctx);
		} else if (range != null) {
			range.render(ctx);
		} else if (setting instanceof KeySetting keySetting) {
			drawKeyChip(ctx, keySetting, hovered, a, t);
		}
	}

	private void drawKeyChip(UiContext ctx, KeySetting keySetting, float hovered, float a, Theme t) {
		boolean isBinding = binding.getAsBoolean();
		String label = isBinding
				? Component.translatable("n4y.keybind.press").getString()
				: keySetting.binding().displayName().getString();
		float chipW = Math.min(110f, N4yText.width(label, false, 0.78f) + 14f);
		float cx = x + width - chipW;
		float cy = y + (height - CHIP_H) / 2f;
		float strength = isBinding ? 1f : 0.3f + 0.5f * hovered;

		GlassRenderer.roundedRect(ctx.g(), cx, cy, chipW, CHIP_H, 5f,
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), isBinding ? 0.7f : 0f), a * (0.35f + 0.35f * strength)),
				Colors.fade(Colors.lerp(t.toggleOff(), t.accent(), isBinding ? 0.5f : 0f), a * (0.25f + 0.3f * strength)),
				GlassRenderer.CORNERS_ALL);
		GlassRenderer.border(ctx.g(), cx, cy, chipW, CHIP_H, 5f, 0.5f,
				Colors.fade(t.borderTop(), a * strength), Colors.fade(t.borderBottom(), a * strength));
		N4yText.drawCentered(ctx.g(), label, cx + chipW / 2f, cy + CHIP_H / 2f, 0.78f,
				Colors.fade(isBinding || keySetting.isBound() ? t.textPrimary() : t.textMuted(), a), false);
	}
}
