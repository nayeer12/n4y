package com.n4y.client.gui.screen.panel;

import com.n4y.client.gui.anim.Animation;
import com.n4y.client.gui.anim.Easing;

/**
 * Scroll position of one scrollable list: where it is, where it is heading and how far it may go.
 *
 * <p>This exists because clamping the target to the content height was written out again at every
 * place that laid out a list — five copies of the same two {@code Math} calls, each able to drift
 * from the others — while the extent, the target and the animation were three loose fields that
 * only meant anything together. Keeping them in one object means the clamp happens in exactly one
 * place: nothing can set a target past the end, because setting the extent re-checks the target.
 */
public final class ScrollState {
	/** Below this the list fits and the scrollbar and fade edges are not drawn. */
	private static final float OVERFLOW_EPSILON = 0.5f;

	private final Animation animation = new Animation(0f, 180, Easing.OUT_CUBIC);
	private float target;
	private float extent;

	/**
	 * Sets how far the list can scroll, which is the content height minus the visible height.
	 * A target that no longer fits is pulled back, so shrinking the content cannot leave the
	 * list scrolled past its end.
	 */
	public void setExtent(float value) {
		extent = Math.max(0f, value);
		target = clamp(target);
	}

	public float extent() {
		return extent;
	}

	/** True when there is anything to scroll at all. */
	public boolean canScroll() {
		return extent > 0f;
	}

	/** True when the overflow is worth drawing a scrollbar and fade edges for. */
	public boolean hasOverflow() {
		return extent > OVERFLOW_EPSILON;
	}

	/** Advances the animation towards the target; call once per frame before reading the offset. */
	public void advance(long now) {
		animation.animateTo(target, now);
	}

	/** The offset to place the content at, in GUI units from the top of the list. */
	public float offset(long now) {
		return animation.value(now);
	}

	/**
	 * Moves the target by {@code amount} GUI units.
	 *
	 * @return false when the list cannot scroll, so the caller can pass the input on
	 */
	public boolean scrollBy(float amount) {
		if (!canScroll()) {
			return false;
		}
		target = clamp(target + amount);
		return true;
	}

	/** Jumps back to the top without animating, for when the content is replaced. */
	public void reset() {
		target = 0f;
		animation.set(0f);
	}

	private float clamp(float value) {
		return Math.max(0f, Math.min(value, extent));
	}
}
