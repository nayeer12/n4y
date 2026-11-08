package com.n4y.client.module.setting;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.n4y.client.N4y;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * One configurable value of a module. Settings keep their value in a field, so a module can
 * read them every tick without a lookup, and serialise themselves into the active profile.
 *
 * @param <T> type of the stored value
 */
public abstract class Setting<T> {
	private final String id;
	private final T defaultValue;
	private T value;
	private Runnable changeListener = () -> { };

	/** Subclasses pass a default that is already valid for their own bounds. */
	protected Setting(String id, T defaultValue) {
		this.id = id;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public final String id() {
		return id;
	}

	/**
	 * The translated name, or a readable version of the id when no translation exists, so a
	 * missing language entry shows "Place Delay" instead of a raw key.
	 */
	public Component displayName() {
		String key = "n4y.setting." + id;
		return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(humanise(id));
	}

	private static String humanise(String id) {
		String[] words = id.split("_");
		StringBuilder sb = new StringBuilder(id.length());
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return sb.toString();
	}

	/** Settings only carry a description when the language file defines one. */
	public final boolean hasDescription() {
		return Language.getInstance().has("n4y.setting." + id + ".desc");
	}

	public final Component description() {
		return Component.translatable("n4y.setting." + id + ".desc");
	}

	public final T get() {
		return value;
	}

	public final T defaultValue() {
		return defaultValue;
	}

	/** Stores a new value after clamping it; notifies only when it really changed. */
	public final void set(T newValue) {
		T clean = sanitise(newValue);
		if (clean.equals(value)) {
			return;
		}
		value = clean;
		changeListener.run();
	}

	public final boolean isDefault() {
		return value.equals(defaultValue);
	}

	public final void reset() {
		set(defaultValue);
	}

	/** Set by the owning module so the configuration can be saved when a value changes. */
	public final void onChange(Runnable listener) {
		this.changeListener = listener;
	}

	/** Brings a value inside whatever bounds the concrete setting has. */
	protected T sanitise(T raw) {
		return raw;
	}

	public abstract Codec<T> codec();

	/** The value as it goes into the profile file. */
	public final JsonElement save() {
		return codec().encodeStart(JsonOps.INSTANCE, value)
				.resultOrPartial(error -> N4y.LOGGER.error("Could not save setting '{}': {}", id, error))
				.orElseGet(com.google.gson.JsonObject::new);
	}

	/** Reads a stored value; a broken one leaves the default in place. */
	public final void load(JsonElement json) {
		codec().parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> N4y.LOGGER.warn("Ignoring invalid value for setting '{}': {}", id, error))
				.ifPresent(this::set);
	}
}
