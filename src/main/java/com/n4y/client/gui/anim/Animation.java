package com.n4y.client.gui.anim;

/**
 * Time-based tween between two values. Driven by wall-clock time so it runs at the same
 * speed at any frame rate and never needs a tick.
 */
public final class Animation {
	private final long durationNanos;
	private final Easing easing;
	private float from;
	private float to;
	private long startNanos;

	public Animation(float value, long durationMillis, Easing easing) {
		this.from = value;
		this.to = value;
		this.durationNanos = Math.max(1L, durationMillis) * 1_000_000L;
		this.easing = easing;
		this.startNanos = 0L;
	}

	/** Starts animating from the current visual value to {@code target}. */
	public void animateTo(float target, long nowNanos) {
		if (target == to) {
			return;
		}
		from = value(nowNanos);
		to = target;
		startNanos = nowNanos;
	}

	public void animateTo(float target) {
		animateTo(target, System.nanoTime());
	}

	/** Jumps without animating. */
	public void set(float value) {
		from = value;
		to = value;
		startNanos = 0L;
	}

	public float value(long nowNanos) {
		float p = progress(nowNanos);
		return from + (to - from) * easing.apply(p);
	}

	public float value() {
		return value(System.nanoTime());
	}

	public float target() {
		return to;
	}

	public boolean isRunning(long nowNanos) {
		return progress(nowNanos) < 1f;
	}

	private float progress(long nowNanos) {
		if (from == to) {
			return 1f;
		}
		float p = (nowNanos - startNanos) / (float) durationNanos;
		return p < 0f ? 0f : Math.min(p, 1f);
	}
}
