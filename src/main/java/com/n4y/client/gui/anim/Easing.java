package com.n4y.client.gui.anim;

/** Easing curves mapping linear progress 0..1 to eased progress. */
@FunctionalInterface
public interface Easing {
	float apply(float t);

	Easing LINEAR = t -> t;
	Easing OUT_CUBIC = t -> 1f - (float) Math.pow(1f - t, 3);
	Easing IN_OUT_CUBIC = t -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
	Easing OUT_QUINT = t -> 1f - (float) Math.pow(1f - t, 5);
	/** Slight overshoot, used for toggles and "liquid" pops. */
	Easing OUT_BACK = t -> {
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		float x = t - 1f;
		return 1f + c3 * x * x * x + c1 * x * x;
	};
}
