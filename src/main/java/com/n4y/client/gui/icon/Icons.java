package com.n4y.client.gui.icon;

/**
 * The n4y icon set: thin line icons in the 24×24 design space, matching the reference design.
 */
public final class Icons {
	public static final Icon GRID = Icon.of("grid")
			.shape(4, 4, 10, 4, 10, 10, 4, 10)
			.shape(14, 4, 20, 4, 20, 10, 14, 10)
			.shape(4, 14, 10, 14, 10, 20, 4, 20)
			.shape(14, 14, 20, 14, 20, 20, 14, 20)
			.build();

	public static final Icon SWORDS = Icon.of("swords")
			.line(4.5f, 19.5f, 17.5f, 6.5f)
			.line(14.5f, 6.5f, 19.5f, 5.5f, 18.5f, 10.5f)
			.line(19.5f, 19.5f, 6.5f, 6.5f)
			.line(9.5f, 6.5f, 4.5f, 5.5f, 5.5f, 10.5f)
			.build();

	public static final Icon RUN = Icon.of("run")
			.dot(15.5f, 4.6f, 2f)
			.line(14.5f, 8.5f, 10.3f, 13f, 13.5f, 16.2f, 12.6f, 20.8f)
			.line(10.3f, 13f, 6f, 16.6f, 6.4f, 20.4f)
			.line(14.5f, 9.2f, 19f, 11.6f)
			.build();

	public static final Icon EYE = Icon.of("eye")
			.curve(3, 12, 12, 3.5f, 21, 12)
			.curve(3, 12, 12, 20.5f, 21, 12)
			.ring(12, 12, 3.2f)
			.build();

	public static final Icon PERSON = Icon.of("person")
			.ring(12, 7.6f, 3.6f)
			.curve(4.5f, 20.5f, 12, 12.2f, 19.5f, 20.5f)
			.build();

	public static final Icon CUBE = Icon.of("cube")
			.shape(12, 3, 20, 7.5f, 20, 16.5f, 12, 21, 4, 16.5f, 4, 7.5f)
			.line(12, 21, 12, 12)
			.line(12, 12, 20, 7.5f)
			.line(12, 12, 4, 7.5f)
			.build();

	public static final Icon DOTS = Icon.of("dots")
			.dot(5.6f, 12, 1.7f).dot(12, 12, 1.7f).dot(18.4f, 12, 1.7f)
			.build();

	public static final Icon SEARCH = Icon.of("search")
			.ring(10.5f, 10.5f, 6f)
			.line(15, 15, 20, 20)
			.build();

	public static final Icon PALETTE = Icon.of("palette")
			.ring(12, 12, 8.5f)
			.dot(9, 9, 1.5f).dot(15.2f, 10.2f, 1.5f).dot(12, 15.6f, 1.5f)
			.build();

	public static final Icon ZOOM = Icon.of("zoom")
			.ring(10.5f, 10.5f, 6f)
			.line(15, 15, 20, 20)
			.line(7.6f, 10.5f, 13.4f, 10.5f)
			.line(10.5f, 7.6f, 10.5f, 13.4f)
			.build();

	public static final Icon ARROW_UP = Icon.of("arrow_up")
			.line(12, 20.5f, 12, 6.5f)
			.line(6.3f, 12.2f, 12, 6.5f, 17.7f, 12.2f)
			.build();

	public static final Icon ARROW_DOWN = Icon.of("arrow_down")
			.line(12, 3.5f, 12, 17.5f)
			.line(6.3f, 11.8f, 12, 17.5f, 17.7f, 11.8f)
			.build();

	public static final Icon CAMERA = Icon.of("camera")
			.shape(3, 7.5f, 15.5f, 7.5f, 15.5f, 18, 3, 18)
			.shape(15.5f, 11, 21, 8, 21, 17.5f, 15.5f, 14.5f)
			.build();

	public static final Icon LAYOUT = Icon.of("layout")
			.shape(3.5f, 5, 20.5f, 5, 20.5f, 19, 3.5f, 19)
			.line(3.5f, 10, 20.5f, 10)
			.line(9.5f, 10, 9.5f, 19)
			.build();

	public static final Icon WAVE = Icon.of("wave")
			.line(3, 15.5f, 6, 10.5f, 9, 15.5f, 12, 10.5f, 15, 15.5f, 18, 10.5f, 21, 15.5f)
			.build();

	private Icons() {
	}
}
