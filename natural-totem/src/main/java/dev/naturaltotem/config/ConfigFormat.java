package dev.naturaltotem.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Formato del archivo de configuración: una línea {@code clave=valor} por ajuste,
 * con un comentario encima que explica qué hace y qué valores acepta.
 *
 * <p>Es texto plano a propósito: se puede editar a mano con cualquier editor y no añade
 * ninguna dependencia al mod.
 */
public final class ConfigFormat {

    private ConfigFormat() {
    }

    /** Líneas completas del archivo, comentarios incluidos. */
    public static List<String> write(TotemConfig config) {
        List<String> lines = new ArrayList<>();
        lines.add("# Natural Totem — configuración");
        lines.add("# Se puede editar a mano y recargar en el juego con /ntotem reload.");
        lines.add("# Las claves desconocidas se ignoran con un aviso en el registro.");
        for (Option<?> option : Options.ALL) {
            lines.add("");
            lines.add("# " + option.description());
            lines.add("# valores: " + option.valueHint() + " — por defecto: " + option.printUnchecked(option.defaultValue()));
            lines.add(option.key() + "=" + config.printed(option));
        }
        return lines;
    }

    /**
     * Aplica las líneas de un archivo sobre la configuración. Las claves que falten
     * conservan su valor actual; las inválidas se avisan y se dejan como estaban.
     *
     * @return avisos legibles, vacío si todo el archivo era válido
     */
    public static List<String> read(List<String> lines, TotemConfig config) {
        List<String> warnings = new ArrayList<>();
        int number = 0;
        for (String raw : lines) {
            number++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator < 0) {
                separator = line.indexOf(':');
            }
            if (separator <= 0) {
                warnings.add("línea " + number + ": se esperaba clave=valor, se encontró \"" + line + "\"");
                continue;
            }
            String key = line.substring(0, separator).strip();
            String value = line.substring(separator + 1).strip();
            Option<?> option = Options.byKey(key).orElse(null);
            if (option == null) {
                warnings.add("línea " + number + ": ajuste desconocido \"" + key + "\"");
                continue;
            }
            try {
                config.setParsed(option, value);
            } catch (IllegalArgumentException error) {
                warnings.add("línea " + number + ": " + option.key() + " — " + error.getMessage());
            }
        }
        return warnings;
    }
}
