package com.n4y.client.combat;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory work, done the way a player does it. Changing the selected slot is the normal
 * client call; moving an item is a real container click, and n4y only makes one while the
 * player actually has the inventory open.
 */
public final class Inventories {
	/** Button number that means "the off hand" in a swap click. */
	public static final int OFFHAND_BUTTON = 40;

	private Inventories() {
	}

	/** First hotbar slot holding {@code item}, or -1. */
	public static int findInHotbar(Minecraft mc, Item item) {
		if (mc.player == null) {
			return -1;
		}
		Inventory inventory = mc.player.getInventory();
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (inventory.getItem(slot).is(item)) {
				return slot;
			}
		}
		return -1;
	}

	/** Selects a hotbar slot. The change is visible to everyone, like scrolling the wheel. */
	public static boolean selectSlot(Minecraft mc, int slot) {
		if (mc.player == null || slot < 0 || slot >= Inventory.getSelectionSize()) {
			return false;
		}
		if (mc.player.getInventory().getSelectedSlot() == slot) {
			return true;
		}
		mc.player.getInventory().setSelectedSlot(slot);
		return true;
	}

	/** Switches to the first hotbar slot holding {@code item}. */
	public static boolean selectFromHotbar(Minecraft mc, Item item) {
		int slot = findInHotbar(mc, item);
		return slot >= 0 && selectSlot(mc, slot);
	}

	/** True when the player has their own inventory open, which is the only time n4y moves items. */
	public static boolean isPlayerInventoryOpen(Minecraft mc) {
		return mc.screen instanceof InventoryScreen;
	}

	/**
	 * First slot of the whole inventory matching {@code test}, searching the main area before
	 * the hotbar, or -1. Slot numbers are the ones a container click expects.
	 */
	public static int findInMenu(Minecraft mc, Predicate<ItemStack> test) {
		if (mc.player == null) {
			return -1;
		}
		var menu = mc.player.inventoryMenu;
		for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
			if (test.test(menu.getSlot(slot).getItem())) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * Swaps the stack in {@code slot} with a hotbar slot or the off hand, through the same
	 * click the player would make. Requires the inventory to be open.
	 *
	 * @param button hotbar index 0..8, or {@link #OFFHAND_BUTTON}
	 */
	public static boolean swap(Minecraft mc, int slot, int button) {
		if (!isPlayerInventoryOpen(mc) || mc.gameMode == null || mc.player == null || slot < 0) {
			return false;
		}
		mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, button,
				ClickType.SWAP, mc.player);
		return true;
	}
}
