package dev.naturaltotem.config;

import dev.naturaltotem.RefillPlanner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Grupos de ajustes con nombre, para no tener que escribir seis órdenes seguidas.
 *
 * <p>{@code classic} es la forma de moverse y de actuar del JAR 1.4.0, y existe porque los
 * valores por defecto de la 1.6.0 ya no lo son: ahora el cursor se curva, la duración varía y el
 * mod sigue vigilando con el inventario abierto.
 *
 * <p>Un grupo solo toca cómo se mueve y cuándo actúa. Lo que es una preferencia de quien juega
 * —la casilla, el objeto, qué se rellena, los avisos en el chat— se queda como esté: aplicar un
 * grupo no debe deshacer lo que se configuró antes.
 */
public final class Presets {

    /** Los ajustes que un grupo decide. El resto no se toca nunca. */
    public static final List<Option<?>> SCOPE = List.of(
            Options.SOURCE_ORDER,
            Options.CONTINUOUS,
            Options.RETRY_MS,
            Options.MOVE_MS_MIN,
            Options.MOVE_MS_MAX,
            Options.ACTION_INTERVAL_MS_MIN,
            Options.ACTION_INTERVAL_MS_MAX,
            Options.MOVE_CURSOR,
            Options.CURVE,
            Options.AIM_JITTER);

    /** Un grupo de ajustes con nombre. */
    public record Preset(String name, String description, Map<Option<?>, Object> values) {
    }

    public static final Preset DEFAULT = new Preset("default",
            "Lo de esta versión: busca el tótem más cercano, el cursor se curva, la duración varía "
                    + "y sigue vigilando con el inventario abierto.",
            defaults());

    public static final Preset CLASSIC = new Preset("classic",
            "Como el JAR 1.4.0: una pasada por apertura, 60 ms fijos y cursor recto al centro.",
            classic());

    public static final List<Preset> ALL = List.of(DEFAULT, CLASSIC);

    private Presets() {
    }

    private static Map<Option<?>, Object> defaults() {
        Map<Option<?>, Object> values = new LinkedHashMap<>();
        for (Option<?> option : SCOPE) {
            values.put(option, option.defaultValue());
        }
        return Map.copyOf(values);
    }

    private static Map<Option<?>, Object> classic() {
        Map<Option<?>, Object> values = new LinkedHashMap<>();
        values.put(Options.SOURCE_ORDER, RefillPlanner.SourceOrder.MAIN_FIRST);
        values.put(Options.CONTINUOUS, false);
        values.put(Options.RETRY_MS, Options.RETRY_MS.defaultValue());
        values.put(Options.MOVE_MS_MIN, 60L);
        values.put(Options.MOVE_MS_MAX, 60L);
        values.put(Options.ACTION_INTERVAL_MS_MIN, 100L);
        values.put(Options.ACTION_INTERVAL_MS_MAX, 100L);
        values.put(Options.MOVE_CURSOR, true);
        values.put(Options.CURVE, 0);
        values.put(Options.AIM_JITTER, 0);
        if (values.size() != SCOPE.size()) {
            throw new IllegalStateException("A preset must decide every option of the scope");
        }
        return Map.copyOf(values);
    }

    public static Optional<Preset> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(preset -> preset.name().equals(wanted)).findFirst();
    }

    public static List<String> names() {
        return ALL.stream().map(Preset::name).toList();
    }

    /** @return {@code true} si el mod se mueve y actúa como dice este grupo */
    public static boolean matches(TotemConfig config, Preset preset) {
        return preset.values().entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(config.get(cast(entry.getKey()))));
    }

    @SuppressWarnings("unchecked")
    private static Option<Object> cast(Option<?> option) {
        return (Option<Object>) option;
    }
}
