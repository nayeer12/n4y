package dev.naturaltotem.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Un ajuste con tipo: sabe leerse, imprimirse, analizarse y validarse.
 *
 * <p>Las opciones se declaran una sola vez en {@link Options}. Todo lo que trabaja con
 * ajustes (el archivo de configuración, el comando, los parámetros de arranque) recorre
 * esa lista, así que añadir un ajuste nuevo no obliga a tocar ningún otro archivo.
 */
public final class Option<T> {

    /** Error de valor inválido con un mensaje pensado para mostrarse al jugador. */
    public static final class InvalidValueException extends IllegalArgumentException {
        public InvalidValueException(String message) {
            super(message);
        }
    }

    private final String key;
    private final String description;
    private final T defaultValue;
    private final String valueHint;
    private final List<String> suggestions;
    private final Function<String, T> parser;
    private final Function<T, String> printer;

    private Option(String key, String description, T defaultValue, String valueHint,
                   List<String> suggestions, Function<String, T> parser, Function<T, String> printer) {
        this.key = Objects.requireNonNull(key, "key");
        this.description = Objects.requireNonNull(description, "description");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.valueHint = Objects.requireNonNull(valueHint, "valueHint");
        this.suggestions = List.copyOf(suggestions);
        this.parser = Objects.requireNonNull(parser, "parser");
        this.printer = Objects.requireNonNull(printer, "printer");
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    public T defaultValue() {
        return defaultValue;
    }

    /** Texto corto con los valores aceptados, por ejemplo {@code 1-9}. */
    public String valueHint() {
        return valueHint;
    }

    /** Valores propuestos al autocompletar en el chat. */
    public List<String> suggestions() {
        return suggestions;
    }

    /** @throws InvalidValueException si el texto no es un valor válido para esta opción */
    public T parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidValueException("falta el valor (" + valueHint + ")");
        }
        return parser.apply(raw.trim());
    }

    public String print(T value) {
        return printer.apply(value);
    }

    /** Imprime un valor que viene de un mapa sin tipo. */
    public String printUnchecked(Object value) {
        return print(cast(value));
    }

    @SuppressWarnings("unchecked")
    public T cast(Object value) {
        return (T) value;
    }

    @Override
    public String toString() {
        return key;
    }

    // --- fábricas -------------------------------------------------------

    public static Option<Boolean> ofBoolean(String key, boolean defaultValue, String description) {
        return new Option<>(key, description, defaultValue, "true/false", List.of("true", "false"),
                raw -> switch (raw.toLowerCase(Locale.ROOT)) {
                    case "true", "on", "yes", "y", "1", "si", "sí" -> Boolean.TRUE;
                    case "false", "off", "no", "n", "0" -> Boolean.FALSE;
                    default -> throw new InvalidValueException("se esperaba true o false, no \"" + raw + "\"");
                },
                value -> value ? "true" : "false");
    }

    public static Option<Integer> ofInt(String key, int defaultValue, int min, int max, String description) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("default out of range for " + key);
        }
        String hint = min + "-" + max;
        List<String> suggestions = new ArrayList<>();
        if (max - min <= 16) {
            for (int value = min; value <= max; value++) {
                suggestions.add(Integer.toString(value));
            }
        } else {
            suggestions.add(Integer.toString(defaultValue));
        }
        return new Option<>(key, description, defaultValue, hint, suggestions,
                raw -> {
                    int value;
                    try {
                        value = Integer.parseInt(raw);
                    } catch (NumberFormatException error) {
                        throw new InvalidValueException("se esperaba un número entero (" + hint + "), no \"" + raw + "\"");
                    }
                    if (value < min || value > max) {
                        throw new InvalidValueException("el valor " + value + " está fuera del rango " + hint);
                    }
                    return value;
                },
                String::valueOf);
    }

    public static Option<Long> ofLong(String key, long defaultValue, long min, long max, String description,
                                      List<String> suggestions) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("default out of range for " + key);
        }
        String hint = min + "-" + max;
        return new Option<>(key, description, defaultValue, hint, suggestions,
                raw -> {
                    long value;
                    try {
                        value = Long.parseLong(raw);
                    } catch (NumberFormatException error) {
                        throw new InvalidValueException("se esperaba un número entero (" + hint + "), no \"" + raw + "\"");
                    }
                    if (value < min || value > max) {
                        throw new InvalidValueException("el valor " + value + " está fuera del rango " + hint);
                    }
                    return value;
                },
                String::valueOf);
    }

    public static <E extends Enum<E>> Option<E> ofEnum(String key, E defaultValue, String description) {
        Class<E> type = defaultValue.getDeclaringClass();
        List<String> names = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            names.add(constant.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        }
        return new Option<>(key, description, defaultValue, String.join("|", names), names,
                raw -> {
                    String wanted = raw.toLowerCase(Locale.ROOT).replace('-', '_');
                    for (E constant : type.getEnumConstants()) {
                        if (constant.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                            return constant;
                        }
                    }
                    throw new InvalidValueException("se esperaba uno de " + String.join(", ", names) + ", no \"" + raw + "\"");
                },
                value -> value.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    /** Texto libre validado con un patrón; se usa para el identificador del objeto. */
    public static Option<String> ofString(String key, String defaultValue, String pattern, String valueHint,
                                          String description, List<String> suggestions) {
        return new Option<>(key, description, defaultValue, valueHint, suggestions,
                raw -> {
                    if (!raw.matches(pattern)) {
                        throw new InvalidValueException("\"" + raw + "\" no tiene el formato esperado (" + valueHint + ")");
                    }
                    return raw;
                },
                Function.identity());
    }
}
