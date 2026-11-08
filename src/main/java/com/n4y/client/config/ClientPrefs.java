package com.n4y.client.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The small root file, {@code config/n4y/client.json}: everything that is not part of a
 * profile. For now that is only which profile to load at start-up.
 *
 * @param activeProfile file name of the profile to load
 */
public record ClientPrefs(String activeProfile) {
	public static final ClientPrefs DEFAULT = new ClientPrefs("default");

	/** Optional when reading so a missing file still loads, but always written. */
	public static final Codec<ClientPrefs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("activeProfile")
					.xmap(value -> value.orElse("default"), java.util.Optional::of)
					.forGetter(ClientPrefs::activeProfile)
	).apply(instance, ClientPrefs::new));
}
