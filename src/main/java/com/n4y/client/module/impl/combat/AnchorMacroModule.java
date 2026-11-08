package com.n4y.client.module.impl.combat;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.combat.Chance;
import com.n4y.client.combat.Clock;
import com.n4y.client.combat.CombatContext;
import com.n4y.client.combat.Inventories;
import com.n4y.client.combat.Targets;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.BooleanSetting;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Respawn anchor combat on the anchor under the crosshair, while right click is held: switches
 * to glowstone and charges an empty anchor, then switches to the explode slot and detonates a
 * charged one. The Place key runs a full sequence: place an anchor, charge it, and get a totem
 * ready in the hand.
 *
 * <p>An anchor only explodes where it cannot set the spawn point, which in practice means
 * outside the Nether. "Only Own" can only know the anchors this client placed during the
 * current session; that list is cleared on disconnect or world change and pruned as soon as a
 * position stops being an anchor, so it never claims an anchor it cannot vouch for.
 */
public final class AnchorMacroModule extends Module {
	private static final int OWNED_LIMIT = 64;
	private static final double DEAD_BODY_RADIUS = 6.0;
	private static final int STEP_TIMEOUT_TICKS = 20;
	private static final long WARN_COOLDOWN_MS = 2000L;
	private static final int FALLBACK_TOTEM_SLOT = 8;

	private final BooleanSetting whileUse = new BooleanSetting("while_use", true);
	private final BooleanSetting stopOnKill = new BooleanSetting("stop_on_kill", false);
	private final IntSetting switchDelay = new IntSetting("switch_delay", 0, 20, 0, 1);
	private final IntSetting switchChance = new IntSetting("switch_chance", 0, 100, 100, 1);
	private final IntSetting placeChance = new IntSetting("place_chance", 0, 100, 100, 1);
	private final IntSetting glowstoneDelay = new IntSetting("glowstone_delay", 0, 20, 0, 1);
	private final IntSetting glowstoneChance = new IntSetting("glowstone_chance", 0, 100, 100, 1);
	private final IntSetting explodeDelay = new IntSetting("explode_delay", 0, 20, 0, 1);
	private final IntSetting explodeChance = new IntSetting("explode_chance", 0, 100, 100, 1);
	private final IntSetting explodeSlot = new IntSetting("explode_slot", 1, 9, 1, 1);
	private final BooleanSetting onlyOwn = new BooleanSetting("only_own", false);
	private final BooleanSetting onlyCharge = new BooleanSetting("only_charge", false);
	private final KeySetting placeKey = new KeySetting("place_key", InputBinding.key(GLFW.GLFW_KEY_X));
	private final BooleanSetting safeAnchor = new BooleanSetting("safe_anchor", true);

	private final CombatContext combat;
	private final Clock switchClock = new Clock();
	private final Clock glowstoneClock = new Clock();
	private final Clock explodeClock = new Clock();
	private final Set<BlockPos> owned = new LinkedHashSet<>();
	private final Set<BlockPos> pendingOwned = new LinkedHashSet<>();
	private Level lastLevel;
	private boolean placeKeyWasDown;
	private long lastWarning;

	/** The Place sequence, one step per stage, each validated before moving on. */
	private enum Stage {
		IDLE, SELECT_ANCHOR, PLACE_ANCHOR, SELECT_GLOWSTONE, CHARGE, SELECT_TOTEM
	}

	private Stage stage = Stage.IDLE;
	private BlockHitResult placeHit;
	private BlockPos placedPos;
	private int stageTicks;

	public AnchorMacroModule(CombatContext combat) {
		super("anchor_macro", ModuleCategory.COMBAT, Icons.FLAME);
		this.combat = combat;
		addSettings(whileUse, stopOnKill, switchDelay, switchChance, placeChance, glowstoneDelay, glowstoneChance,
				explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge, placeKey, safeAnchor);
	}

