package com.n4y.client.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.hud.HudModule;
import com.n4y.client.gui.hud.HudPlacement;
import com.n4y.client.module.Module;
import com.n4y.client.module.setting.Setting;

import net.minecraft.util.ExtraCodecs;

/**
 * Stored state of one module inside a {@link Profile}.
 *
 * @param enabled  whether it was active
 * @param key      key or mouse button bound to it
 * @param hud      where an on-screen element was placed, when it was moved from its default
 * @param settings values of the settings the player changed, by setting id; each one is
 *                 stored in its own shape so the file stays readable and editable by hand
 */
public record ModuleState(boolean enabled, InputBinding key, Optional<HudPlacement> hud, Map<String, JsonElement> settings) {
	public static final ModuleState DEFAULT =
			new ModuleState(false, InputBinding.UNBOUND, Optional.empty(), Map.of());

	public static final Codec<ModuleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", false).forGetter(ModuleState::enabled),
			InputBinding.CODEC.optionalFieldOf("key", InputBinding.UNBOUND).forGetter(ModuleState::key),
			HudPlacement.CODEC.optionalFieldOf("hud").forGetter(ModuleState::hud),
			Codec.unboundedMap(Codec.STRING, ExtraCodecs.JSON).optionalFieldOf("settings", Map.of()).forGetter(ModuleState::settings)
	).apply(instance, ModuleState::new));

	public ModuleState {
		settings = Map.copyOf(settings);
	}

	/** Captures a module, leaving out every setting still at its default value. */
	public static ModuleState of(Module module) {
		Optional<HudPlacement> placement = module instanceof HudModule hud && hud.isMoved()
				? Optional.of(hud.placement())
				: Optional.empty();
		Map<String, JsonElement> values = new LinkedHashMap<>();
		for (Setting<?> setting : module.settings()) {
			if (!setting.isDefault()) {
				values.put(setting.id(), setting.save());
			}
		}
		return new ModuleState(module.isEnabled(), module.key(), placement, values);
	}
}
