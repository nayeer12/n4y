package com.n4y.client.combat;

import java.util.function.BiConsumer;

import net.minecraft.network.chat.Component;

/**
 * What the combat modules share: the single interaction gate, the crystal routine both
 * crystal modules drive, and a way to warn the player through n4y's notifications.
 *
 * @param interactions every use and attack goes through here
 * @param crystals     AutoCrystal's logic, shared with AutoHitCrystal
 * @param warn         shows a notification with a title and a detail line
 */
public record CombatContext(Interactions interactions, CrystalRoutine crystals, BiConsumer<Component, Component> warn) {
	public static CombatContext create(BiConsumer<Component, Component> warn) {
		Interactions interactions = new Interactions();
		return new CombatContext(interactions, new CrystalRoutine(interactions), warn);
	}
}