	/** Records anchors the player places by hand, so Only Own also covers them. Called once. */
	public void listen() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR)) {
				BlockPos clicked = hit.getBlockPos();
				pendingOwned.add(level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection()));
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	protected void onEnable(Minecraft mc) {
		clearClocks();
		cancelPlace();
		lastLevel = mc == null ? null : mc.level;
	}

	@Override
	protected void onDisable(Minecraft mc) {
		clearClocks();
		cancelPlace();
	}

	private void clearClocks() {
		switchClock.clear();
		glowstoneClock.clear();
		explodeClock.clear();
	}

	@Override
	public void onTick(Minecraft mc) {
		switchClock.tick();
		glowstoneClock.tick();
		explodeClock.tick();
		if (mc.player == null || mc.level == null) {
			return;
		}
		trackOwnership(mc.level);
		if (mc.screen != null) {
			cancelPlace();
			return;
		}

		boolean placeDown = placeKey.isBound() && placeKey.binding().isDown(mc.getWindow());
		if (placeDown && !placeKeyWasDown) {
			requestPlace(mc);
		}
		placeKeyWasDown = placeDown;
		if (stage != Stage.IDLE) {
			runPlace(mc);
			return;
		}

		if (whileUse.value() && isUsingSomething(mc)) {
			return;
		}
		if (stopOnKill.value() && Targets.deadPlayerNearby(mc, DEAD_BODY_RADIUS)) {
			return;
		}
		if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
			return;
		}
		BlockHitResult hit = Targets.blockUnderCrosshair(mc);
		if (hit == null) {
			return;
		}
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		if (!state.is(Blocks.RESPAWN_ANCHOR)) {
			return;
		}
		if (onlyOwn.value() && !owned.contains(hit.getBlockPos())) {
			return;
		}
		if (state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
			charge(mc, hit);
		} else if (!onlyCharge.value()) {
			explode(mc, hit);
		}
	}

	/** While Use: eating, drinking or holding a shield up with right click held. */
	private boolean isUsingSomething(Minecraft mc) {
		if (mc.player.isUsingItem()) {
			return true;
		}
		boolean rightDown = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		return rightDown && (isUsable(mc.player.getMainHandItem()) || isUsable(mc.player.getOffhandItem()));
	}

	private static boolean isUsable(ItemStack stack) {
		return stack.has(DataComponents.FOOD) || stack.is(Items.SHIELD);
	}

	private void charge(Minecraft mc, BlockHitResult hit) {
		if (!Chance.roll(placeChance.value())) {
			return;
		}
		if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
			if (switchClock.ready() && Chance.roll(switchChance.value()) && Inventories.selectFromHotbar(mc, Items.GLOWSTONE)) {
				switchClock.arm(switchDelay.value());
			}
			return;
		}
		if (glowstoneClock.ready() && Chance.roll(glowstoneChance.value()) && combat.interactions().useOn(mc, hit)) {
			glowstoneClock.arm(glowstoneDelay.value());
		}
	}

	private void explode(Minecraft mc, BlockHitResult hit) {
		int slot = explodeSlot.value() - 1;
		if (mc.player.getInventory().getSelectedSlot() != slot) {
			if (switchClock.ready() && Chance.roll(switchChance.value()) && Inventories.selectSlot(mc, slot)) {
				switchClock.arm(switchDelay.value());
			}
			return;
		}
		if (!explodeClock.ready() || !Chance.roll(explodeChance.value())) {
			return;
		}
		if (safeAnchor.value()) {
			Component problem = safeAnchorProblem(mc, hit.getBlockPos());
			if (problem != null) {
				warn(Component.translatable("n4y.anchor.blocked"), problem);
				return;
			}
		}
		if (combat.interactions().useOn(mc, hit)) {
			explodeClock.arm(explodeDelay.value());
			owned.remove(hit.getBlockPos());
		}
	}

	/**
	 * Safe Anchor: every condition that must hold before detonating. Returns what is wrong, or
	 * null when everything checks out. If a condition cannot be confirmed, it counts as failed.
	 */
	private Component safeAnchorProblem(Minecraft mc, BlockPos pos) {
		BlockState state = mc.level.getBlockState(pos);
		if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
			return Component.translatable("n4y.anchor.not_charged");
		}
		if (mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)) {
			return Component.translatable("n4y.anchor.sets_spawn");
		}
		if (!hasTotemReady(mc)) {
			return Component.translatable("n4y.anchor.no_totem");
		}
		// Glowstone in hand would charge the anchor again instead of detonating it.
		if (mc.player.getMainHandItem().is(Items.GLOWSTONE) || mc.player.getOffhandItem().is(Items.GLOWSTONE)) {
			return Component.translatable("n4y.anchor.glowstone_in_hand");
		}
		return null;
	}

	private static boolean hasTotemReady(Minecraft mc) {
		return mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
				|| Inventories.findInHotbar(mc, Items.TOTEM_OF_UNDYING) >= 0;
	}

	// ---------------------------------------------------------------- Place sequence

	/**
	 * Starts the Place sequence on the block under the crosshair, as a press of the Place key
	 * does. Ignored while a sequence is already running.
	 */
	public void requestPlace(Minecraft mc) {
		if (stage != Stage.IDLE || mc.player == null || mc.level == null) {
			return;
		}
		BlockHitResult hit = Targets.blockUnderCrosshair(mc);
		if (hit == null) {
			warn(Component.translatable("n4y.anchor.place_failed"), Component.translatable("n4y.anchor.look_at_block"));
			return;
		}
		BlockPos clicked = hit.getBlockPos();
		BlockPos target = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		if (!mc.level.getBlockState(target).canBeReplaced()
				|| !mc.level.getEntities((Entity) null, new AABB(target), entity -> entity.blocksBuilding).isEmpty()) {
			warn(Component.translatable("n4y.anchor.place_failed"), Component.translatable("n4y.anchor.no_space"));
			return;
		}
		if (Inventories.findInHotbar(mc, Items.RESPAWN_ANCHOR) < 0 || Inventories.findInHotbar(mc, Items.GLOWSTONE) < 0) {
			warn(Component.translatable("n4y.anchor.place_failed"), Component.translatable("n4y.anchor.missing_items"));
			return;
		}
		placeHit = hit;
		placedPos = target;
		enter(Stage.SELECT_ANCHOR);
	}

	private void enter(Stage next) {
		stage = next;
		stageTicks = 0;
	}

	private void cancelPlace() {
		stage = Stage.IDLE;
		placeHit = null;
		placedPos = null;
		stageTicks = 0;
	}

	private void failPlace(Component reason) {
		cancelPlace();
		warn(Component.translatable("n4y.anchor.place_failed"), reason);
	}

	private void runPlace(Minecraft mc) {
		if (++stageTicks > STEP_TIMEOUT_TICKS) {
			failPlace(Component.translatable("n4y.anchor.timed_out"));
			return;
		}
		switch (stage) {
			case SELECT_ANCHOR -> {
				if (switchClock.ready() && Inventories.selectFromHotbar(mc, Items.RESPAWN_ANCHOR)) {
					switchClock.arm(switchDelay.value());
					enter(Stage.PLACE_ANCHOR);
				}
			}
			case PLACE_ANCHOR -> {
				if (mc.level.getBlockState(placedPos).is(Blocks.RESPAWN_ANCHOR)) {
					rememberOwned(placedPos);
					enter(Stage.SELECT_GLOWSTONE);
					return;
				}
				BlockHitResult now = Targets.blockUnderCrosshair(mc);
				if (now == null || !now.getBlockPos().equals(placeHit.getBlockPos())) {
					failPlace(Component.translatable("n4y.anchor.aim_changed"));
					return;
				}
				if (switchClock.ready() && mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
					combat.interactions().useOn(mc, now);
				}
			}
			case SELECT_GLOWSTONE -> {
				if (switchClock.ready() && Inventories.selectFromHotbar(mc, Items.GLOWSTONE)) {
					switchClock.arm(switchDelay.value());
					enter(Stage.CHARGE);
				}
			}
			case CHARGE -> {
				BlockState state = mc.level.getBlockState(placedPos);
				if (!state.is(Blocks.RESPAWN_ANCHOR)) {
					failPlace(Component.translatable("n4y.anchor.anchor_gone"));
					return;
				}
				if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
					enter(Stage.SELECT_TOTEM);
					return;
				}
				// The new anchor must be what the crosshair rests on: no aiming for the player.
				BlockHitResult now = Targets.blockUnderCrosshair(mc);
				if (now == null || !now.getBlockPos().equals(placedPos)) {
					failPlace(Component.translatable("n4y.anchor.aim_changed"));
					return;
				}
				if (glowstoneClock.ready() && mc.player.getMainHandItem().is(Items.GLOWSTONE)
						&& combat.interactions().useOn(mc, now)) {
					glowstoneClock.arm(glowstoneDelay.value());
				}
			}
			case SELECT_TOTEM -> {
				if (!switchClock.ready()) {
					return;
				}
				int totem = Inventories.findInHotbar(mc, Items.TOTEM_OF_UNDYING);
				if (totem >= 0) {
					Inventories.selectSlot(mc, totem);
				} else {
					Inventories.selectSlot(mc, FALLBACK_TOTEM_SLOT);
					warn(Component.translatable("n4y.anchor.no_totem_title"), Component.translatable("n4y.anchor.no_totem"));
				}
				cancelPlace();
			}
			case IDLE -> {
			}
		}
	}

	// ---------------------------------------------------------------- ownership

	private void rememberOwned(BlockPos pos) {
		owned.add(pos.immutable());
		while (owned.size() > OWNED_LIMIT) {
			Iterator<BlockPos> oldest = owned.iterator();
			oldest.next();
			oldest.remove();
		}
	}

	/** Confirms pending placements, drops positions that are no longer anchors, resets on world change. */
	private void trackOwnership(Level level) {
		if (lastLevel == null) {
			// First look at the world since enabling: nothing to forget yet.
			lastLevel = level;
		} else if (level != lastLevel) {
			lastLevel = level;
			owned.clear();
			pendingOwned.clear();
			cancelPlace();
			return;
		}
		Iterator<BlockPos> pending = pendingOwned.iterator();
		while (pending.hasNext()) {
			BlockPos pos = pending.next();
			if (level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
				rememberOwned(pos);
			}
			pending.remove();
		}
		owned.removeIf(pos -> level.isLoaded(pos) && !level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)
				|| !level.isLoaded(pos));
	}

	/** Anchors this client can vouch for right now; exposed for the tests. */
	public Set<BlockPos> ownedAnchors() {
		return Set.copyOf(owned);
	}

	private void warn(Component title, Component detail) {
		long now = System.currentTimeMillis();
		if (now - lastWarning >= WARN_COOLDOWN_MS) {
			lastWarning = now;
			combat.warn().accept(title, detail);
		}
	}
}
