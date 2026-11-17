package dev.naturaltotem.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StartupOverridesTest {

    private static Map<String, String> map(String... pairs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
        return values;
    }

    @Test
    void appliesOnePropertyPerOption() {
        TotemConfig config = new TotemConfig();
        StartupOverrides.Result result = StartupOverrides.apply(config,
                map("naturaltotem.move-ms", "80", "naturaltotem.hotbar-slot", "1", "java.version", "21"),
                Map.of());

        assertEquals(80L, config.get(Options.MOVE_MS_MIN));
        assertEquals(1, config.get(Options.HOTBAR_SLOT));
        assertEquals(2, result.applied().size(), result.applied().toString());
        assertEquals(0, result.warnings().size());
    }

    @Test
    void appliesSeveralOptionsInOneProperty() {
        TotemConfig config = new TotemConfig();
        StartupOverrides.apply(config,
                map("naturaltotem.options", "enabled=false, move-ms=0 ; source-order=ascending"),
                Map.of());

        assertFalse(config.enabled());
        assertEquals(0L, config.get(Options.MOVE_MS_MIN));
        assertEquals(dev.naturaltotem.RefillPlanner.SourceOrder.ASCENDING, config.get(Options.SOURCE_ORDER));
    }

    @Test
    void environmentVariablesWorkTooAndPropertiesWinOverThem() {
        TotemConfig config = new TotemConfig();
        StartupOverrides.apply(config,
                map("naturaltotem.hotbar-slot", "2"),
                map("NATURAL_TOTEM_HOTBAR_SLOT", "5", "NATURAL_TOTEM_DEBUG", "true", "PATH", "/usr/bin"));

        assertEquals(2, config.get(Options.HOTBAR_SLOT), "la propiedad de la línea de órdenes manda");
        assertTrue(config.debug());
    }

    @Test
    void unknownOrInvalidValuesAreWarnedAndIgnored() {
        TotemConfig config = new TotemConfig();
        StartupOverrides.Result result = StartupOverrides.apply(config,
                map("naturaltotem.casilla", "3", "naturaltotem.hotbar-slot", "99",
                        "naturaltotem.options", "move-ms"),
                Map.of());

        assertTrue(config.isAllDefault(), "nada inválido debe cambiar la configuración");
        assertEquals(3, result.warnings().size(), result.warnings().toString());
        assertTrue(result.applied().isEmpty());
    }

    @Test
    void readingTheProcessWithoutOverridesChangesNothing() {
        TotemConfig config = new TotemConfig();
        StartupOverrides.Result result = StartupOverrides.applyFromProcess(config);
        assertTrue(config.isAllDefault());
        assertTrue(result.isEmpty(), result.applied() + " " + result.warnings());
    }
}
