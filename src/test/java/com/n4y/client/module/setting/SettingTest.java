package com.n4y.client.module.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.n4y.client.core.input.InputBinding;

class SettingTest {
	@Test
	void numbersStayInsideTheirBounds() {
		IntSetting chance = new IntSetting("place_chance", 0, 100, 100, 1);
		assertEquals(100, chance.value());
		chance.set(150);
		assertEquals(100, chance.value(), "above the maximum");
		chance.set(-20);
		assertEquals(0, chance.value(), "below the minimum");
		chance.set(55);
		assertEquals(55, chance.value());
	}

	@Test
	void aDefaultOutsideTheBoundsIsBroughtIn() {
		IntSetting slot = new IntSetting("explode_slot", 1, 9, 42, 1);
		assertEquals(9, slot.value());
		assertEquals(9, slot.defaultValue());
		assertTrue(slot.isDefault());
	}

	@Test
	void stepsSnapTheValue() {
		IntSetting stepped = new IntSetting("stepped", 0, 100, 0, 10);
		stepped.set(24);
		assertEquals(20, stepped.value());
		stepped.set(26);
		assertEquals(30, stepped.value());
	}

	@Test
	void fractionsMapBothWays() {
		IntSetting delay = new IntSetting("place_delay", 0, 20, 0, 1);
		delay.setFraction(0.5f);
		assertEquals(10, delay.value());
		assertEquals(0.5f, delay.fraction(), 1e-4);
		delay.setFraction(2f);
		assertEquals(20, delay.value());
	}

	@Test
	void rangesKeepTheirOrderAndDrawInside() {
		IntRangeSetting sword = new IntRangeSetting("sword_delay", 0, 1000, 540, 550);
		assertEquals(540, sword.get().min());
		assertEquals(550, sword.get().max());
		for (int i = 0; i < 200; i++) {
			int value = sword.randomValue();
			assertTrue(sword.get().contains(value), "drew " + value + " outside the range");
		}
		// Dragging the low handle past the high one must not invert the range.
		sword.setLowFraction(1f);
		assertTrue(sword.get().min() <= sword.get().max());
	}

	@Test
	void resetAndChangeNotifications() {
		BooleanSetting flag = new BooleanSetting("while_use", true);
		int[] changes = {0};
		flag.onChange(() -> changes[0]++);
		flag.set(true);
		assertEquals(0, changes[0], "setting the same value notified");
		flag.toggle();
		assertFalse(flag.value());
		assertEquals(1, changes[0]);
		flag.reset();
		assertTrue(flag.value());
		assertTrue(flag.isDefault());
		assertEquals(2, changes[0]);
	}

	@Test
	void keySettingsHoldKeyboardAndMouse() {
		KeySetting activate = new KeySetting("activate_key", InputBinding.mouse(1));
		assertTrue(activate.isBound());
		activate.set(InputBinding.key(86));
		assertEquals(InputBinding.Type.KEY, activate.binding().type());
		activate.set(InputBinding.UNBOUND);
		assertFalse(activate.isBound());
	}

	@Test
	void settingsSerialiseToTheirOwnShape() {
		IntSetting number = new IntSetting("n", 0, 20, 0, 1);
		number.set(7);
		IntSetting reloaded = new IntSetting("n", 0, 20, 0, 1);
		reloaded.load(number.save());
		assertEquals(7, reloaded.value());

		IntRangeSetting range = new IntRangeSetting("r", 0, 1000, 540, 550);
		range.set(new IntRange(100, 200));
		IntRangeSetting rangeBack = new IntRangeSetting("r", 0, 1000, 540, 550);
		rangeBack.load(range.save());
		assertEquals(new IntRange(100, 200), rangeBack.get());
	}

	@Test
	void aBrokenStoredValueLeavesTheDefault() {
		IntSetting number = new IntSetting("n", 0, 20, 5, 1);
		number.load(new com.google.gson.JsonPrimitive("not a number"));
		assertEquals(5, number.value(), "a corrupt value replaced the default");
	}
}
