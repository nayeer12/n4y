package com.n4y.client.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;

class InputBindingTest {
	@Test
	void unboundAndBound() {
		assertFalse(InputBinding.UNBOUND.isBound());
		assertFalse(InputBinding.key(-1).isBound());
		assertTrue(InputBinding.key(86).isBound());
		assertTrue(InputBinding.mouse(1).isBound());
		assertFalse(InputBinding.mouse(-1).isBound());
	}

	@Test
	void roundTripKeepsTypeAndCode() {
		for (InputBinding binding : new InputBinding[] {InputBinding.UNBOUND, InputBinding.key(86), InputBinding.mouse(1)}) {
			var json = InputBinding.CODEC.encodeStart(JsonOps.INSTANCE, binding).getOrThrow();
			assertEquals(binding, InputBinding.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		}
	}

	@Test
	void theOldBareNumberFormatStillLoads() {
		InputBinding legacy = InputBinding.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(86)).getOrThrow();
		assertEquals(InputBinding.key(86), legacy, "a profile saved before mouse support stopped working");
		InputBinding none = InputBinding.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(-1)).getOrThrow();
		assertEquals(InputBinding.UNBOUND, none);
	}

	@Test
	void writesTheModernShape() {
		var json = InputBinding.CODEC.encodeStart(JsonOps.INSTANCE, InputBinding.mouse(1)).getOrThrow();
		assertTrue(json.isJsonObject(), "bindings are written as an object, not a bare number");
		assertEquals("mouse", json.getAsJsonObject().get("type").getAsString());
		assertEquals(1, json.getAsJsonObject().get("code").getAsInt());
	}
}
