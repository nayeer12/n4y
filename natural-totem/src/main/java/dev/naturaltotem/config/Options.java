package dev.naturaltotem.config;

import dev.naturaltotem.RefillPlanner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo de ajustes de Natural Totem.
 *
 * <p>Los valores por defecto son exactamente el comportamiento del JAR 1.4.0: casilla 9,
 * mano secundaria, movimiento de cursor de 60 ms e intervalo mínimo de 100 ms entre acciones.
 * Con la configuración recién creada el mod hace lo mismo que antes.
 */
public final class Options {

    public static final Option<Boolean> ENABLED = Option.ofBoolean("enabled", true,
            "Activa o desactiva el rellenado automático.");

    public static final Option<Integer> HOTBAR_SLOT = Option.ofInt("hotbar-slot", 9, 1, 9,
            "Casilla de la barra rápida que se mantiene con tótem (1-9).");

    public static final Option<Boolean> FILL_HOTBAR = Option.ofBoolean("fill-hotbar", true,
            "Rellena la casilla de la barra rápida.");

    public static final Option<Boolean> FILL_OFFHAND = Option.ofBoolean("fill-offhand", true,
            "Rellena la mano secundaria.");

    public static final Option<Boolean> OFFHAND_FIRST = Option.ofBoolean("offhand-first", false,
            "Atiende primero la mano secundaria y después la barra rápida.");

    public static final Option<RefillPlanner.SourceOrder> SOURCE_ORDER =
            Option.ofEnum("source-order", RefillPlanner.SourceOrder.MAIN_FIRST,
                    "Orden de búsqueda del tótem de origen: inventario principal, barra rápida o índice ascendente.");

    public static final Option<Boolean> SELECT_SLOT = Option.ofBoolean("select-slot", true,
            "Selecciona la casilla configurada cuando ya tiene un tótem.");

    public static final Option<Long> MOVE_MS = Option.ofLong("move-ms", 60L, 0L, 5000L,
            "Duración del acercamiento del cursor a la casilla, en milisegundos (0 = inmediato).",
            List.of("0", "60", "120", "250"));

    public static final Option<Long> ACTION_INTERVAL_MS = Option.ofLong("action-interval-ms", 100L, 0L, 5000L,
            "Intervalo mínimo entre acciones, en milisegundos.",
            List.of("0", "100", "200", "500"));

    public static final Option<Boolean> MOVE_CURSOR = Option.ofBoolean("move-cursor", true,
            "Mueve el cursor real del ratón hasta la casilla antes del intercambio.");

    public static final Option<Boolean> REQUIRE_FOCUS = Option.ofBoolean("require-focus", true,
            "Cancela la pasada si la ventana del juego pierde el foco.");

    public static final Option<Boolean> SKIP_WHILE_CLICKING = Option.ofBoolean("skip-while-clicking", true,
            "Espera mientras mantienes pulsado un botón del ratón.");

    public static final Option<String> ITEM = Option.ofString("item", "minecraft:totem_of_undying",
            "[a-z0-9_.-]+(:[a-z0-9_./-]+)?", "id de objeto",
            "Objeto que se mantiene en la casilla y la mano secundaria.",
            List.of("minecraft:totem_of_undying", "minecraft:golden_apple", "minecraft:enchanted_golden_apple"));

    public static final Option<Boolean> ANNOUNCE = Option.ofBoolean("announce", false,
            "Avisa en el chat cada vez que se rellena una casilla.");

    public static final Option<Boolean> DEBUG = Option.ofBoolean("debug", false,
            "Escribe en el registro del juego por qué se cancela o se omite una pasada.");

    /** Todas las opciones, en el orden en que se muestran y se guardan. */
    public static final List<Option<?>> ALL = List.of(
            ENABLED,
            HOTBAR_SLOT,
            FILL_HOTBAR,
            FILL_OFFHAND,
            OFFHAND_FIRST,
            SOURCE_ORDER,
            SELECT_SLOT,
            MOVE_MS,
            ACTION_INTERVAL_MS,
            MOVE_CURSOR,
            REQUIRE_FOCUS,
            SKIP_WHILE_CLICKING,
            ITEM,
            ANNOUNCE,
            DEBUG);

    private static final Map<String, Option<?>> BY_KEY = index();

    private Options() {
    }

    private static Map<String, Option<?>> index() {
        Map<String, Option<?>> byKey = new LinkedHashMap<>();
        for (Option<?> option : ALL) {
            if (byKey.put(option.key(), option) != null) {
                throw new IllegalStateException("Duplicate option key: " + option.key());
            }
        }
        return Map.copyOf(byKey);
    }

    /**
     * Busca una opción por clave. Acepta mayúsculas y da igual si se escribe con guiones,
     * guiones bajos o puntos, para que {@code move_ms}, {@code move.ms} y {@code move-ms} valgan igual.
     */
    public static Optional<Option<?>> byKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        Option<?> exact = BY_KEY.get(key);
        if (exact != null) {
            return Optional.of(exact);
        }
        String wanted = normalize(key);
        for (Option<?> option : ALL) {
            if (normalize(option.key()).equals(wanted)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }

    /** Claves que empiezan por el prefijo dado, para el autocompletado. */
    public static List<String> keysStartingWith(String prefix) {
        String wanted = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();
        for (Option<?> option : ALL) {
            if (option.key().startsWith(wanted)) {
                keys.add(option.key());
            }
        }
        return keys;
    }

    static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', '-').replace('.', '-');
    }
}
