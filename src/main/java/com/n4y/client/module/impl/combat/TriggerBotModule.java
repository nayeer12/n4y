package com.n4y.client.module.impl.combat;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.combat.CombatContext;
import com.n4y.client.combat.Targets;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntRangeSetting;
import com.n4y.client.module.setting.IntSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Attacks the entity under the crosshair when the weapon's delay has passed. It only ever hits
 * what the player is already aiming at; the camera is never moved for them.
 *
 * <p>Target filtering: players by default, every entity with All Entities. There is no friends
 * list in n4y, so nothing is excluded by name. Delays are in milliseconds, drawn from the
 * range before every hit; swords and axes keep separate timers.
 */
public final class TriggerBotModule extends Module {
	private final BooleanSetting workInScreen = new BooleanSetting("work_in_screen", false);
	private final BooleanSetting whileUse = new BooleanSetting("while_use", false);
	private final BooleanSetting onLeftClick = new BooleanSetting("on_left_click", false);
	private final BooleanSetting allItems = new BooleanSetting("all_items", false);
	private final IntRangeSetting swordDelay = new IntRangeSetting("sword_delay", 0, 1000, 540, 550);
	private final IntRangeSetting axeDelay = new IntRangeSetting("axe_delay", 0, 1000, 780, 800);
	private final BooleanSetting checkShield = new BooleanSetting("check_shield", false);
	private final BooleanSetting onlyCritSword = new BooleanSetting("only_crit_sword", false);
	private final BooleanSetting onlyCritAxe = new BooleanSetting("only_crit_axe", false);
	private final BooleanSetting swingHand = new BooleanSetting("swing_hand", true);
	private final BooleanSetting whileAscending = new BooleanSetting("while_ascending", false);
	private final BooleanSetting allEntities = new BooleanSetting("all_entities", false);
	private final BooleanSetting useShield = new BooleanSetting("use_shield", false);
	private final IntSetting shieldTime = new IntSetting("shield_time", 100, 1000, 350, 1);
	private final BooleanSetting samePlayer = new BooleanSetting("same_player", false);

	private final CombatContext combat;
	private long lastSwordHit;
	private long lastAxeHit;
	private int nextSwordDelay;
	private int nextAxeDelay;
	private long shieldUntil;
	private boolean raisedShield;

	private enum Weapon {
		SWORD, AXE
	}

	public TriggerBotModule(CombatContext combat) {
		super("trigger_bot", ModuleCategory.COMBAT, Icons.CROSSHAIR);
		this.combat = combat;
		addSettings(workInScreen, whileUse, onLeftClick, allItems, swordDelay, axeDelay, checkShield,
				whileAscending, samePlayer, onlyCritSword, onlyCritAxe, swingHand, allEntities, useShield, shieldTime);
	}

	@Override
	protected void onEnable(Minecraft mc) {
		nextSwordDelay = swordDelay.randomValue();
		nextAxeDelay = axeDelay.randomValue();
		lastSwordHit = 0L;
		lastAxeHit = 0L;
		raisedShield = false;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		lowerShield(mc);
	}

	@Override
	public void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (raisedShield && now >= shieldUntil) {
			lowerShield(mc);
		}
		if (!workInScreen.value() && mc.screen != null) {
			return;
		}
		long window = mc.getWindow().handle();
		if (onLeftClick.value() && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			return;
		}
		ItemStack offhand = player.getOffhandItem();
		boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (!whileUse.value() && rightDown && !raisedShield
				&& (offhand.has(DataComponents.FOOD) || offhand.is(Items.SHIELD))) {
			return;
		}
		if (!whileAscending.value() && !player.onGround()
				&& (player.getDeltaMovement().y > 0 || player.fallDistance <= 0.0)) {
			return;
		}

		Weapon weapon = weaponOf(player.getMainHandItem());
		if (weapon == null) {
			return;
		}
		Entity target = Targets.entityUnderCrosshair(mc);
		if (target == null || !isValidTarget(target)) {
			return;
		}
		LivingEntity last = player.getLastHurtMob();
		if (samePlayer.value() && last != null && last.isAlive() && target != last) {
			return;
		}
		if (checkShield.value() && Targets.isProtectedByShield(target, player)) {
			return;
		}
		boolean critOnly = weapon == Weapon.SWORD ? onlyCritSword.value() : onlyCritAxe.value();
		if (critOnly && !Targets.wouldCrit(player, target)) {
			return;
		}

		long lastHit = weapon == Weapon.SWORD ? lastSwordHit : lastAxeHit;
		int delay = weapon == Weapon.SWORD ? nextSwordDelay : nextAxeDelay;
		if (now - lastHit >= delay) {
			lowerShield(mc);
			combat.interactions().attack(mc, target, swingHand.value());
			if (weapon == Weapon.SWORD) {
				lastSwordHit = now;
				nextSwordDelay = swordDelay.randomValue();
			} else {
				lastAxeHit = now;
				nextAxeDelay = axeDelay.randomValue();
			}
		} else if (useShield.value() && offhand.is(Items.SHIELD) && !player.isUsingItem()
				&& combat.interactions().startUsing(mc, InteractionHand.OFF_HAND)) {
			raisedShield = true;
			shieldUntil = now + shieldTime.value();
		}
	}

	/** All Items uses the sword timing for anything held, as the original did. */
	private Weapon weaponOf(ItemStack held) {
		if (allItems.value() || held.is(ItemTags.SWORDS)) {
			return Weapon.SWORD;
		}
		return held.is(ItemTags.AXES) ? Weapon.AXE : null;
	}

	private boolean isValidTarget(Entity target) {
		if (!target.isAlive()) {
			return false;
		}
		return target instanceof Player || allEntities.value();
	}

	/** Lowers the shield only if this module raised it, so it never fights the player's own block. */
	private void lowerShield(Minecraft mc) {
		if (raisedShield) {
			raisedShield = false;
			combat.interactions().stopUsing(mc);
		}
	}
}
