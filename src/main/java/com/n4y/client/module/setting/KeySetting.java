package com.n4y.client.module.setting;

import com.mojang.serialization.Codec;
import com.n4y.client.core.input.InputBinding;

/**
 * A key or mouse button the module reads itself, such as AutoCrystal's activate key, which is
 * held down rather than toggling anything.
 */
public final class KeySetting extends Setting<InputBinding> {
	public KeySetting(String id, InputBinding defaultValue) {
		super(id, defaultValue);
	}

	public InputBinding binding() {
		return get();
	}

	public boolean isBound() {
		return get().isBound();
	}

	@Override
	public Codec<InputBinding> codec() {
		return InputBinding.CODEC;
	}
}
