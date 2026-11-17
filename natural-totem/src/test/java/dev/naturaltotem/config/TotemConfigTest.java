package dev.naturaltotem.config;

import dev.naturaltotem.RefillPlanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class TotemConfigTest {

    @Test
    void defaultsAreTheOnesDocumentedForThisVersion() {
        TotemConfig config = new TotemConfig();
        RandomGenerator random = new Random(1);
        assertTrue(config.isAllDefault());
        assertTrue(config.enabled());
        assertEquals(8, config.hotbarSlotIndex());
        assertEquals("minecraft:totem_of_undying", config.itemId());
        assertTrue(config.continuous(), "por defecto sigue vigilando con el inventario abierto");
        assertEquals(RefillPlanner.SourceOrder.NEAREST_MAIN, config.get(Options.SOURCE_ORDER));
        assertEquals(RefillPlanner.Options.LEGACY.hotbarSlot(), config.plan().hotbarSlot());
        assertFalse(config.nothingToFill());
        assertEquals(List.of(), config.warnings());

        for (int attempt = 0; attempt < 200; attempt++) {
            long move = config.nextMoveNanos(random);
            assertTrue(move >= 50_000_000L && move <= 100_000_000L, "cursor entre 50 y 100 ms, era " + move);
            assertEquals(100_000_000L, config.nextActionIntervalNanos(random));
        }
    }

    @Test
    void theClassicPresetBringsBackVersion140() {
        TotemConfig config = new TotemConfig();
        config.applyAll(Presets.CLASSIC.values());
        RandomGenerator random = new Random(2);

        assertEquals(RefillPlanner.Options.LEGACY, config.plan());
        assertFalse(config.continuous(), "la 1.4.0 hacía una sola pasada por apertura");
        assertEquals(60_000_000L, config.nextMoveNanos(random));
        assertEquals(100_000_000L, config.nextActionIntervalNanos(random));
        assertEquals(0.0, config.curvature(), "la 1.4.0 movía el cursor en línea recta");
        assertEquals(0.0, config.aimJitter(), "la 1.4.0 apuntaba al centro exacto");
        assertTrue(Presets.matches(config, Presets.CLASSIC));
        assertFalse(Presets.matches(config, Presets.DEFAULT));
    }

    @Test
    void timingRangesAreSortedAndWarnedAboutWhenTheyAreUpsideDown() {
        TotemConfig config = new TotemConfig();
        RandomGenerator random = new Random(3);
        config.set(Options.MOVE_MS_MIN, 80L);
        config.set(Options.MOVE_MS_MAX, 20L);

        assertEquals(1, config.warnings().size(), config.warnings().toString());
        for (int attempt = 0; attempt < 50; attempt++) {
            assertEquals(80_000_000L, config.nextMoveNanos(random), "un margen al revés usa el mínimo");
        }

        config.set(Options.MOVE_MS_MAX, 80L);
        assertEquals(List.of(), config.warnings());
        assertEquals(80_000_000L, config.nextMoveNanos(random));
    }

    @Test
    void aWideIntervalRangeProducesDifferentWaits() {
        TotemConfig config = new TotemConfig();
        RandomGenerator random = new Random(4);
        config.set(Options.ACTION_INTERVAL_MS_MIN, 50L);
        config.set(Options.ACTION_INTERVAL_MS_MAX, 100L);

        Set<Long> seen = new HashSet<>();
        for (int attempt = 0; attempt < 500; attempt++) {
            long interval = config.nextActionIntervalNanos(random);
            assertTrue(interval >= 50_000_000L && interval <= 100_000_000L, "fuera del margen: " + interval);
            seen.add(interval);
        }
        assertTrue(seen.size() > 10, "la espera debería variar, se vieron " + seen.size() + " valores");
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

        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.MOVE_MS_MIN, "-1"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.MOVE_MS_MIN, "5001"));
        assertThrows(Option.InvalidValueException.class, () -> config.setParsed(Options.ITEM, "Totem Of Undying"));
    }

    @Test
    void zeroIntervalStillGivesThePacerAPositiveValue() {
        TotemConfig config = new TotemConfig();
        RandomGenerator random = new Random(5);
        config.setParsed(Options.ACTION_INTERVAL_MS_MIN, "0");
        config.setParsed(Options.ACTION_INTERVAL_MS_MAX, "0");
        assertEquals(1L, config.nextActionIntervalNanos(random));
        config.setParsed(Options.MOVE_MS_MIN, "0");
        config.setParsed(Options.MOVE_MS_MAX, "0");
        assertEquals(0L, config.nextMoveNanos(random));
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
        assertSame(Options.MOVE_MS_MIN, Options.byKey("move-ms-min").orElseThrow());
        assertSame(Options.MOVE_MS_MIN, Options.byKey("MOVE_MS_MIN").orElseThrow());
        assertSame(Options.MOVE_MS_MIN, Options.byKey("move.ms.min").orElseThrow());
        assertSame(Options.MOVE_MS_MIN, Options.byKey("move-ms").orElseThrow(), "nombre antiguo de la 1.5.0");
        assertSame(Options.ACTION_INTERVAL_MS_MIN, Options.byKey("action-interval-ms").orElseThrow());
        assertTrue(Options.byKey("no-existe").isEmpty());
        assertTrue(Options.byKey(null).isEmpty());
        assertTrue(Options.keysStartingWith("fill-").containsAll(List.of("fill-hotbar", "fill-offhand")));
    }

    @Test
    void changesAreReportedOnceAndResetsRestoreTheDefaults() {
        TotemConfig config = new TotemConfig();
        List<String> changes = new ArrayList<>();
        config.onChange(option -> changes.add(option == null ? "all" : option.key()));

        assertTrue(config.set(Options.MOVE_MS_MIN, 120L));
        assertFalse(config.set(Options.MOVE_MS_MIN, 120L), "un valor repetido no es un cambio");
        assertEquals(List.of("move-ms-min"), changes);
        assertFalse(config.isDefault(Options.MOVE_MS_MIN));
        assertFalse(config.isAllDefault());

        config.reset(Options.MOVE_MS_MIN);
        assertTrue(config.isAllDefault());
        assertEquals(List.of("move-ms-min", "move-ms-min"), changes);

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
