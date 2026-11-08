package com.n4y.client.gui.widget;

import com.n4y.client.gui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Everything a widget needs for one frame.
 *
 * @param g      target surface
 * @param theme  palette already interpolated by the theme engine
 * @param now    frame timestamp in nanoseconds, shared so widgets animate in lockstep
 * @param alpha  global fade of the interface (open/close animation)
 * @param mouseX cursor position in GUI units
 * @param mouseY cursor position in GUI units
 */
public record UiContext(GuiGraphics g, Theme theme, long now, float alpha, float mouseX, float mouseY) {
}
