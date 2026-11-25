package com.n4y.client.gui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.n4y.client.gui.icon.Icons;

import net.minecraft.network.chat.Component;

class NotificationExpiryTest {
	private static final long MILLIS = 1_000_000L;

	private static Notification at(long bornAtMillis, String name) {
		return new Notification(Icons.CUBE, Component.literal(name), Component.empty(), false, bornAtMillis * MILLIS);
	}

	@Test
	void dropsTheToastsThatRanOutOfTimeAndKeepsTheRest() {
		NotificationCenter center = new NotificationCenter();
		center.push(at(0L, "oldest"));
		center.push(at(1_000L, "middle"));
		center.push(at(2_000L, "newest"));

		// At 2.7s the first is past its 2.6s lifetime and the other two are not.
		center.dropExpired(2_700L * MILLIS);
		assertEquals(2, center.size());

		center.dropExpired(3_700L * MILLIS);
		assertEquals(1, center.size());

		center.dropExpired(10_000L * MILLIS);
		assertEquals(0, center.size());
	}

	@Test
	void nothingExpiresBeforeItsTime() {
		NotificationCenter center = new NotificationCenter();
		center.push(at(0L, "a"));
		center.push(at(0L, "b"));
		center.dropExpired((Notification.LIFETIME - 1) * MILLIS);
		assertEquals(2, center.size());
	}

	@Test
	void anEmptyQueueIsFine() {
		NotificationCenter center = new NotificationCenter();
		center.dropExpired(System.nanoTime());
		assertEquals(0, center.size());
	}
}
