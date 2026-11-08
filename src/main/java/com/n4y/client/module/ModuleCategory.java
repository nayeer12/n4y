package com.n4y.client.module;

import com.n4y.client.gui.icon.Icon;
import com.n4y.client.gui.icon.Icons;

import net.minecraft.network.chat.Component;

/** Sidebar categories. {@link #ALL} is a filter, never a module's own category. */
public enum ModuleCategory {
	ALL("all", Icons.GRID),
	COMBAT("combat", Icons.SWORDS),
	MOVEMENT("movement", Icons.RUN),
	VISUAL("visual", Icons.EYE),
	PLAYER("player", Icons.PERSON),
	WORLD("world", Icons.CUBE),
	MISC("misc", Icons.DOTS);

	private final String id;
	private final Icon icon;

	ModuleCategory(String id, Icon icon) {
		this.id = id;
		this.icon = icon;
	}

	public String id() {
		return id;
	}

	public Icon icon() {
		return icon;
	}

	public Component displayName() {
		return Component.translatable("n4y.category." + id);
	}
}
