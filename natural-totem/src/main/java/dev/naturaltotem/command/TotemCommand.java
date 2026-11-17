package dev.naturaltotem.command;

import dev.naturaltotem.config.ConfigStore;
import dev.naturaltotem.config.Option;
import dev.naturaltotem.config.Options;
import dev.naturaltotem.config.Presets;
import dev.naturaltotem.config.TotemConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * El comando del mod, sin nada de Minecraft dentro.
 *
 * <p>Recibe las palabras ya separadas y escribe el resultado en {@link Feedback}. La capa del
 * juego solo se encarga de pintar esas líneas en el chat, de modo que todo el comportamiento
 * del comando se puede probar sin abrir el juego.
 */
public final class TotemCommand {

    /** Nombre principal y alias aceptados, sin la barra inicial. */
    public static final String NAME = "ntotem";
    public static final List<String> ALIASES = List.of("ntotem", "natotem", "naturaltotem", "nt");

    /** Destino de las respuestas del comando. */
    public interface Feedback {
        void info(String line);

        void success(String line);

        void error(String line);

        /** Encabezado de una lista; por defecto se muestra como información. */
        default void header(String line) {
            info(line);
        }
    }

    private final TotemConfig config;
    private final ConfigStore store;

    public TotemCommand(TotemConfig config, ConfigStore store) {
        this.config = Objects.requireNonNull(config, "config");
        this.store = Objects.requireNonNull(store, "store");
    }

    /** @return {@code true} si el texto era un comando de este mod y ya se ha atendido */
    public boolean handle(String rawCommand, Feedback out) {
        List<String> words = split(rawCommand);
        if (words.isEmpty()) {
            return false;
        }
        String name = words.getFirst().toLowerCase(Locale.ROOT);
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (!ALIASES.contains(name)) {
            return false;
        }
        run(words.subList(1, words.size()).toArray(new String[0]), out);
        return true;
    }

