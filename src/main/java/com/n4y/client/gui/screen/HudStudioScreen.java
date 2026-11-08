package com.n4y.client.gui.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.N4yClient;
import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;
import com.n4y.client.gui.hud.HudAnchor;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.gui.render.Colors;
import com.n4y.client.gui.render.GlassRenderer;
import com.n4y.client.gui.render.N4yText;
import com.n4y.client.gui.theme.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Drag the on-screen elements where you want them. Dropping one anchors it to the nearest
 * corner or edge, so the layout survives a change of resolution, and the positions are saved
 * into the active profile.
 */
public final class HudStudioScreen extends Screen {
	private final Screen parent;
	private final Animation fade = new Animation(0f, 220, Easing.OUT_CUBIC);

	private HudModule dragging;
	private float grabX;
	private float grabY;

	public HudStudioScreen(Screen parent) {
		super(Component.translatable("n4y.hud.studio"));
		this.parent = parent;
	}

	private List<HudModule> movable() {
		return N4yClient.get().hud().elements().stream()
				.filter(element -> element.isEnabled() && element.movable())
				.toList();
	}

	@Override
	protected void init() {
		fade.animateTo(1f);
		N4yClient.get().hud().setSuppressed(true);
	}

	@Override
	public void removed() {
		N4yClient.get().hud().setSuppressed(false);
		N4yClient.get().config().markDirty();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	// ---------------------------------------------------------------- render

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		long now = System.nanoTime();
		Theme theme = N4yClient.get().themes().current(now);
		float a = fade.value(now);

		GlassRenderer.backdrop(g, width, height, theme, a * 0.75f);
		drawGuides(g, theme, a);

		Minecraft mc = minecraft;
		for (HudModule element : N4yClient.get().hud().elements()) {
			if (!element.isEnabled()) {
				continue;
			}
			float w = element.width(mc);
			float h = element.height(mc);
			float x = element.placement().screenX(width, w);
			float y = element.placement().screenY(height, h);
			element.drawHud(g, mc, theme, x, y);
			boolean active = element == dragging || contains(element, mouseX, mouseY);
			int outline = Colors.fade(element.movable() ? theme.accent() : theme.textMuted(), a * (active ? 1f : 0.5f));
			GlassRenderer.border(g, x - 2, y - 2, w + 4, h + 4, 8f, active ? 1f : 0.6f, outline, outline);
			if (!element.movable()) {
				N4yText.drawCentered(g, Component.translatable("n4y.hud.fixed").getString(),
						x + w / 2f, y - 8f, 0.72f, Colors.fade(theme.textMuted(), a), false);
			}
		}

		drawHeader(g, theme, a);
	}

	/** Faint marks at every anchor, so it is clear where an element will snap. */
	private void drawGuides(GuiGraphics g, Theme theme, float a) {
		int color = Colors.fade(theme.borderTop(), a * 0.35f);
		for (HudAnchor anchor : HudAnchor.values()) {
			float x = anchor.originX(width, 0f);
			float y = anchor.originY(height, 0f);
			GlassRenderer.roundedRect(g, x - 4f, y - 0.5f, 8f, 1f, 0.5f, color);
			GlassRenderer.roundedRect(g, x - 0.5f, y - 4f, 1f, 8f, 0.5f, color);
		}
	}

	private void drawHeader(GuiGraphics g, Theme theme, float a) {
		String title = getTitle().getString();
		String hint = Component.translatable("n4y.hud.studio.hint").getString();
		float w = Math.max(N4yText.width(title, true, 1.1f), N4yText.width(hint, false, 0.8f)) + 28f;
		float h = 42f;
		float x = (width - w) / 2f;
		float y = 10f;
		GlassRenderer.glassPanel(g, x, y, w, h, 11f, theme, a);
		N4yText.drawCentered(g, title, x + w / 2f, y + 14f, 1.1f, Colors.fade(theme.textPrimary(), a), true);
		N4yText.drawCentered(g, hint, x + w / 2f, y + 29f, 0.8f, Colors.fade(theme.textMuted(), a), false);
	}

	// ---------------------------------------------------------------- input

	private boolean contains(HudModule element, double mx, double my) {
		float w = element.width(minecraft);
		float h = element.height(minecraft);
		float x = element.placement().screenX(width, w);
		float y = element.placement().screenY(height, h);
		return mx >= x - 2 && my >= y - 2 && mx < x + w + 2 && my < y + h + 2;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (HudModule element : movable()) {
				if (contains(element, event.x(), event.y())) {
					dragging = element;
					grabX = (float) event.x() - element.placement().screenX(width, element.width(minecraft));
					grabY = (float) event.y() - element.placement().screenY(height, element.height(minecraft));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging != null) {
			moveTo(dragging, (float) event.x() - grabX, (float) event.y() - grabY);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			N4yClient.get().config().markDirty();
			return true;
		}
		return super.mouseReleased(event);
	}

	/** Keeps the element on screen and re-anchors it to the nearest corner. */
	private void moveTo(HudModule element, float x, float y) {
		float w = element.width(minecraft);
		float h = element.height(minecraft);
		float clampedX = Math.max(0f, Math.min(width - w, x));
		float clampedY = Math.max(0f, Math.min(height - h, y));
		element.setPlacement(HudPlacement.fromScreen(clampedX, clampedY, w, h, width, height));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_R) {
			N4yClient.get().hud().elements().forEach(HudModule::resetPlacement);
			N4yClient.get().config().markDirty();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
