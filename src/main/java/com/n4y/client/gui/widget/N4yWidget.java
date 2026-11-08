package com.n4y.client.gui.widget;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;

/**
 * Base of every n4y widget. The screen owns the layout and writes {@link #bounds} each frame;
 * the widget owns its animation state and drawing.
 */
public abstract class N4yWidget {
	protected float x;
	protected float y;
	protected float width;
	protected float height;
	protected final Animation hover = new Animation(0f, 150, Easing.OUT_CUBIC);

	public void bounds(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public float x() {
		return x;
	}

	public float y() {
		return y;
	}

	public float width() {
		return width;
	}

	public float height() {
		return height;
	}

	public boolean contains(double mx, double my) {
		return mx >= x && my >= y && mx < x + width && my < y + height;
	}

	/** Advances the hover animation and draws the widget. */
	public final void render(UiContext ctx) {
		hover.animateTo(contains(ctx.mouseX(), ctx.mouseY()) ? 1f : 0f, ctx.now());
		draw(ctx, hover.value(ctx.now()));
	}

	protected abstract void draw(UiContext ctx, float hovered);
}
