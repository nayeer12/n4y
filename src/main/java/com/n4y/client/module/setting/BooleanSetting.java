package com.n4y.client.module.setting;

import com.mojang.serialization.Codec;

/** An on/off setting. */
public final class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String id, boolean defaultValue) {
		super(id, defaultValue);
	}

	public boolean value() {
		return get();
	}

	public void toggle() {
		set(!get());
	}

	@Override
	public Codec<Boolean> codec() {
		return Codec.BOOL;
	}
}
