package com.n4y.client.core;

/**
 * Number formatting that appends into a buffer the caller owns.
 *
 * <p>{@code String.format} is the readable way to build these strings and the wrong one for text
 * that is rebuilt while the game draws: every call parses the format string and allocates a
 * {@code Formatter}, a {@code StringBuilder} and the result. These write into a reusable buffer
 * instead, so a value that has not changed costs nothing at all.
 */
public final class Numbers {
	private Numbers() {
	}

	/** Appends {@code value} with one decimal place, rounded half-up away from zero. */
	public static void appendFixed1(StringBuilder out, double value) {
		long scaled = Math.round(Math.abs(value) * 10d);
		if (value < 0 && scaled != 0) {
			out.append('-');
		}
		out.append(scaled / 10L).append('.').append((char) ('0' + (int) (scaled % 10L)));
	}

	/** Appends {@code seconds} as {@code h:mm:ss}. Negative input is treated as zero. */
	public static void appendClock(StringBuilder out, long seconds) {
		long total = Math.max(0L, seconds);
		out.append(total / 3600L).append(':');
		appendTwoDigits(out, total % 3600L / 60L);
		out.append(':');
		appendTwoDigits(out, total % 60L);
	}

	private static void appendTwoDigits(StringBuilder out, long value) {
		if (value < 10L) {
			out.append('0');
		}
		out.append(value);
	}
}
