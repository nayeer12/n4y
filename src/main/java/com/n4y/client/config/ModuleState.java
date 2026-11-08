package com.n4y.client.config;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.module.Module;

/**
 * Stored state of one module inside a {@link Profile}.
 *
 * @param enabled whether it was active
 * @param key     GLFW key code bound to it, or {@link Module#UNBOUND}
 * @param hud     where an on-screen element was placed, when it was moved from its default
 */
public record ModuleState(boolean enabled, int key, Optional<HudPlacement> hud) {
	public static final ModuleState DEFAULT = new ModuleState(false, Module.UNBOUND, Optional.empty());

	public static final Codec<ModuleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", false).forGetter(ModuleState::enabled),
			Codec.INT.optionalFieldOf("key", Module.UNBOUND).forGetter(ModuleState::key),
			HudPlacement.CODEC.optionalFieldOf("hud").forGetter(ModuleState::hud)
	).apply(instance, ModuleState::new));

	public static ModuleState of(Module module) {
		Optional<HudPlacement> placement = module instanceof HudModule hud && hud.isMoved()
				? Optional.of(hud.placement())
				: Optional.empty();
		return new ModuleState(module.isEnabled(), module.key(), placement);
	}
}
