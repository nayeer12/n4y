package com.n4y.client.module.impl.combat;

import com.n4y.client.combat.Clock;
import com.n4y.client.combat.Inventories;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Smart AutoTotem: refills the off hand, and optionally a hotbar slot, with a totem, but only
 * while the player has their own inventory open. With the inventory closed it does nothing at
 * all: no clicks, no cursor movement, no hidden moves.
 *
 * <p>Every move is a normal swap click. After each one the module waits until the inventory
 * shows the result (or a short timeout passes) before making another, so it never repeats a
 * move that is already on its way or floods the server.
 */
public final class SmartAutoTotemModule extends Module {
	private static final int CONFIRM_TIMEOUT_TICKS = 10;

	private final IntSetting delay = new IntSetting("delay", 0, 20, 0, 1);
	private final BooleanSetting hotbar = new BooleanSetting("hotbar", true);
	private final IntSetting totemSlot = new IntSetting("totem_slot", 1, 9, 1, 1);
	private final BooleanSetting autoSwitch = new BooleanSetting("auto_switch", false);
	private final BooleanSetting forceTotem = new BooleanSetting("force_totem", false);

	private final Clock clock = new Clock();
	private boolean wasOpen;
	/** What the last click was meant to achieve, or NONE when nothing is in flight. */
	private Pending pending = Pending.NONE;
	private int pendingTicks;

	private enum Pending {
		NONE, OFFHAND, HOTBAR
	}

	public SmartAutoTotemModule() {
		super("auto_totem", ModuleCategory.COMBAT, Icons.HEART);
		addSettings(delay, hotbar, totemSlot, autoSwitch, forceTotem);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		resetState();
	}

	@Override
	protected void onDisable(Minecraft mc) {
		resetState();
	}

	private void resetState() {
		clock.clear();
		pending = Pending.NONE;
		pendingTicks = 0;
		wasOpen = false;
	}

	private static boolean isTotem(ItemStack stack) {
		return stack.is(Items.TOTEM_OF_UNDYING);
	}

	@Override
	public void onTick(Minecraft mc) {
		if (mc.player == null || !Inventories.isPlayerInventoryOpen(mc)) {
			if (wasOpen) {
				resetState();
			}
			return;
		}
		int hotbarIndex = totemSlot.value() - 1;
		if (!wasOpen) {
			wasOpen = true;
			clock.arm(delay.value());
			if (autoSwitch.value()) {
				Inventories.selectSlot(mc, hotbarIndex);
			}
		}
		clock.tick();

		if (pending != Pending.NONE && !confirmed(mc, hotbarIndex) && ++pendingTicks < CONFIRM_TIMEOUT_TICKS) {
			return;
		}
		pending = Pending.NONE;
		pendingTicks = 0;
		if (!clock.ready()) {
			return;
		}

		if (!isTotem(mc.player.getOffhandItem())) {
			int source = Inventories.findInMenu(mc, SmartAutoTotemModule::isTotem);
			if (source >= 0 && Inventories.swap(mc, source, Inventories.OFFHAND_BUTTON)) {
				begin(Pending.OFFHAND);
			}
			return;
		}

		if (hotbar.value()) {
			ItemStack current = mc.player.getInventory().getItem(hotbarIndex);
			if (current.isEmpty() || forceTotem.value() && !isTotem(current)) {
				int destination = InventoryMenu.USE_ROW_SLOT_START + hotbarIndex;
				int source = findTotemExcept(mc, destination);
				if (source >= 0 && Inventories.swap(mc, source, hotbarIndex)) {
					begin(Pending.HOTBAR);
				}
			}
		}
	}

	private void begin(Pending what) {
		pending = what;
		pendingTicks = 0;
		clock.arm(delay.value());
	}

	private boolean confirmed(Minecraft mc, int hotbarIndex) {
		return switch (pending) {
			case OFFHAND -> isTotem(mc.player.getOffhandItem());
			case HOTBAR -> isTotem(mc.player.getInventory().getItem(hotbarIndex));
			case NONE -> true;
		};
	}

	private static int findTotemExcept(Minecraft mc, int excludedSlot) {
		var menu = mc.player.inventoryMenu;
		for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
			if (slot != excludedSlot && isTotem(menu.getSlot(slot).getItem())) {
				return slot;
			}
		}
		return -1;
	}
}
