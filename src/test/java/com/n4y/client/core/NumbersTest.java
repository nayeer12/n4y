package com.n4y.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumbersTest {
	private static String fixed1(double value) {
		StringBuilder out = new StringBuilder();
		Numbers.appendFixed1(out, value);
		return out.toString();
	}

	private static String clock(long seconds) {
		StringBuilder out = new StringBuilder();
		Numbers.appendClock(out, seconds);
		return out.toString();
	}

	@Test
	void oneDecimalMatchesStringFormat() {
		for (double value : new double[] {0d, 1d, 12.34d, 12.35d, 0.04d, 99.99d, 1234.56d}) {
			assertEquals(String.format("%.1f", value), fixed1(value), "value " + value);
		}
	}

	@Test
	void oneDecimalKeepsTheSignOfNegativeValues() {
		assertEquals("-12.3", fixed1(-12.34d));
		assertEquals("-0.1", fixed1(-0.05d));
		// Values that round to zero must not come out as "-0.0".
		assertEquals("0.0", fixed1(-0.01d));
		assertEquals("0.0", fixed1(-0d));
	}

	@Test
	void clockPadsMinutesAndSeconds() {
		assertEquals("0:00:00", clock(0));
		assertEquals("0:00:09", clock(9));
		assertEquals("0:01:00", clock(60));
		assertEquals("1:00:00", clock(3600));
		assertEquals("2:03:04", clock(2 * 3600 + 3 * 60 + 4));
		assertEquals("100:00:01", clock(100 * 3600 + 1));
	}

	@Test
	void clockTreatsNegativeTimeAsZero() {
		assertEquals("0:00:00", clock(-5));
	}
}
