package com.n4y.client.core.event;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small, reflection-free event bus.
 *
 * <p>Listeners are registered per exact event class and called in {@link Priority} order
 * (registration order inside the same priority). A listener that throws is logged and skipped,
 * so one broken module can never take the client down.
 *
 * <p>The listeners of an event type are held in an immutable array that is replaced as a whole,
 * never edited in place. That matters twice over. Subscribing from the client thread can no
 * longer be seen half-done by a {@link #post} on the render thread, which could previously read
 * the list in the window between {@code clear()} and {@code addAll()} and silently drop an
 * event. And posting reads one volatile reference and walks it by index, so a per-frame event
 * allocates nothing at all, not even an iterator.
 */
public final class EventBus {
	private static final Logger LOGGER = LoggerFactory.getLogger("n4y/EventBus");
	private static final Entry<?>[] NONE = new Entry<?>[0];
	private static final Comparator<Entry<?>> ORDER =
			Comparator.<Entry<?>>comparingInt(entry -> entry.priority().ordinal())
					.thenComparingLong(Entry::order);

	private final Map<Class<? extends Event>, Listeners> listeners = new ConcurrentHashMap<>();

	/** Handle returned by {@link #subscribe}; call {@link #close()} to unsubscribe. */
	public interface Subscription extends AutoCloseable {
		@Override
		void close();
	}

	private record Entry<E extends Event>(Consumer<E> handler, Priority priority, long order) {
	}

	/** Per-event-type holder. The array is published by writing this one volatile field. */
	private static final class Listeners {
		private volatile Entry<?>[] entries = NONE;
	}

	/** Guarded by {@code this}; only ever read while subscribing. */
	private long counter;

	public <E extends Event> Subscription subscribe(Class<E> type, Consumer<E> handler) {
		return subscribe(type, Priority.NORMAL, handler);
	}

	public synchronized <E extends Event> Subscription subscribe(Class<E> type, Priority priority, Consumer<E> handler) {
		Objects.requireNonNull(type, "event type");
		Objects.requireNonNull(handler, "handler");
		Entry<E> entry = new Entry<>(handler, priority == null ? Priority.NORMAL : priority, counter++);
		Listeners holder = listeners.computeIfAbsent(type, ignored -> new Listeners());
		Entry<?>[] current = holder.entries;
		Entry<?>[] next = Arrays.copyOf(current, current.length + 1);
		next[current.length] = entry;
		Arrays.sort(next, ORDER);
		holder.entries = next;
		return () -> unsubscribe(holder, entry);
	}

	/** Removing an entry that is already gone is a no-op, so closing twice is safe. */
	private synchronized void unsubscribe(Listeners holder, Entry<?> entry) {
		Entry<?>[] current = holder.entries;
		int index = -1;
		for (int i = 0; i < current.length; i++) {
			if (current[i] == entry) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return;
		}
		Entry<?>[] next = new Entry<?>[current.length - 1];
		System.arraycopy(current, 0, next, 0, index);
		System.arraycopy(current, index + 1, next, index, current.length - 1 - index);
		holder.entries = next;
	}

	/** Delivers {@code event} to every listener of its exact class and returns it. */
	@SuppressWarnings("unchecked")
	public <E extends Event> E post(E event) {
		Listeners holder = listeners.get(event.getClass());
		if (holder == null) {
			return event;
		}
		Entry<?>[] snapshot = holder.entries;
		for (int i = 0; i < snapshot.length; i++) {
			Entry<E> entry = (Entry<E>) snapshot[i];
			try {
				entry.handler().accept(event);
			} catch (Throwable t) {
				LOGGER.error("Listener for {} failed", event.getClass().getSimpleName(), t);
			}
		}
		return event;
	}

	public int listenerCount(Class<? extends Event> type) {
		Listeners holder = listeners.get(type);
		return holder == null ? 0 : holder.entries.length;
	}
}
