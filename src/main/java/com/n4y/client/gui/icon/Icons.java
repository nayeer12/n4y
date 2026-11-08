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

	public static final Icon FOLDER = Icon.of("folder")
			.line(3, 18.5f, 3, 6, 9.5f, 6, 11.5f, 8.8f, 21, 8.8f, 21, 18.5f, 3, 18.5f)
			.build();

	public static final Icon CLOSE = Icon.of("close")
			.line(7, 7, 17, 17)
			.line(17, 7, 7, 17)
			.build();

	public static final Icon KEY = Icon.of("key")
			.ring(8, 12, 4.2f)
			.line(12, 12, 20.5f, 12)
			.line(17.5f, 12, 17.5f, 15.5f)
			.build();

	public static final Icon SUN = Icon.of("sun")
			.ring(12, 12, 4.6f)
			.line(12, 2.5f, 12, 5.5f).line(12, 18.5f, 12, 21.5f)
			.line(2.5f, 12, 5.5f, 12).line(18.5f, 12, 21.5f, 12)
			.line(5.3f, 5.3f, 7.4f, 7.4f).line(16.6f, 16.6f, 18.7f, 18.7f)
			.line(18.7f, 5.3f, 16.6f, 7.4f).line(7.4f, 16.6f, 5.3f, 18.7f)
			.build();

	public static final Icon HEART = Icon.of("heart")
			.curve(12, 20, 5.5f, 14.8f, 3.6f, 10.4f)
			.curve(3.6f, 10.4f, 2.6f, 6.2f, 7.4f, 5.6f)
			.curve(7.4f, 5.6f, 10.4f, 5.2f, 12, 8.4f)
			.curve(12, 8.4f, 13.6f, 5.2f, 16.6f, 5.6f)
			.curve(16.6f, 5.6f, 21.4f, 6.2f, 20.4f, 10.4f)
			.curve(20.4f, 10.4f, 18.5f, 14.8f, 12, 20)
			.build();

	public static final Icon HAND = Icon.of("hand")
			.shape(8, 20.5f, 17.5f, 20.5f, 17.5f, 11.5f, 8, 11.5f)
			.line(10.4f, 11.5f, 10.4f, 7f)
			.line(12.75f, 11.5f, 12.75f, 5.5f)
			.line(15.1f, 11.5f, 15.1f, 7f)
			.build();

	public static final Icon FLAME = Icon.of("flame")
			.curve(12, 21, 4.5f, 16.5f, 10, 10.5f)
			.curve(10, 10.5f, 11, 13, 12.5f, 13.2f)
			.curve(12.5f, 13.2f, 13.5f, 9, 12, 3)
			.curve(12, 3, 19.5f, 9, 18, 15)
			.curve(18, 15, 17, 19.5f, 12, 21)
			.build();

	public static final Icon CLOCK = Icon.of("clock")
			.ring(12, 12, 8f)
			.line(12, 6.5f, 12, 12, 15.8f, 13.8f)
			.build();

	public static final Icon CLOUD = Icon.of("cloud")
			.curve(7, 17.5f, 2.5f, 17.5f, 3.6f, 13.2f)
			.curve(3.6f, 13.2f, 4.6f, 10.6f, 7.6f, 10.9f)
			.curve(7.6f, 10.9f, 8.4f, 6, 13.4f, 6.5f)
			.curve(13.4f, 6.5f, 17, 7, 17.2f, 10.9f)
			.curve(17.2f, 10.9f, 21.4f, 11.1f, 20.9f, 14.5f)
			.curve(20.9f, 14.5f, 20.4f, 17.5f, 17, 17.5f)
			.line(7, 17.5f, 17, 17.5f)
			.build();

	public static final Icon CROSSHAIR = Icon.of("crosshair")
			.ring(12, 12, 7f)
			.line(12, 2.5f, 12, 6f).line(12, 18f, 12, 21.5f)
			.line(2.5f, 12, 6f, 12).line(18f, 12, 21.5f, 12)
			.dot(12, 12, 1.4f)
			.build();

	public static final Icon CHART = Icon.of("chart")
			.line(4, 4.5f, 4, 20, 20.5f, 20)
			.line(6.5f, 15.5f, 10.5f, 10.5f, 14, 13.5f, 19.5f, 6.5f)
			.build();

	public static final Icon PIN = Icon.of("pin")
			.ring(12, 9.4f, 3.4f)
			.line(9.4f, 11.9f, 12, 20.5f)
			.line(14.6f, 11.9f, 12, 20.5f)
			.build();

	public static final Icon FILM = Icon.of("film")
			.shape(3.5f, 5.5f, 20.5f, 5.5f, 20.5f, 18.5f, 3.5f, 18.5f)
			.line(8, 5.5f, 8, 18.5f)
			.line(16, 5.5f, 16, 18.5f)
			.line(3.5f, 12, 20.5f, 12)
			.build();

	public static final Icon CHEVRON_RIGHT = Icon.of("chevron_right")
			.line(9.5f, 5.5f, 16, 12, 9.5f, 18.5f)
			.build();

	public static final Icon ARROW_LEFT = Icon.of("arrow_left")
			.line(20.5f, 12, 4.5f, 12)
			.line(11f, 5.5f, 4.5f, 12, 11f, 18.5f)
			.build();

	private Icons() {
	}
}
