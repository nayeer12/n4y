package com.n4y.client.module.impl.combat;

import org.lwjgl.glfw.GLFW;

import com.n4y.client.combat.CombatContext;
import com.n4y.client.core.input.InputBinding;
import com.n4y.client.gui.icon.Icons;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.setting.IntSetting;
import com.n4y.client.module.setting.KeySetting;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

/**
 * While the activate key is held with an end crystal in hand, places crystals on the obsidian
 * or bedrock under the crosshair and hits the crystal or slime under it.
 *
 * <p>Only what is under the crosshair: the camera never moves on its own. Placements follow
 * vanilla's own use cooldown, so this alternates placing and breaking from one key rather than
 * clicking faster than the game allows. Delays are in ticks.
 */
public final class AutoCrystalModule extends Module {
	private final KeySetting activateKey = new KeySetting("activate_key", InputBinding.mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
	private final IntSetting placeDelay = new IntSetting("place_delay", 0, 20, 0, 1);
	private final IntSetting breakDelay = new IntSetting("break_delay", 0, 20, 0, 1);
	private final IntSetting placeChance = new IntSetting("place_chance", 0, 100, 100, 1);
	private final IntSetting breakChance = new IntSetting("break_chance", 0, 100, 100, 1);

	private final CombatContext combat;

	public AutoCrystalModule(CombatContext combat) {
		super("auto_crystal", ModuleCategory.COMBAT, Icons.CUBE);
		this.combat = combat;
		addSettings(activateKey, placeDelay, breakDelay, placeChance, breakChance);
	}

	public int placeDelay() {
		return placeDelay.value();
	}

	public int breakDelay() {
		return breakDelay.value();
	}

	public int placeChance() {
		return placeChance.value();
	}

	public int breakChance() {
		return breakChance.value();
	}

	@Override
	protected void onEnable(Minecraft mc) {
		combat.crystals().reset();
	}

	@Override
	protected void onDisable(Minecraft mc) {
		combat.crystals().reset();
	}

	@Override
	public void onTick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.screen != null || mc.player.isUsingItem()) {
			return;
		}
		if (activateKey.isBound() && !activateKey.binding().isDown(mc.getWindow())) {
			combat.crystals().reset();
			return;
		}
		if (!mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
			return;
		}
		combat.crystals().step(mc, placeDelay(), breakDelay(), placeChance(), breakChance());
	}
}
