package com.n4y.client.module;

import com.n4y.client.N4y;
import com.n4y.client.gui.icon.Icon;

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
	/** Key code meaning "no key bound". */
	public static final int UNBOUND = -1;

	private final String id;
	private final ModuleCategory category;
	private final Icon icon;
	private boolean enabled;
	private int key = UNBOUND;
	private Runnable changeListener = () -> { };

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

	/** GLFW key code that toggles this module, or {@link #UNBOUND}. */
	public final int key() {
		return key;
	}

	public final void setKey(int value) {
		if (key == value) {
			return;
		}
		key = value;
		changeListener.run();
	}

	public final boolean hasKey() {
		return key != UNBOUND;
	}

	/** Set by the {@link ModuleManager} so changes can be persisted. */
	public final void setChangeListener(Runnable listener) {
		this.changeListener = listener;
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
	}

	protected void onEnable(Minecraft mc) {
	}

	protected void onDisable(Minecraft mc) {
	}

	/** Called every client tick while enabled. Never called on the render thread's hot path. */
	public void onTick(Minecraft mc) {
	}
}
