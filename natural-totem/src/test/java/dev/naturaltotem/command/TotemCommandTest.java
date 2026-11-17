package dev.naturaltotem.command;

import dev.naturaltotem.config.ConfigStore;
import dev.naturaltotem.config.Options;
import dev.naturaltotem.config.TotemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotemCommandTest {

    /** Recoge las respuestas del comando para poder comprobarlas. */
    private static final class Recorder implements TotemCommand.Feedback {
        final List<String> lines = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public void info(String line) {
            lines.add(line);
        }

        @Override
        public void success(String line) {
            lines.add(line);
        }

        @Override
        public void error(String line) {
            lines.add(line);
            errors.add(line);
        }

        String text() {
            return String.join("\n", lines);
        }
    }

    private TotemConfig config;
    private ConfigStore.InMemory store;
    private TotemCommand command;
    private Recorder out;

    @BeforeEach
    void setUp() {
        config = new TotemConfig();
        store = new ConfigStore.InMemory(config);
        command = new TotemCommand(config, store);
        out = new Recorder();
    }

    private void run(String line) {
        List<String> words = TotemCommand.split(line);
        command.run(words.toArray(new String[0]), out);
    }

    @Test
    void recognizesItsOwnCommandAndIgnoresTheRest() {
        assertTrue(command.handle("ntotem status", out));
        assertTrue(command.handle("/nt status", out));
        assertTrue(command.handle("NATOTEM status", out));
        assertFalse(command.handle("gamemode creative", out));
        assertFalse(command.handle("", out));
        assertFalse(command.handle(null, out));
    }

    @Test
    void setChangesTheValueSavesItAndReportsIt() {
        run("set move-ms 80");
        assertEquals(80L, config.get(Options.MOVE_MS));
        assertEquals(1, store.saves());
        assertTrue(out.text().contains("60 → 80"), out.text());
        assertTrue(out.errors.isEmpty());
    }

    @Test
    void theShorthandFormWorksToo() {
        run("hotbar-slot 1");
        assertEquals(1, config.get(Options.HOTBAR_SLOT));
        run("move_ms 10");
        assertEquals(10L, config.get(Options.MOVE_MS));
    }

    @Test
    void anInvalidValueChangesNothingAndExplainsWhy() {
        run("set hotbar-slot 42");
        assertEquals(9, config.get(Options.HOTBAR_SLOT));
        assertEquals(0, store.saves());
        assertEquals(1, out.errors.size());
        assertTrue(out.errors.getFirst().contains("1-9"), out.errors.toString());
    }

    @Test
    void anUnknownOptionSuggestsSomethingSimilar() {
        run("set hotbar 3");
        assertEquals(1, out.errors.size());
        assertTrue(out.text().contains("hotbar-slot"), out.text());
    }

    @Test
    void anUnknownCommandPointsToHelp() {
        run("despegar");
        assertEquals(1, out.errors.size());
        assertTrue(out.text().contains("help"), out.text());
    }

    @Test
    void missingArgumentsAreReported() {
        run("set move-ms");
        assertTrue(out.errors.getFirst().contains("Falta el valor"), out.errors.toString());
        out.errors.clear();
        run("set");
        assertTrue(out.errors.getFirst().contains("Uso"), out.errors.toString());
        out.errors.clear();
        run("get");
        assertTrue(out.errors.getFirst().contains("Uso"), out.errors.toString());
    }

    @Test
    void onOffAndToggleFollowEachOther() {
        run("off");
        assertFalse(config.enabled());
        run("toggle");
        assertTrue(config.enabled());
        run("set enabled toggle");
        assertFalse(config.enabled());
        run("on");
        assertTrue(config.enabled());
        assertTrue(out.errors.isEmpty());
        assertEquals(4, store.saves());
    }

    @Test
    void resetRestoresOneOptionOrAllOfThem() {
        run("set move-ms 500");
        run("set debug true");
        run("reset move-ms");
        assertEquals(60L, config.get(Options.MOVE_MS));
        assertTrue(config.debug());

        run("reset");
        assertTrue(config.isAllDefault());
        assertTrue(out.errors.isEmpty());
    }

    @Test
    void listShowsEveryOptionAndMarksTheChangedOnes() {
        run("set move-ms 120");
        out.lines.clear();
        run("list");
        String text = out.text();
        for (var option : Options.ALL) {
            assertTrue(text.contains(option.key() + " = "), "falta " + option.key());
        }
        assertTrue(text.contains("move-ms = 120 *"), text);

        out.lines.clear();
        run("list fill");
        assertTrue(out.text().contains("fill-hotbar"));
        assertFalse(out.text().contains("move-ms = "));

        out.lines.clear();
        run("list zzz");
        assertEquals(1, out.errors.size());
    }

    @Test
    void getExplainsTheOption() {
        run("get source-order");
        String text = out.text();
        assertTrue(text.contains("source-order = main-first"), text);
        assertTrue(text.contains("main-first|hotbar-first|ascending"), text);
    }

    @Test
    void statusSummarizesAndWarnsWhenNothingWouldBeFilled() {
        run("status");
        assertTrue(out.text().contains("activo"), out.text());
        assertTrue(out.text().contains("por defecto"), out.text());

        out.lines.clear();
        run("set fill-hotbar false");
        run("set fill-offhand false");
        assertFalse(out.errors.isEmpty(), "debe avisar de que no queda nada que rellenar");

        out.lines.clear();
        run("status");
        assertTrue(out.text().contains("no hay nada que rellenar"), out.text());
    }

    @Test
    void everyChangeIsSavedWithoutAskingAndReloadBringsTheFileBack() {
        run("set move-ms 300");
        assertEquals(1, store.saves(), "un cambio se guarda solo");

        // Un cambio hecho fuera del comando, como si se hubiera editado el archivo a mano.
        config.set(Options.MOVE_MS, 10L);
        run("reload");
        assertEquals(300L, config.get(Options.MOVE_MS));
        assertEquals(1, store.reloads());
        assertTrue(out.text().contains("recargada"), out.text());
    }

    @Test
    void saveReportsWhatTheStoreSays() {
        run("save");
        assertEquals(1, store.saves());
        assertTrue(out.text().contains("guardada"), out.text());
        assertTrue(out.errors.isEmpty());
    }

    @Test
    void withoutArgumentsItShowsTheStatus() {
        command.run(new String[0], out);
        assertTrue(out.text().contains("Natural Totem"), out.text());
        assertTrue(out.text().contains("help"), out.text());
    }

    @Test
    void helpListsEverySubcommand() {
        run("help");
        String text = out.text();
        for (String subcommand : List.of("status", "on", "list", "get", "set", "reset", "save", "reload")) {
            assertTrue(text.contains(subcommand), "falta " + subcommand + " en la ayuda");
        }
    }

    @Test
    void quotedValuesStayTogether() {
        assertEquals(List.of("set", "item", "minecraft:golden_apple"),
                TotemCommand.split("  set   item \"minecraft:golden_apple\"  "));
        run("set item \"minecraft:golden_apple\"");
        assertEquals("minecraft:golden_apple", config.itemId());
    }

    @Test
    void settingTheSameValueTwiceIsNotReportedAsAChange() {
        run("set move-ms 60");
        assertTrue(out.text().contains("ya valía 60"), out.text());
    }
}
