package com.n4y.client.gui.icon;

import java.util.ArrayList;
import java.util.List;

/**
 * A line icon described in a 24×24 design space (y grows downwards), rasterised on demand by
 * {@link IconTextures}. Shapes are strokes, so one definition stays crisp at any size.
 *
 * @param key       cache key, unique per icon
 * @param polylines open or closed paths, flattened as {@code x0,y0,x1,y1,…}
 * @param rings     stroked circles as {@code cx,cy,r}
 * @param dots      filled circles as {@code cx,cy,r}
 * @param stroke    stroke width in design units
 */
public record Icon(String key, List<float[]> polylines, List<float[]> rings, List<float[]> dots, float stroke) {
	public static final float SPACE = 24f;

	public static Builder of(String key) {
		return new Builder(key);
	}

	/** Collects the primitives of one icon. */
	public static final class Builder {
		private final String key;
		private final List<float[]> polylines = new ArrayList<>();
		private final List<float[]> rings = new ArrayList<>();
		private final List<float[]> dots = new ArrayList<>();
		private float stroke = 1.9f;

		private Builder(String key) {
			this.key = key;
		}

		public Builder stroke(float width) {
			this.stroke = width;
			return this;
		}

		public Builder line(float... points) {
			polylines.add(points);
			return this;
		}

		/** Closed path: repeats the first point at the end. */
		public Builder shape(float... points) {
			float[] closed = new float[points.length + 2];
			System.arraycopy(points, 0, closed, 0, points.length);
			closed[points.length] = points[0];
			closed[points.length + 1] = points[1];
			polylines.add(closed);
			return this;
		}

		public Builder ring(float cx, float cy, float r) {
			rings.add(new float[] {cx, cy, r});
			return this;
		}

		public Builder dot(float cx, float cy, float r) {
			dots.add(new float[] {cx, cy, r});
			return this;
		}

		/** Quadratic bézier sampled into a polyline. */
		public Builder curve(float x0, float y0, float cx, float cy, float x1, float y1) {
			int steps = 10;
			float[] pts = new float[(steps + 1) * 2];
			for (int i = 0; i <= steps; i++) {
				float t = i / (float) steps;
				float u = 1f - t;
				pts[i * 2] = u * u * x0 + 2 * u * t * cx + t * t * x1;
				pts[i * 2 + 1] = u * u * y0 + 2 * u * t * cy + t * t * y1;
			}
			polylines.add(pts);
			return this;
		}

		public Icon build() {
			return new Icon(key, List.copyOf(polylines), List.copyOf(rings), List.copyOf(dots), stroke);
		}
	}
}
