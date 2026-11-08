package com.n4y.client.core.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small, reflection-free event bus.
 *
 * <p>Listeners are registered per exact event class and called in {@link Priority} order
 * (registration order inside the same priority). A listener that throws is logged and
 * skipped so one broken module can never take the client down. Posting allocates nothing
 * when an event has no listeners, which keeps per-frame events cheap.
 */
public final class EventBus {
	private static final Logger LOGGER = LoggerFactory.getLogger("n4y/EventBus");

	private final Map<Class<? extends Event>, CopyOnWriteArrayList<Entry<?>>> listeners = new ConcurrentHashMap<>();

	/** Handle returned by {@link #subscribe}; call {@link #close()} to unsubscribe. */
	public interface Subscription extends AutoCloseable {
		@Override
		void close();
	}

	private record Entry<E extends Event>(Consumer<E> handler, Priority priority, long order) {
	}

	private long counter;

	public <E extends Event> Subscription subscribe(Class<E> type, Consumer<E> handler) {
		return subscribe(type, Priority.NORMAL, handler);
	}

	public synchronized <E extends Event> Subscription subscribe(Class<E> type, Priority priority, Consumer<E> handler) {
		Entry<E> entry = new Entry<>(handler, priority, counter++);
		CopyOnWriteArrayList<Entry<?>> list = listeners.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
		List<Entry<?>> sorted = new ArrayList<>(list);
		sorted.add(entry);
		sorted.sort(Comparator.<Entry<?>, Integer>comparing(e -> e.priority().ordinal()).thenComparingLong(Entry::order));
		list.clear();
		list.addAll(sorted);
		return () -> list.remove(entry);
	}

	/** Delivers {@code event} to every listener of its exact class and returns it. */
	@SuppressWarnings("unchecked")
	public <E extends Event> E post(E event) {
		CopyOnWriteArrayList<Entry<?>> list = listeners.get(event.getClass());
		if (list == null || list.isEmpty()) {
			return event;
		}
		for (Entry<?> raw : list) {
			Entry<E> entry = (Entry<E>) raw;
			try {
				entry.handler().accept(event);
			} catch (Throwable t) {
				LOGGER.error("Listener for {} failed", event.getClass().getSimpleName(), t);
			}
		}
		return event;
	}

	public int listenerCount(Class<? extends Event> type) {
		List<Entry<?>> list = listeners.get(type);
		return list == null ? 0 : list.size();
	}
}
