package dev.naturaltotem.config;

import dev.naturaltotem.RefillPlanner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Valores actuales de los ajustes.
 *
 * <p>Se escribe desde el hilo del cliente (comando, arranque) y se lee desde el mismo hilo,
 * pero el mapa es concurrente para que una lectura durante el render nunca vea un estado a medias.
 * No tiene nada de Minecraft dentro, así que las pruebas lo usan tal cual.
 */
public final class TotemConfig {

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private volatile Consumer<Option<?>> listener = option -> {
    };

    public TotemConfig() {
        resetAll();
    }

    /** Se llama después de cada cambio con la opción afectada, o {@code null} si fue un cambio masivo. */
    public void onChange(Consumer<Option<?>> listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public <T> T get(Option<T> option) {
        return option.cast(values.get(option.key()));
    }

    /** @return {@code true} si el valor cambió */
    public <T> boolean set(Option<T> option, T value) {
        Objects.requireNonNull(option, "option");
        Objects.requireNonNull(value, "value");
        Object previous = values.put(option.key(), value);
        boolean changed = !value.equals(previous);
        if (changed) {
            listener.accept(option);
        }
        return changed;
    }

    /**
     * Analiza el texto y lo guarda.
     *
     * @throws Option.InvalidValueException si el texto no es válido para esa opción
     */
    public <T> boolean setParsed(Option<T> option, String raw) {
        return set(option, option.parse(raw));
    }

    public boolean reset(Option<?> option) {
        return setUnchecked(option, option.defaultValue());
    }

    public void resetAll() {
        for (Option<?> option : Options.ALL) {
            values.put(option.key(), option.defaultValue());
        }
        listener.accept(null);
    }

    @SuppressWarnings("unchecked")
    private boolean setUnchecked(Option<?> option, Object value) {
        return set((Option<Object>) option, value);
    }

    public boolean isDefault(Option<?> option) {
        return option.defaultValue().equals(values.get(option.key()));
    }

    /** @return {@code true} si todos los ajustes están en su valor por defecto */
    public boolean isAllDefault() {
        for (Option<?> option : Options.ALL) {
            if (!isDefault(option)) {
                return false;
            }
        }
        return true;
    }

    public String printed(Option<?> option) {
        return option.printUnchecked(values.get(option.key()));
    }

    /** Vista clave/valor en el orden del catálogo, para guardar o mostrar. */
    public Map<String, String> asStrings() {
        Map<String, String> printed = new LinkedHashMap<>();
        for (Option<?> option : Options.ALL) {
            printed.put(option.key(), printed(option));
        }
        return printed;
    }

    /** Copia independiente, usada por las pruebas y por "reload" para no perder el estado si falla. */
    public TotemConfig copy() {
        TotemConfig copy = new TotemConfig();
        copy.values.putAll(values);
        return copy;
    }

    public void copyFrom(TotemConfig other) {
        values.putAll(other.values);
        listener.accept(null);
    }

    // --- valores derivados que usa la automatización ---------------------

    public boolean enabled() {
        return get(Options.ENABLED);
    }

    /** Índice interno 0-8 de la casilla configurada (la opción se expone como 1-9). */
    public int hotbarSlotIndex() {
        return get(Options.HOTBAR_SLOT) - 1;
    }

    public long moveNanos() {
        return get(Options.MOVE_MS) * 1_000_000L;
    }

    /** El temporizador exige un intervalo positivo; 0 ms significa "sin espera". */
    public long actionIntervalNanos() {
        return Math.max(1L, get(Options.ACTION_INTERVAL_MS) * 1_000_000L);
    }

    public boolean requireFocus() {
        return get(Options.REQUIRE_FOCUS);
    }

    public boolean moveCursor() {
        return get(Options.MOVE_CURSOR);
    }

    public boolean skipWhileClicking() {
        return get(Options.SKIP_WHILE_CLICKING);
    }

    public boolean debug() {
        return get(Options.DEBUG);
    }

    public boolean announce() {
        return get(Options.ANNOUNCE);
    }

    public String itemId() {
        return get(Options.ITEM);
    }

    /** Traduce los ajustes a las reglas que entiende el planificador. */
    public RefillPlanner.Options plan() {
        return new RefillPlanner.Options(
                hotbarSlotIndex(),
                get(Options.FILL_HOTBAR),
                get(Options.FILL_OFFHAND),
                get(Options.OFFHAND_FIRST),
                get(Options.SOURCE_ORDER));
    }

    /** @return {@code true} si no hay ninguna casilla que atender con los ajustes actuales */
    public boolean nothingToFill() {
        return !get(Options.FILL_HOTBAR) && !get(Options.FILL_OFFHAND);
    }
}