    /** Ejecuta el comando ya sin su nombre. */
    public void run(String[] args, Feedback out) {
        if (args.length == 0) {
            status(out);
            out.info("Escribe /" + NAME + " help para ver todas las órdenes.");
            return;
        }
        String command = args[0].toLowerCase(Locale.ROOT);
        String[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
        switch (command) {
            case "help", "ayuda", "?" -> help(out);
            case "status", "estado" -> status(out);
            case "on", "enable" -> toggle(out, Boolean.TRUE);
            case "off", "disable" -> toggle(out, Boolean.FALSE);
            case "toggle" -> toggle(out, null);
            case "list", "lista" -> list(rest.length > 0 ? rest[0] : null, out);
            case "get", "ver" -> get(rest, out);
            case "set", "poner" -> set(rest, out);
            case "reset", "default", "defaults" -> reset(rest, out);
            case "preset", "perfil" -> preset(rest, out);
            case "save", "guardar" -> out.success(store.save());
            case "reload", "recargar" -> out.success(store.reload());
            default -> shorthand(args, out);
        }
    }

    // --- órdenes ---------------------------------------------------------

    private void help(Feedback out) {
        out.header("Natural Totem — /" + NAME + " (alias: " + String.join(", ", ALIASES.subList(1, ALIASES.size())) + ")");
        out.info("/" + NAME + " status — resumen del estado actual");
        out.info("/" + NAME + " on | off | toggle — activa o desactiva el mod");
        out.info("/" + NAME + " list [texto] — lista los ajustes y sus valores");
        out.info("/" + NAME + " get <ajuste> — muestra un ajuste y qué acepta");
        out.info("/" + NAME + " set <ajuste> <valor> — cambia un ajuste (también vale /" + NAME + " <ajuste> <valor>)");
        out.info("/" + NAME + " reset [ajuste|all] — vuelve a los valores por defecto");
        out.info("/" + NAME + " preset <" + String.join("|", Presets.names())
                + "> — cómo se mueve y cuándo actúa (classic = como el 1.4.0)");
        out.info("/" + NAME + " save | reload — guarda o vuelve a leer " + store.location());
        out.info("Al arrancar también se leen -Dnaturaltotem.<ajuste>=<valor> y NATURAL_TOTEM_<AJUSTE>.");
    }

    private void status(Feedback out) {
        out.header("Natural Totem: " + (config.enabled() ? "activo" : "desactivado"));
        String objetivo = config.itemId();
        String casilla = config.get(Options.FILL_HOTBAR) ? "casilla " + config.get(Options.HOTBAR_SLOT) : "casilla no";
        String mano = config.get(Options.FILL_OFFHAND) ? "mano secundaria sí" : "mano secundaria no";
        out.info("Objeto: " + objetivo + " — " + casilla + ", " + mano
                + (config.get(Options.OFFHAND_FIRST) ? " (mano primero)" : ""));
        out.info("Origen: " + config.printed(Options.SOURCE_ORDER)
                + (config.continuous()
                        ? " — sigue vigilando con el inventario abierto"
                        : " — una pasada por apertura"));
        out.info("Tiempos: cursor " + range(Options.MOVE_MS_MIN, Options.MOVE_MS_MAX) + " ms, intervalo "
                + range(Options.ACTION_INTERVAL_MS_MIN, Options.ACTION_INTERVAL_MS_MAX) + " ms"
                + (config.moveCursor() ? "" : ", sin mover el cursor"));
        for (String warning : config.warnings()) {
            out.error(warning);
        }
        for (Presets.Preset preset : Presets.ALL) {
            if (Presets.matches(config, preset)) {
                out.info("Se mueve como el grupo \"" + preset.name() + "\": " + preset.description());
                break;
            }
        }
        List<String> changed = new ArrayList<>();
        for (Option<?> option : Options.ALL) {
            if (!config.isDefault(option)) {
                changed.add(option.key() + "=" + config.printed(option));
            }
        }
        if (changed.isEmpty()) {
            out.info("Todos los ajustes están en su valor por defecto (comportamiento de la versión 1.4.0).");
        } else {
            out.info("Cambiados: " + String.join(", ", changed));
        }
    }

    private void toggle(Feedback out, Boolean target) {
        boolean value = target != null ? target : !config.enabled();
        config.set(Options.ENABLED, value);
        store.autoSave();
        out.success("Natural Totem " + (value ? "activado" : "desactivado") + ".");
    }

    private void list(String filter, Feedback out) {
        String wanted = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        out.header("Ajustes de Natural Totem" + (wanted.isEmpty() ? "" : " que contienen \"" + wanted + "\""));
        int shown = 0;
        for (Option<?> option : Options.ALL) {
            if (!wanted.isEmpty()
                    && !option.key().contains(wanted)
                    && !option.description().toLowerCase(Locale.ROOT).contains(wanted)) {
                continue;
            }
            shown++;
            String mark = config.isDefault(option) ? "" : " *";
            out.info(option.key() + " = " + config.printed(option) + mark + "  (" + option.valueHint() + ")");
        }
        if (shown == 0) {
            out.error("Ningún ajuste coincide con \"" + filter + "\".");
        } else if (!config.isAllDefault()) {
            out.info("* = distinto del valor por defecto.");
        }
    }

    private void get(String[] args, Feedback out) {
        if (args.length == 0) {
            out.error("Uso: /" + NAME + " get <ajuste>");
            return;
        }
        Option<?> option = resolve(args[0], out);
        if (option == null) {
            return;
        }
        out.header(option.key() + " = " + config.printed(option));
        out.info(option.description());
        out.info("Valores: " + option.valueHint() + " — por defecto: " + option.printUnchecked(option.defaultValue()));
    }

    private void set(String[] args, Feedback out) {
        if (args.length == 0) {
            out.error("Uso: /" + NAME + " set <ajuste> <valor>");
            return;
        }
        Option<?> option = resolve(args[0], out);
        if (option == null) {
            return;
        }
        if (args.length < 2) {
            out.error("Falta el valor para " + option.key() + " (" + option.valueHint() + ").");
            return;
        }
        String value = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        assign(option, value, out);
    }

    /** Permite escribir directamente {@code /ntotem move-ms 80}. */
    private void shorthand(String[] args, Feedback out) {
        Option<?> option = Options.byKey(args[0]).orElse(null);
        if (option == null) {
            out.error("No conozco la orden \"" + args[0] + "\". Escribe /" + NAME + " help.");
            return;
        }
        if (args.length == 1) {
            get(args, out);
            return;
        }
        assign(option, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)), out);
    }

    private void assign(Option<?> option, String value, Feedback out) {
        if (option.equals(Options.ENABLED) && value.equalsIgnoreCase("toggle")) {
            toggle(out, null);
            return;
        }
        String previous = config.printed(option);
        try {
            config.setParsed(option, value);
        } catch (IllegalArgumentException error) {
            out.error(option.key() + ": " + error.getMessage());
            return;
        }
        store.autoSave();
        String now = config.printed(option);
        if (now.equals(previous)) {
            out.info(option.key() + " ya valía " + now + ".");
        } else {
            out.success(option.key() + ": " + previous + " → " + now);
        }
        for (String warning : config.warnings()) {
            out.error("Aviso: " + warning);
        }
    }

    private void preset(String[] args, Feedback out) {
        if (args.length == 0) {
            out.error("Uso: /" + NAME + " preset <" + String.join("|", Presets.names()) + ">");
            for (Presets.Preset preset : Presets.ALL) {
                out.info(preset.name() + " — " + preset.description());
            }
            return;
        }
        Presets.Preset preset = Presets.byName(args[0]).orElse(null);
        if (preset == null) {
            out.error("No existe el grupo \"" + args[0] + "\". Hay: " + String.join(", ", Presets.names()) + ".");
            return;
        }
        config.applyAll(preset.values());
        store.autoSave();
        out.success("Aplicado el grupo \"" + preset.name() + "\": " + preset.description());
        out.info("Solo cambia cómo se mueve y cuándo actúa; la casilla, el objeto y los avisos se quedan como están.");
    }

    private String range(Option<Long> min, Option<Long> max) {
        long low = config.get(min);
        long high = config.get(max);
        return high > low ? low + "-" + high : Long.toString(low);
    }

    private void reset(String[] args, Feedback out) {
        if (args.length == 0 || args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("todo")) {
            config.resetAll();
            store.autoSave();
            out.success("Todos los ajustes han vuelto a sus valores por defecto.");
            return;
        }
        Option<?> option = resolve(args[0], out);
        if (option == null) {
            return;
        }
        String previous = config.printed(option);
        config.reset(option);
        store.autoSave();
        out.success(option.key() + ": " + previous + " → " + config.printed(option) + " (por defecto)");
    }

    private Option<?> resolve(String key, Feedback out) {
        Option<?> option = Options.byKey(key).orElse(null);
        if (option != null) {
            return option;
        }
        out.error("No existe el ajuste \"" + key + "\".");
        List<String> similar = new ArrayList<>();
        String wanted = key.toLowerCase(Locale.ROOT);
        for (Option<?> candidate : Options.ALL) {
            if (candidate.key().contains(wanted) || wanted.contains(candidate.key())) {
                similar.add(candidate.key());
            }
        }
        out.info(similar.isEmpty()
                ? "Escribe /" + NAME + " list para ver los ajustes."
                : "¿Querías decir " + String.join(", ", similar) + "?");
        return null;
    }

    /** Separa el texto en palabras respetando las comillas dobles. */
    public static List<String> split(String rawCommand) {
        List<String> words = new ArrayList<>();
        if (rawCommand == null) {
            return words;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean started = false;
        for (int index = 0; index < rawCommand.length(); index++) {
            char character = rawCommand.charAt(index);
            if (character == '"') {
                quoted = !quoted;
                started = true;
            } else if (Character.isWhitespace(character) && !quoted) {
                if (started) {
                    words.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
            } else {
                current.append(character);
                started = true;
            }
        }
        if (started) {
            words.add(current.toString());
        }
        return words;
    }
}
