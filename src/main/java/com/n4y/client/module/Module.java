package com.n4y.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.n4y.client.N4y;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icon;
import com.n4y.client.module.setting.Setting;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * A single client feature. Subclasses only implement behaviour: enabling, disabling and the
 * per-tick work. Rendering and layout live in the GUI layer, never here.
 *
 * <p>A module that changes a vanilla option must restore it in {@link #onDisable}, because the
 * manager disables everything on shutdown so the user's settings survive.
 */
public abstract class Module {
	private final String id;
	private final ModuleCategory category;
	private final Icon icon;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private InputBinding key = InputBinding.UNBOUND;
	private Runnable changeListener = () -> { };
	private java.util.function.Consumer<Module> stateListener = module -> { };

	protected Module(String id, ModuleCategory category, Icon icon) {
		this.id = id;
		this.category = category;
		this.icon = icon;
	}

	public final String id() {
		return id;
	}

	public final ModuleCategory category() {
		return category;
	}

	public final Icon icon() {
		return icon;
	}

	public Component displayName() {
		return Component.translatable("n4y.module." + id);
	}

	public Component description() {
		return Component.translatable("n4y.module." + id + ".desc");
	}

	public final boolean isEnabled() {
		return enabled;
	}

	/** Key or mouse button that switches this module on and off. */
	public final InputBinding key() {
		return key;
	}

	public final void setKey(InputBinding value) {
		InputBinding safe = value == null ? InputBinding.UNBOUND : value;
		if (key.equals(safe)) {
			return;
		}
		key = safe;
		changeListener.run();
	}

	public final boolean hasKey() {
		return key.isBound();
	}

	/**
	 * Registers this module's settings, in the order the interface shows them. Call it from
	 * the constructor; every setting reports its changes so the profile is saved.
	 */
	protected final void addSettings(Setting<?>... values) {
		for (Setting<?> setting : values) {
			setting.onChange(() -> changeListener.run());
			settings.add(setting);
		}
	}

	public final List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	public final boolean hasSettings() {
		return !settings.isEmpty();
	}

	public final Optional<Setting<?>> setting(String settingId) {
		return settings.stream().filter(setting -> setting.id().equals(settingId)).findFirst();
	}

	/** Puts every setting back to the value it ships with. */
	public final void resetSettings() {
		settings.forEach(Setting::reset);
	}

	/** Set by the {@link ModuleManager} so changes can be persisted. */
	public final void setChangeListener(Runnable listener) {
		this.changeListener = listener;
	}

	/** Set by the {@link ModuleManager} so being switched on or off can be announced. */
	public final void setStateListener(java.util.function.Consumer<Module> listener) {
		this.stateListener = listener;
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	/** Applies the new state, logging and swallowing errors so one module cannot break the UI. */
	public final void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		Minecraft mc = Minecraft.getInstance();
		try {
			if (value) {
				onEnable(mc);
			} else {
				onDisable(mc);
			}
		} catch (Throwable t) {
			N4y.LOGGER.error("Module '{}' failed to {}", id, value ? "enable" : "disable", t);
		}
		changeListener.run();
		stateListener.accept(this);
	}

	protected void onEnable(Minecraft mc) {
	}

	protected void onDisable(Minecraft mc) {
	}

	/** Called every client tick while enabled. Never called on the render thread's hot path. */
	public void onTick(Minecraft mc) {
	}
}
