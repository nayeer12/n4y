package com.n4y.client.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.n4y.client.module.Module;

/**
 * Stored state of one module inside a {@link Profile}.
 *
 * @param enabled whether it was active
 * @param key     GLFW key code bound to it, or {@link Module#UNBOUND}
 */
public record ModuleState(boolean enabled, int key) {
	public static final ModuleState DEFAULT = new ModuleState(false, Module.UNBOUND);

	public static final Codec<ModuleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", false).forGetter(ModuleState::enabled),
			Codec.INT.optionalFieldOf("key", Module.UNBOUND).forGetter(ModuleState::key)
	).apply(instance, ModuleState::new));

	public static ModuleState of(Module module) {
		return new ModuleState(module.isEnabled(), module.key());
	}
}
