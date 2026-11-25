package com.n4y.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IntKeyCacheTest {
	@Test
	void storesAndFindsByKey() {
		IntKeyCache<String> cache = new IntKeyCache<>(4);
		cache.put(7, "seven", null);
		cache.put(4096, "big", null);
		assertEquals("seven", cache.get(7));
		assertEquals("big", cache.get(4096));
		assertNull(cache.get(8));
		assertEquals(2, cache.size());
	}

	@Test
	void repeatedLookupsOfTheSameKeyStayCorrect() {
		IntKeyCache<String> cache = new IntKeyCache<>(4);
		cache.put(1, "a", null);
		cache.put(2, "b", null);
		for (int i = 0; i < 5; i++) {
			assertEquals("a", cache.get(1));
			assertEquals("b", cache.get(2));
		}
	}

	@Test
	void replacingAKeyDoesNotGrowTheCache() {
		IntKeyCache<String> cache = new IntKeyCache<>(4);
		cache.put(3, "first", null);
		cache.put(3, "second", null);
		assertEquals("second", cache.get(3));
		assertEquals(1, cache.size());
	}

	@Test
	void evictsOnlyTheLeastRecentlyUsedEntry() {
		List<String> dropped = new ArrayList<>();
		IntKeyCache<String> cache = new IntKeyCache<>(3);
		cache.put(1, "a", dropped::add);
		cache.put(2, "b", dropped::add);
		cache.put(3, "c", dropped::add);
		// Touch 1 and 3, leaving 2 as the oldest use.
		assertEquals("a", cache.get(1));
		assertEquals("c", cache.get(3));

		cache.put(4, "d", dropped::add);

		assertEquals(List.of("b"), dropped, "a full cache must drop one entry, not all of them");
		assertEquals(3, cache.size());
		assertNull(cache.get(2));
		assertEquals("a", cache.get(1));
		assertEquals("c", cache.get(3));
		assertEquals("d", cache.get(4));
	}

	@Test
	void clearReportsEverythingItDrops() {
		List<String> dropped = new ArrayList<>();
		IntKeyCache<String> cache = new IntKeyCache<>(2);
		cache.put(1, "a", null);
		cache.put(2, "b", null);
		cache.clear(dropped::add);
		assertEquals(2, dropped.size());
		assertEquals(0, cache.size());
		assertNull(cache.get(1));
	}

	@Test
	void capacityMustBePositive() {
		assertThrows(IllegalArgumentException.class, () -> new IntKeyCache<>(0));
	}
}
