package com.n4y.client.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import com.n4y.client.core.input.InputBinding;
import com.n4y.client.module.Module;
import com.n4y.client.module.ModuleCategory;
import com.n4y.client.module.impl.combat.AnchorMacroModule;
import com.n4y.client.module.impl.combat.AutoCrystalModule;
import com.n4y.client.module.impl.combat.AutoHitCrystalModule;
import com.n4y.client.module.impl.combat.SmartAutoTotemModule;
import com.n4y.client.module.impl.combat.TriggerBotModule;
import com.n4y.client.module.setting.Setting;

/**
 * Pins down the specification agreed for each combat module: exactly which settings it has,
 * and the default each one ships with. A setting added, dropped or renamed by accident fails
 * here before it reaches a profile.
 */
class CombatSpecTest {
	private final CombatContext combat = CombatContext.create((title, detail) -> { });

	private static List<String> ids(Module module) {
		return module.settings().stream().map(Setting::id).toList();
	}

	private static Object value(Module module, String id) {
		return module.setting(id).orElseThrow(() -> new AssertionError("missing setting " + id)).get();
	}

	@Test
	void autoCrystalKeepsExactlyFiveSettings() {
		AutoCrystalModule module = new AutoCrystalModule(combat);
		assertEquals(List.of("activate_key", "place_delay", "break_delay", "place_chance", "break_chance"), ids(module));
		assertEquals(InputBinding.mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT), value(module, "activate_key"));
		assertEquals(0, value(module, "place_delay"));
		assertEquals(0, value(module, "break_delay"));
		assertEquals(100, value(module, "place_chance"));
		assertEquals(100, value(module, "break_chance"));
		assertEquals(ModuleCategory.COMBAT, module.category());
	}

	@Test
	void autoHitCrystalKeepsNineSettings() {
		AutoHitCrystalModule module = new AutoHitCrystalModule(combat, new AutoCrystalModule(combat));
		assertEquals(List.of("activate_key", "check_place", "switch_delay", "switch_chance", "place_delay",
				"place_chance", "work_with_totem", "work_with_crystal", "sword_swap"), ids(module));
		assertEquals(false, value(module, "check_place"));
		assertEquals(false, value(module, "work_with_totem"));
		assertEquals(false, value(module, "work_with_crystal"));
		assertEquals(true, value(module, "sword_swap"));
	}

	@Test
	void anchorMacroKeepsTwelvePlusPlaceAndSafeAnchor() {
		AnchorMacroModule module = new AnchorMacroModule(combat);
		assertEquals(List.of("while_use", "stop_on_kill", "switch_delay", "switch_chance", "place_chance",
				"glowstone_delay", "glowstone_chance", "explode_delay", "explode_chance", "explode_slot",
				"only_own", "only_charge", "place_key", "safe_anchor"), ids(module));
		assertEquals(true, value(module, "while_use"));
		assertEquals(false, value(module, "stop_on_kill"));
		assertEquals(1, value(module, "explode_slot"));
		assertEquals(false, value(module, "only_own"));
		assertEquals(false, value(module, "only_charge"));
		assertEquals(InputBinding.key(GLFW.GLFW_KEY_X), value(module, "place_key"));
		assertEquals(true, value(module, "safe_anchor"));
	}

	@Test
	void smartAutoTotemKeepsFiveSettings() {
		SmartAutoTotemModule module = new SmartAutoTotemModule();
		assertEquals(List.of("delay", "hotbar", "totem_slot", "auto_switch", "force_totem"), ids(module));
		assertEquals(0, value(module, "delay"));
		assertEquals(true, value(module, "hotbar"));
		assertEquals(1, value(module, "totem_slot"));
		assertEquals(false, value(module, "auto_switch"));
		assertEquals(false, value(module, "force_totem"));
	}

	@Test
	void triggerBotKeepsFifteenSettingsWithTheOriginalDefaults() {
		TriggerBotModule module = new TriggerBotModule(combat);
		List<String> ids = ids(module);
		assertEquals(15, ids.size());
		assertTrue(ids.containsAll(List.of("work_in_screen", "while_use", "on_left_click", "all_items", "sword_delay",
				"axe_delay", "check_shield", "only_crit_sword", "only_crit_axe", "swing_hand", "while_ascending",
				"all_entities", "use_shield", "shield_time", "same_player")));
		assertEquals(new com.n4y.client.module.setting.IntRange(540, 550), value(module, "sword_delay"));
		assertEquals(new com.n4y.client.module.setting.IntRange(780, 800), value(module, "axe_delay"));
		assertEquals(true, value(module, "swing_hand"));
		assertEquals(350, value(module, "shield_time"));
		assertEquals(false, value(module, "all_entities"));
	}

	@Test
	void removedSettingsAreReallyGone() {
		List<Module> modules = List.of(new AutoCrystalModule(combat),
				new AutoHitCrystalModule(combat, new AutoCrystalModule(combat)),
				new AnchorMacroModule(combat), new SmartAutoTotemModule(), new TriggerBotModule(combat));
		for (Module module : modules) {
			for (String gone : List.of("click_simulation", "particle_chance", "fake_punch", "damage_tick",
					"anti_weakness", "stray_bypass", "mode", "auto_open", "stay_open_for")) {
				assertFalse(module.setting(gone).isPresent(), module.id() + " still has " + gone);
			}
		}
	}
}
