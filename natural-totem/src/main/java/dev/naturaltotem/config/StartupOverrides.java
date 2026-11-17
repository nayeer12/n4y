package dev.naturaltotem.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ajustes pasados en la línea de órdenes al arrancar el juego.
 *
 * <p>Tres formas, todas equivalentes:
 * <ul>
 *   <li>{@code -Dnaturaltotem.move-ms=80} (una propiedad por ajuste),</li>
 *   <li>{@code -Dnaturaltotem.options=move-ms=80,hotbar-slot=1} (varios de una vez),</li>
 *   <li>{@code NATURAL_TOTEM_MOVE_MS=80} o {@code NATURAL_TOTEM_OPTIONS=...} (variables de entorno).</li>
 * </ul>
 *
 * <p>Se aplican después de leer el archivo, así que mandan sobre él, pero no lo sobrescriben:
 * son para una partida concreta o para un script de arranque.
 */
public final class StartupOverrides {

    public static final String PROPERTY_PREFIX = "naturaltotem.";
    public static final String PROPERTY_BUNDLE = "naturaltotem.options";
    public static final String ENVIRONMENT_PREFIX = "NATURAL_TOTEM_";
    public static final String ENVIRONMENT_BUNDLE = "NATURAL_TOTEM_OPTIONS";

    private StartupOverrides() {
    }

    /** Resultado de aplicar los parámetros de arranque. */
    public record Result(List<String> applied, List<String> warnings) {
        public boolean isEmpty() {
            return applied.isEmpty() && warnings.isEmpty();
        }
    }

    /** Usa las propiedades del sistema y el entorno del proceso. */
    public static Result applyFromProcess(TotemConfig config) {
        Map<String, String> properties = new java.util.LinkedHashMap<>();
        System.getProperties().stringPropertyNames()
                .forEach(name -> properties.put(name, System.getProperty(name)));
        return apply(config, properties, System.getenv());
    }

    public static Result apply(TotemConfig config, Map<String, String> properties, Map<String, String> environment) {
        List<String> applied = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        applyBundle(config, environment.get(ENVIRONMENT_BUNDLE), ENVIRONMENT_BUNDLE, applied, warnings);
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(ENVIRONMENT_PREFIX) || name.equals(ENVIRONMENT_BUNDLE)) {
                continue;
            }
            applySingle(config, name.substring(ENVIRONMENT_PREFIX.length()), entry.getValue(), name, applied, warnings);
        }

        applyBundle(config, properties.get(PROPERTY_BUNDLE), PROPERTY_BUNDLE, applied, warnings);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = entry.getKey();
            if (!name.toLowerCase(Locale.ROOT).startsWith(PROPERTY_PREFIX) || name.equals(PROPERTY_BUNDLE)) {
                continue;
            }
            applySingle(config, name.substring(PROPERTY_PREFIX.length()), entry.getValue(), name, applied, warnings);
        }
        return new Result(applied, warnings);
    }

    private static void applyBundle(TotemConfig config, String bundle, String origin,
                                    List<String> applied, List<String> warnings) {
        if (bundle == null || bundle.isBlank()) {
            return;
        }
        for (String pair : bundle.split("[,;]")) {
            String entry = pair.strip();
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                warnings.add(origin + ": se esperaba clave=valor, se encontró \"" + entry + "\"");
                continue;
            }
            applySingle(config, entry.substring(0, separator), entry.substring(separator + 1), origin, applied, warnings);
        }
    }

    private static void applySingle(TotemConfig config, String key, String value, String origin,
                                    List<String> applied, List<String> warnings) {
        Option<?> option = Options.byKey(key.strip()).orElse(null);
        if (option == null) {
            warnings.add(origin + ": ajuste desconocido \"" + key.strip() + "\"");
            return;
        }
        String text = value == null ? "" : value.strip();
        try {
            config.setParsed(option, text);
            applied.add(option.key() + "=" + config.printed(option) + " (" + origin + ")");
        } catch (IllegalArgumentException error) {
            warnings.add(origin + ": " + option.key() + " — " + error.getMessage());
        }
    }
}
