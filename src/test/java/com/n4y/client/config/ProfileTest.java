package com.n4y.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;
import com.n4y.client.module.Module;

class ProfileTest {
	@Test
	void fileNamesAreSafe() {
		assertEquals("grabacion", Profile.fileNameOf("Grabación"));
		assertEquals("mi_perfil", Profile.fileNameOf("Mi Perfil"));
		assertEquals("profile", Profile.fileNameOf("***"));
		assertEquals("etcpasswd", Profile.fileNameOf("../../etc/passwd"));
	}

	@Test
	void nameValidation() {
		assertTrue(Profile.isValidName("PvP"));
		assertTrue(Profile.isValidName("Grabación 2"));
		assertFalse(Profile.isValidName(" "));
		assertFalse(Profile.isValidName("***"));
		assertFalse(Profile.isValidName("x".repeat(Profile.MAX_NAME_LENGTH + 1)));
	}

	@Test
	void codecRoundTrip() {
		Profile profile = new Profile("PvP", "pink", Map.of("zoom", new ModuleState(true, 86)));
		var json = Profile.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow();
		Profile back = Profile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(profile, back);
		assertEquals(86, back.state("zoom").key());
		assertEquals(Module.UNBOUND, back.state("missing").key());
		assertFalse(back.state("missing").enabled());
	}
}
