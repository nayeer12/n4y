package dev.naturaltotem.config;

import dev.naturaltotem.CursorGlide;
import dev.naturaltotem.RefillPlanner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

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

    /** Aplica varios ajustes de golpe y avisa una sola vez. */
    public void applyAll(Map<Option<?>, Object> changes) {
        changes.forEach((option, value) -> values.put(option.key(), value));
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

    /** Duración del próximo acercamiento del cursor, sorteada dentro del margen configurado. */
    public long nextMoveNanos(RandomGenerator random) {
        return CursorGlide.randomBetween(get(Options.MOVE_MS_MIN), get(Options.MOVE_MS_MAX), random) * 1_000_000L;
    }

    /**
     * Espera hasta la próxima acción, sorteada dentro del margen configurado.
     * El temporizador exige un valor positivo, así que 0 ms significa "sin espera".
     */
    public long nextActionIntervalNanos(RandomGenerator random) {
        long millis = CursorGlide.randomBetween(
                get(Options.ACTION_INTERVAL_MS_MIN), get(Options.ACTION_INTERVAL_MS_MAX), random);
        return Math.max(1L, millis * 1_000_000L);
    }

    public long retryNanos() {
        return get(Options.RETRY_MS) * 1_000_000L;
    }

    public boolean continuous() {
        return get(Options.CONTINUOUS);
    }

    public boolean requireFocus() {
        return get(Options.REQUIRE_FOCUS);
    }

    public boolean moveCursor() {
        return get(Options.MOVE_CURSOR);
    }

    /** Curvatura del recorrido en tanto por uno (el ajuste se expone en por ciento). */
    public double curvature() {
        return get(Options.CURVE) / 100.0;
    }

    public double aimJitter() {
        return get(Options.AIM_JITTER);
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

    /**
     * Combinaciones de ajustes que seguramente no son lo que se quería. No son errores: el mod
     * funciona igual, pero conviene decirlas en voz alta al cambiar un ajuste o al pedir el estado.
     */
    public List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        if (nothingToFill()) {
            warnings.add("fill-hotbar y fill-offhand están los dos en false: no hay nada que rellenar.");
        }
        if (get(Options.MOVE_MS_MAX) < get(Options.MOVE_MS_MIN)) {
            warnings.add("move-ms-max es menor que move-ms-min: se usará siempre el mínimo.");
        }
        if (get(Options.ACTION_INTERVAL_MS_MAX) < get(Options.ACTION_INTERVAL_MS_MIN)) {
            warnings.add("action-interval-ms-max es menor que el mínimo: se usará siempre el mínimo.");
        }
        return warnings;
    }
}
