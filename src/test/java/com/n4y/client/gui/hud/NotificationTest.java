package com.n4y.client.gui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.n4y.client.gui.icon.Icons;

import net.minecraft.network.chat.Component;

class NotificationTest {
	@Test
	void agesAndExpires() {
		long born = 1_000_000_000L;
		Notification notification = new Notification(Icons.CUBE, Component.literal("a"), Component.empty(), true, born);
		assertEquals(0f, notification.age(born), 1e-3);
		assertEquals(500f, notification.age(born + 500_000_000L), 1e-3);
		assertFalse(notification.expired(born + 1_000_000_000L));
		assertTrue(notification.expired(born + (Notification.LIFETIME + 100) * 1_000_000L));
	}

	@Test
	void theQueueDoesNotGrowWithoutBound() {
		NotificationCenter center = new NotificationCenter();
		for (int i = 0; i < 50; i++) {
			center.push(Icons.CUBE, Component.literal("n" + i), Component.empty(), false);
		}
		assertTrue(center.size() <= 8, "the queue kept every message, got " + center.size());
		center.clear();
		assertEquals(0, center.size());
	}
}
