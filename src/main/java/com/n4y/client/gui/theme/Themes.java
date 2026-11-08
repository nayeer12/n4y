package com.n4y.client.gui.theme;

import java.util.List;
import java.util.Optional;

/**
 * The four built-in Glass themes. Purple is the default identity of n4y.
 * Values were picked from the reference design: dark tinted glass, bright accent, white text.
 */
public final class Themes {
	public static final Theme PURPLE = new Theme("purple", "Púrpura",
			0xFF8B6CFF, 0x668B6CFF,
			0x8C1A1238, 0xB30C0A1F,
			0x9E2B2357, 0xB01A1640,
			0x5A3A3270, 0x7A4A4290, 0x8C7F6BE0,
			0x66FFFFFF, 0x1AFFFFFF,
			0xFFFFFFFF, 0xC4D9D3FF, 0x8CC7BFF5, 0x55FFFFFF);

	public static final Theme BLUE = new Theme("blue", "Azul",
			0xFF3EA6FF, 0x663EA6FF,
			0x8C0E1A38, 0xB3070F22,
			0x9E1B2D57, 0xB0111F42,
			0x5A243F72, 0x7A2F5192, 0x8C3B86E0,
			0x66FFFFFF, 0x1AFFFFFF,
			0xFFFFFFFF, 0xC4CFE3FF, 0x8CB5CBF0, 0x55FFFFFF);

	public static final Theme PINK = new Theme("pink", "Rosa",
			0xFFF25CD6, 0x66F25CD6,
			0x8C2A1033, 0xB314081C,
			0x9E44204F, 0xB02E163A,
			0x5A5A2C66, 0x7A743885, 0x8CC24FB4,
			0x66FFFFFF, 0x1AFFFFFF,
			0xFFFFFFFF, 0xC4F3D3F0, 0x8CE5BCE0, 0x55FFFFFF);

	public static final Theme GREEN = new Theme("green", "Verde",
			0xFF8EE84A, 0x668EE84A,
			0x8C102414, 0xB307140A,
			0x9E1F3D22, 0xB0142A17,
			0x5A2C5530, 0x7A3A6E3E, 0x8C5FB33A,
			0x66FFFFFF, 0x1AFFFFFF,
			0xFFFFFFFF, 0xC4D6F2CF, 0x8CBEDDB5, 0x55FFFFFF);

	public static final List<Theme> ALL = List.of(PURPLE, BLUE, PINK, GREEN);

	private Themes() {
	}

	public static Optional<Theme> byId(String id) {
		return ALL.stream().filter(t -> t.id().equals(id)).findFirst();
	}
}
