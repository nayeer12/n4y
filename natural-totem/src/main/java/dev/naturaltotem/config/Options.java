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
 * <p>Los valores por defecto buscan un movimiento corto y variable y que el mod siga
 * rellenando mientras el inventario esté abierto. El comportamiento exacto del JAR 1.4.0
 * —una pasada por apertura, 60 ms fijos, cursor recto al centro— se recupera con
 * {@code /ntotem preset classic}.
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
            Option.ofEnum("source-order", RefillPlanner.SourceOrder.NEAREST_MAIN,
                    "De dónde sale el tótem: el más cercano del inventario (nearest-main), el más cercano "
                            + "de cualquier sitio (nearest), o por índice (main-first, hotbar-first, ascending).");

    public static final Option<Boolean> SELECT_SLOT = Option.ofBoolean("select-slot", true,
            "Selecciona la casilla configurada cuando ya tiene un tótem.");

    public static final Option<Boolean> CONTINUOUS = Option.ofBoolean("continuous", true,
            "Sigue vigilando con el inventario abierto: si el tótem estalla, lo repone sin cerrarlo.");

    public static final Option<Long> RETRY_MS = Option.ofLong("retry-ms", 200L, 0L, 5000L,
            "Espera antes de reintentar cuando un intento se queda a medias, en milisegundos.",
            List.of("100", "200", "500"));

    public static final Option<Long> MOVE_MS_MIN = Option.ofLong("move-ms-min", 50L, 0L, 5000L,
            "Duración mínima del acercamiento del cursor, en milisegundos.",
            List.of("0", "50", "60", "120"), List.of("move-ms"));

    public static final Option<Long> MOVE_MS_MAX = Option.ofLong("move-ms-max", 100L, 0L, 5000L,
            "Duración máxima del acercamiento; cada movimiento sortea un tiempo entre el mínimo y este.",
            List.of("60", "100", "150", "250"));

    public static final Option<Long> ACTION_INTERVAL_MS_MIN = Option.ofLong("action-interval-ms-min", 100L, 0L, 5000L,
            "Espera mínima entre acciones, en milisegundos.",
            List.of("0", "100", "200"), List.of("action-interval-ms"));

    public static final Option<Long> ACTION_INTERVAL_MS_MAX = Option.ofLong("action-interval-ms-max", 100L, 0L, 5000L,
            "Espera máxima entre acciones; cada acción sortea un tiempo entre el mínimo y este.",
            List.of("100", "150", "300"));

    public static final Option<Boolean> MOVE_CURSOR = Option.ofBoolean("move-cursor", true,
            "Mueve el cursor real del ratón hasta la casilla antes del intercambio.");

    public static final Option<Integer> CURVE = Option.ofInt("curve", 12, 0, 50,
            "Cuánto se curva el recorrido del cursor respecto a la línea recta, en por ciento (0 = recto).");

    public static final Option<Integer> AIM_JITTER = Option.ofInt("aim-jitter", 3, 0, 7,
            "Cuánto puede desviarse el punto de llegada del centro de la casilla, en píxeles (0 = al centro).");

    public static final Option<Boolean> REQUIRE_FOCUS = Option.ofBoolean("require-focus", true,
            "Deja de actuar mientras la ventana del juego no tenga el foco.");

    public static final Option<Boolean> SKIP_WHILE_CLICKING = Option.ofBoolean("skip-while-clicking", true,
            "Espera mientras mantienes pulsado un botón del ratón.");

    public static final Option<String> ITEM = Option.ofString("item", "minecraft:totem_of_undying",
            "[a-z0-9_.-]+(:[a-z0-9_./-]+)?", "id de objeto",
            "Objeto que se mantiene en la casilla y la mano secundaria.",
            List.of("minecraft:totem_of_undying", "minecraft:golden_apple", "minecraft:enchanted_golden_apple"));

    public static final Option<Boolean> ANNOUNCE = Option.ofBoolean("announce", false,
            "Avisa en el chat cada vez que se rellena una casilla.");

    public static final Option<Boolean> DEBUG = Option.ofBoolean("debug", false,
            "Escribe en el registro del juego por qué se cancela o se omite un intento.");

    /** Todas las opciones, en el orden en que se muestran y se guardan. */
    public static final List<Option<?>> ALL = List.of(
            ENABLED,
            HOTBAR_SLOT,
            FILL_HOTBAR,
            FILL_OFFHAND,
            OFFHAND_FIRST,
            SOURCE_ORDER,
            SELECT_SLOT,
            CONTINUOUS,
            RETRY_MS,
            MOVE_MS_MIN,
            MOVE_MS_MAX,
            ACTION_INTERVAL_MS_MIN,
            ACTION_INTERVAL_MS_MAX,
            MOVE_CURSOR,
            CURVE,
            AIM_JITTER,
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
            for (String alias : option.aliases()) {
                if (byKey.put(alias, option) != null) {
                    throw new IllegalStateException("Duplicate option key: " + alias);
                }
            }
        }
        return Map.copyOf(byKey);
    }

    /**
     * Busca una opción por clave. Acepta mayúsculas, da igual si se escribe con guiones,
     * guiones bajos o puntos, y reconoce los nombres antiguos de los ajustes que se han
     * renombrado, para que un archivo de configuración viejo se siga leyendo.
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
        for (Map.Entry<String, Option<?>> entry : BY_KEY.entrySet()) {
            if (normalize(entry.getKey()).equals(wanted)) {
                return Optional.of(entry.getValue());
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
