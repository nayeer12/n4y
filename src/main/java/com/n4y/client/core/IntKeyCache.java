package com.n4y.client.core;

import java.util.function.Consumer;

/**
 * Small, fixed-capacity cache from an {@code int} key to a value, written for the render path.
 *
 * <p>A lookup allocates nothing and boxes nothing, which a {@code HashMap<Integer, V>} or a
 * {@code HashMap<String, V>} with a concatenated key cannot promise: those allocate on every
 * single call, hundreds of times per frame. Entries are held in parallel arrays and found with
 * a linear scan, because the capacity is tens of entries, not thousands: the scan stays inside
 * a couple of cache lines and beats hashing at this size.
 *
 * <p>When the cache is full the least recently used entry is evicted and handed to the eviction
 * callback, so the caller can release the GPU texture behind it. Evicting one entry is what
 * makes this better than flushing the whole cache: a burst of new keys (the player changed the
 * GUI scale) costs one regenerated texture instead of every texture, all on the same frame.
 *
 * <p>Not thread safe. Render thread only.
 */
public final class IntKeyCache<V> {
	private final int[] keys;
	private final Object[] values;
	private final long[] used;
	private final int capacity;
	private int size;
	private long clock;
	/** Index of the last hit; repeated lookups of the same key are the common case. */
	private int lastHit = -1;

	public IntKeyCache(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive, got " + capacity);
		}
		this.capacity = capacity;
		this.keys = new int[capacity];
		this.values = new Object[capacity];
		this.used = new long[capacity];
	}

	/** The value stored under {@code key}, or {@code null}. */
	@SuppressWarnings("unchecked")
	public V get(int key) {
		int hit = lastHit;
		if (hit >= 0 && hit < size && keys[hit] == key) {
			used[hit] = ++clock;
			return (V) values[hit];
		}
		for (int i = 0; i < size; i++) {
			if (keys[i] == key) {
				used[i] = ++clock;
				lastHit = i;
				return (V) values[i];
			}
		}
		return null;
	}

	/**
	 * Stores {@code value} under {@code key}, evicting the least recently used entry first if
	 * the cache is full.
	 *
	 * @param onEvict handed the value that was dropped so it can be released, or not called
	 */
	public void put(int key, V value, Consumer<V> onEvict) {
		for (int i = 0; i < size; i++) {
			if (keys[i] == key) {
				values[i] = value;
				used[i] = ++clock;
				lastHit = i;
				return;
			}
		}
		int slot;
		if (size < capacity) {
			slot = size++;
		} else {
			slot = 0;
			for (int i = 1; i < size; i++) {
				if (used[i] < used[slot]) {
					slot = i;
				}
			}
			if (onEvict != null) {
				onEvict.accept(cast(values[slot]));
			}
		}
		keys[slot] = key;
		values[slot] = value;
		used[slot] = ++clock;
		lastHit = slot;
	}

	/** Drops every entry, reporting each value so the caller can release what it owns. */
	public void clear(Consumer<V> onRemove) {
		for (int i = 0; i < size; i++) {
			if (onRemove != null) {
				onRemove.accept(cast(values[i]));
			}
			values[i] = null;
		}
		size = 0;
		lastHit = -1;
	}

	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	private V cast(Object value) {
		return (V) value;
	}
}
