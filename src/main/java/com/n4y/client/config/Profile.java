package com.n4y.client.config;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.n4y.client.core.Strings;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A named, self-contained configuration: the theme plus the state of every module.
 * Switching profile swaps the whole look and feel of the client at once.
 *
 * @param name    name as the user typed it
 * @param theme   id of the Glass theme
 * @param modules state per module id; modules missing from the map keep their defaults
 */
public record Profile(String name, String theme, Map<String, ModuleState> modules) {
	public static final String DEFAULT_NAME = "Default";
	public static final int MAX_NAME_LENGTH = 24;

	/**
	 * Fields are optional when reading, so an old or hand-edited file still loads, but they are
	 * always written: a profile on disk should be readable and editable without guessing.
	 */
	public static final Codec<Profile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").xmap(value -> value.orElse(DEFAULT_NAME), Optional::of).forGetter(Profile::name),
			Codec.STRING.optionalFieldOf("theme").xmap(value -> value.orElse("purple"), Optional::of).forGetter(Profile::theme),
			Codec.unboundedMap(Codec.STRING, ModuleState.CODEC).optionalFieldOf("modules")
					.xmap(value -> value.orElse(Map.of()), Optional::of).forGetter(Profile::modules)
	).apply(instance, Profile::new));

	public Profile {
		modules = Map.copyOf(modules);
	}

	public static Profile empty(String name) {
		return new Profile(name, "purple", Map.of());
	}

	public ModuleState state(String moduleId) {
		return modules.getOrDefault(moduleId, ModuleState.DEFAULT);
	}

	/**
	 * File name this profile is stored under: lower case, spaces as underscores and nothing
	 * that could escape the profiles folder.
	 */
	public String fileName() {
		return fileNameOf(name);
	}

	public static String fileNameOf(String name) {
		String sanitised = sanitise(name);
		return sanitised.isEmpty() ? "profile" : sanitised;
	}

	/**
	 * Keeps only characters that are safe in a file name, so a name can never point outside
	 * the profiles folder. Returns an empty string when nothing usable is left.
	 */
	private static String sanitise(String name) {
		String normalised = Strings.normalise(name == null ? "" : name);
		StringBuilder sb = new StringBuilder(normalised.length());
		boolean hasLetterOrDigit = false;
		for (int i = 0; i < normalised.length() && sb.length() < MAX_NAME_LENGTH; i++) {
			char c = normalised.charAt(i);
			if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
				sb.append(c);
				hasLetterOrDigit = true;
			} else if (c == '-') {
				sb.append(c);
			} else if (c == ' ' || c == '_') {
				sb.append('_');
			}
		}
		return hasLetterOrDigit ? sb.toString() : "";
	}

	/**
	 * A name is usable when it is not blank, fits the length limit and still contains at least
	 * one letter, digit or hyphen once sanitised, so two different names cannot collide on an
	 * empty file name.
	 */
	public static boolean isValidName(String name) {
		String trimmed = name == null ? "" : name.strip();
		return !trimmed.isEmpty() && trimmed.length() <= MAX_NAME_LENGTH && !sanitise(trimmed).isEmpty();
	}
}
