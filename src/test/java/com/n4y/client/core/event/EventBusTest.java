package com.n4y.client.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventBusTest {
	record Ping(int value) implements Event {
	}

	static final class Stop extends Cancellable {
	}

	@Test
	void deliversInPriorityThenRegistrationOrder() {
		EventBus bus = new EventBus();
		List<String> order = new ArrayList<>();
		bus.subscribe(Ping.class, Priority.LOW, e -> order.add("low"));
		bus.subscribe(Ping.class, e -> order.add("normal1"));
		bus.subscribe(Ping.class, Priority.HIGHEST, e -> order.add("highest"));
		bus.subscribe(Ping.class, e -> order.add("normal2"));
		bus.post(new Ping(1));
		assertEquals(List.of("highest", "normal1", "normal2", "low"), order);
	}

	@Test
	void unsubscribeAndFailingListenerIsolation() {
		EventBus bus = new EventBus();
		int[] hits = {0};
		bus.subscribe(Ping.class, e -> { throw new IllegalStateException("boom"); });
		EventBus.Subscription sub = bus.subscribe(Ping.class, e -> hits[0]++);
		bus.post(new Ping(1));
		sub.close();
		bus.post(new Ping(2));
		assertEquals(1, hits[0]);
		assertEquals(1, bus.listenerCount(Ping.class));
	}

	@Test
	void cancellation() {
		EventBus bus = new EventBus();
		bus.subscribe(Stop.class, Cancellable::cancel);
		assertTrue(bus.post(new Stop()).isCancelled());
	}
}
