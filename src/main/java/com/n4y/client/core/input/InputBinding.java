package com.n4y.client.core.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import org.lwjgl.glfw.GLFW;

/**
 * A key or a mouse button the player bound to something. n4y keeps its own bindings inside the
 * profile instead of vanilla's options file, so they travel with the profile.
 *
 * @param type where the input comes from
 * @param code GLFW key code, or GLFW mouse button number
 */
public record InputBinding(InputBinding.Type type, int code) {
	/** Nothing bound. */
	public static final InputBinding UNBOUND = new InputBinding(Type.NONE, -1);

	/** Source of an input. */
	public enum Type implements StringRepresentable {
		NONE("none"),
		KEY("key"),
		MOUSE("mouse");

		public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

		private final String id;

		Type(String id) {
			this.id = id;
		}

		@Override
		public String getSerializedName() {
			return id;
		}
	}

	private static final Codec<InputBinding> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Type.CODEC.optionalFieldOf("type").xmap(value -> value.orElse(Type.NONE), java.util.Optional::of).forGetter(InputBinding::type),
			Codec.INT.optionalFieldOf("code").xmap(value -> value.orElse(-1), java.util.Optional::of).forGetter(InputBinding::code)
	).apply(instance, InputBinding::new));

	/**
	 * Reads either the modern object form or a bare key code, which is how n4y stored binds
	 * before mouse buttons were supported, so existing profiles keep working. Always writes the
	 * object form.
	 */
	public static final Codec<InputBinding> CODEC = Codec.either(Codec.INT, RECORD_CODEC).xmap(
			either -> either.map(InputBinding::ofLegacyKeyCode, binding -> binding),
			binding -> com.mojang.datafixers.util.Either.right(binding));

	public static InputBinding key(int code) {
		return code == -1 ? UNBOUND : new InputBinding(Type.KEY, code);
	}

	public static InputBinding mouse(int button) {
		return button < 0 ? UNBOUND : new InputBinding(Type.MOUSE, button);
	}

	/** A bare number is a keyboard key, which is the only thing the old format could store. */
	public static InputBinding ofLegacyKeyCode(int code) {
		return key(code);
	}

	public boolean isBound() {
		return type != Type.NONE && code >= 0;
	}

	/** True while the player is physically holding this key or button down. */
	public boolean isDown(Window window) {
		return switch (type) {
			case KEY -> InputConstants.isKeyDown(window, code);
			case MOUSE -> GLFW.glfwGetMouseButton(window.handle(), code) == GLFW.GLFW_PRESS;
			case NONE -> false;
		};
	}

	/** Name shown on the key chip, e.g. {@code V}, {@code Left Shift} or {@code Right Button}. */
	public Component displayName() {
		return switch (type) {
			case KEY -> InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName();
			case MOUSE -> InputConstants.Type.MOUSE.getOrCreate(code).getDisplayName();
			case NONE -> Component.translatable("n4y.key.none");
		};
	}
}
