package dev.naturaltotem.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileTest {

    @Test
    void writingAndReadingBackGivesTheSameValues() {
        TotemConfig original = new TotemConfig();
        original.set(Options.HOTBAR_SLOT, 2);
        original.set(Options.MOVE_MS, 250L);
        original.set(Options.ENABLED, false);
        original.set(Options.ITEM, "minecraft:golden_apple");

        TotemConfig reloaded = new TotemConfig();
        assertEquals(List.of(), ConfigFormat.read(ConfigFormat.write(original), reloaded));
        assertEquals(original.asStrings(), reloaded.asStrings());
    }

    @Test
    void theFileExplainsEveryOption() {
        List<String> lines = ConfigFormat.write(new TotemConfig());
        for (Option<?> option : Options.ALL) {
            assertTrue(lines.contains(option.key() + "=" + option.printUnchecked(option.defaultValue())),
                    "falta la línea de " + option.key());
            assertTrue(lines.contains("# " + option.description()), "falta la descripción de " + option.key());
        }
    }

    @Test
    void unknownOrBrokenLinesAreReportedWithoutLosingTheRest() {
        TotemConfig config = new TotemConfig();
        List<String> warnings = ConfigFormat.read(List.of(
                "# comentario",
                "",
                "move-ms=120",
                "casilla-magica=3",
                "hotbar-slot=99",
                "esto no es una línea válida",
                "  fill-offhand = false  "), config);

        assertEquals(120L, config.get(Options.MOVE_MS));
        assertFalse(config.get(Options.FILL_OFFHAND));
        assertEquals(9, config.get(Options.HOTBAR_SLOT), "un valor inválido deja el anterior");
        assertEquals(3, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("casilla-magica"));
        assertTrue(warnings.get(1).contains("hotbar-slot"));
        assertTrue(warnings.get(2).contains("línea 6"));
    }

    @Test
    void theStoreCreatesTheFileWithTheDefaults(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("nested").resolve("natural_totem.properties");
        TotemConfig config = new TotemConfig();
        FileConfigStore store = new FileConfigStore(file, config, message -> {
        });

        assertEquals(List.of(), store.load());
        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("hotbar-slot=9"));
    }

    @Test
    void savingAndReloadingKeepsTheValues(@TempDir Path directory) {
        Path file = directory.resolve("natural_totem.properties");
        TotemConfig config = new TotemConfig();
        FileConfigStore store = new FileConfigStore(file, config, message -> {
        });
        store.load();

        config.set(Options.HOTBAR_SLOT, 4);
        store.save();
        config.set(Options.HOTBAR_SLOT, 1);
        assertTrue(store.reload().contains("recargada"));
        assertEquals(4, config.get(Options.HOTBAR_SLOT));
    }

    @Test
    void aBrokenFileKeepsTheRunningSettings(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("natural_totem.properties");
        TotemConfig config = new TotemConfig();
        List<String> logged = new ArrayList<>();
        FileConfigStore store = new FileConfigStore(file, config, logged::add);
        store.load();

        config.set(Options.MOVE_MS, 200L);
        Files.writeString(file, "move-ms=qué\n", StandardCharsets.UTF_8);

        String message = store.reload();
        assertTrue(message.contains("errores"), message);
        assertEquals(200L, config.get(Options.MOVE_MS), "los ajustes en uso no se pierden");
        assertFalse(logged.isEmpty());
    }

    @Test
    void readingKeepsOptionsThatTheFileDoesNotMention() {
        TotemConfig config = new TotemConfig();
        config.set(Options.DEBUG, true);
        assertEquals(List.of(), ConfigFormat.read(List.of("move-ms=10"), config));
        assertTrue(config.debug());
    }
}
