package com.n4y.client.core;

import java.util.Locale;

/** Text helpers shared by the search box and the profile names. */
public final class Strings {
	private Strings() {
	}

	/**
	 * Lower cases and replaces accented letters with their plain form, so "Cámara" matches
	 * "camara" when searching and becomes {@code camara} in a file name.
	 */
	public static String normalise(String text) {
		String lower = text.toLowerCase(Locale.ROOT).strip();
		StringBuilder sb = new StringBuilder(lower.length());
		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			sb.append(switch (c) {
				case 'á', 'à', 'ä', 'â', 'ã', 'å' -> 'a';
				case 'é', 'è', 'ë', 'ê' -> 'e';
				case 'í', 'ì', 'ï', 'î' -> 'i';
				case 'ó', 'ò', 'ö', 'ô', 'õ' -> 'o';
				case 'ú', 'ù', 'ü', 'û' -> 'u';
				case 'ñ' -> 'n';
				case 'ç' -> 'c';
				default -> c;
			});
		}
		return sb.toString();
	}
}
