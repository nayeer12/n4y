package dev.naturaltotem.config;

import dev.naturaltotem.RefillPlanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotemConfigTest {

    @Test
    void defaultsAreTheBehaviorOfVersion140() {
        TotemConfig config = new TotemConfig();
        assertTrue(config.isAllDefault());
        assertTrue(config.enabled());
        assertEquals(8, config.hotbarSlotIndex());
        assertEquals(60_000_000L, config.moveNanos());
        assertEquals(100_000_000L, config.actionIntervalNanos());
        assertEquals("minecraft:totem_of_undying", config.itemId());
        assertEquals(RefillPlanner.Options.LEGACY, config.plan());
        assertFalse(config.nothingToFill());
    }

    @Test
    void everyOptionHasAUniqueKeyAndAPrintableDefault() {
        List<String> keys = new ArrayList<>();
        TotemConfig config = new TotemConfig();
        for (Option<?> option : Options.ALL) {
            assertFalse(keys.contains(option.key()), "clave repetida: " + option.key());
            keys.add(option.key());
            assertFalse(option.description().isBlank(), option.key() + " no tiene descripción");
            String printed = config.printed(option);
            assertEquals(printed, option.printUnchecked(option.defaultValue()));
            assertDoesNotThrow(() -> config.setParsed(option, printed), option.key() + " no relee su propio valor");
        }
    }

    @Test
    void parsesTheUsualSpellingsOfBooleans() {
        TotemConfig config = new TotemConfig();
        for (String yes : List.of("true", "on", "YES", "1", "sí")) {
            config.set(Options.ENABLED, false);
            config.setParsed(Options.ENABLED, yes);
            assertTrue(config.enabled(), yes);
        }
        for (String no : List.of("false", "off", "NO", "0")) {
            config.set(Options.ENABLED, true);
            config.setParsed(Options.ENABLED, no);
            assertFalse(config.enabled(), no);
        }
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.ENABLED, "quizá"));
    }

    @Test
    void refusesValuesOutOfRangeAndKeepsThePreviousOne() {
        TotemConfig config = new TotemConfig();
        config.setParsed(Options.HOTBAR_SLOT, "1");
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.HOTBAR_SLOT, "0"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.HOTBAR_SLOT, "10"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.HOTBAR_SLOT, "tres"));
        assertEquals(1, config.get(Options.HOTBAR_SLOT));
        assertEquals(0, config.hotbarSlotIndex());

        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.MOVE_MS, "-1"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.MOVE_MS, "5001"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.ITEM, "Totem Of Undying"));
    }

    @Test
    void zeroIntervalStillGivesThePacerAPositiveValue() {
        TotemConfig config = new TotemConfig();
        config.setParsed(Options.ACTION_INTERVAL_MS, "0");
        assertEquals(1L, config.actionIntervalNanos());
        config.setParsed(Options.MOVE_MS, "0");
        assertEquals(0L, config.moveNanos());
    }

    @Test
    void enumOptionAcceptsDashesUnderscoresAndCase() {
        TotemConfig config = new TotemConfig();
        for (String spelling : List.of("hotbar-first", "HOTBAR_FIRST", "Hotbar-First")) {
            config.set(Options.SOURCE_ORDER, RefillPlanner.SourceOrder.MAIN_FIRST);
            config.setParsed(Options.SOURCE_ORDER, spelling);
            assertEquals(RefillPlanner.SourceOrder.HOTBAR_FIRST, config.get(Options.SOURCE_ORDER), spelling);
        }
        assertEquals("hotbar-first", config.printed(Options.SOURCE_ORDER));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.SOURCE_ORDER, "random"));
    }

    @Test
    void keysAreFoundHoweverTheyAreWritten() {
        assertSame(Options.MOVE_MS, Options.byKey("move-ms").orElseThrow());
        assertSame(Options.MOVE_MS, Options.byKey("MOVE_MS").orElseThrow());
        assertSame(Options.MOVE_MS, Options.byKey("move.ms").orElseThrow());
        assertTrue(Options.byKey("no-existe").isEmpty());
        assertTrue(Options.byKey(null).isEmpty());
        assertTrue(Options.keysStartingWith("fill-").containsAll(List.of("fill-hotbar", "fill-offhand")));
    }

    @Test
    void changesAreReportedOnceAndResetsRestoreTheDefaults() {
        TotemConfig config = new TotemConfig();
        List<String> changes = new ArrayList<>();
        config.onChange(option -> changes.add(option == null ? "all" : option.key()));

        assertTrue(config.set(Options.MOVE_MS, 120L));
        assertFalse(config.set(Options.MOVE_MS, 120L), "un valor repetido no es un cambio");
        assertEquals(List.of("move-ms"), changes);
        assertFalse(config.isDefault(Options.MOVE_MS));
        assertFalse(config.isAllDefault());

        config.reset(Options.MOVE_MS);
        assertTrue(config.isAllDefault());
        assertEquals(List.of("move-ms", "move-ms"), changes);

        config.set(Options.DEBUG, true);
        config.resetAll();
        assertTrue(config.isAllDefault());
        assertEquals("all", changes.getLast());
    }

    @Test
    void copiesAreIndependent() {
        TotemConfig config = new TotemConfig();
        config.set(Options.HOTBAR_SLOT, 3);
        TotemConfig copy = config.copy();
        config.set(Options.HOTBAR_SLOT, 7);
        assertEquals(3, copy.get(Options.HOTBAR_SLOT));

        copy.copyFrom(config);
        assertEquals(7, copy.get(Options.HOTBAR_SLOT));
    }

    @Test
    void warnsWhenThereIsNothingToFill() {
        TotemConfig config = new TotemConfig();
        config.set(Options.FILL_HOTBAR, false);
        assertFalse(config.nothingToFill());
        config.set(Options.FILL_OFFHAND, false);
        assertTrue(config.nothingToFill());
    }
}
