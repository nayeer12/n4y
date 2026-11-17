// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; ampliado con opciones configurables.
package dev.naturaltotem;

import java.util.Objects;

/**
 * Decide el siguiente intercambio: de qué casilla sale el tótem y a cuál entra.
 *
 * <p>Es la única parte con las reglas del rellenado y no toca Minecraft, así que se prueba
 * sola. Con {@link Options#LEGACY} responde exactamente lo mismo que el JAR 1.4.0; eso es
 * lo que comprueba {@code BinaryParityTest} contra el original.
 */
public final class RefillPlanner {

    /** Casillas del inventario principal, incluida la barra rápida (0-35). */
    public static final int MAIN_SLOTS = 36;

    /** Índice de la mano secundaria dentro del contenedor del jugador. */
    public static final int OFFHAND_SLOT = 40;

    /** Última casilla de la barra rápida. */
    private static final int LAST_HOTBAR_SLOT = 8;

    /** Orden en que se busca el tótem de origen. */
    public enum SourceOrder {
        /** Inventario principal (9-35) y después la barra rápida: el orden del JAR 1.4.0. */
        MAIN_FIRST,
        /** Barra rápida primero, luego el inventario principal. */
        HOTBAR_FIRST,
        /** Índice ascendente, de la casilla 0 a la 35. */
        ASCENDING,
        /** El tótem más cercano al destino en la pantalla, venga de donde venga. */
        NEAREST,
        /** El más cercano del inventario principal; solo baja a la barra rápida si no queda ninguno. */
        NEAREST_MAIN
    }

    /**
     * Dónde se dibuja cada casilla. Sirve para elegir el tótem más cercano y para que el
     * recorrido del cursor sea corto.
     */
    public interface Geometry {
        /** @return coordenada x de la casilla en la pantalla, o {@code Integer.MIN_VALUE} si no se conoce */
        int x(int slot);

        int y(int slot);
    }

    /**
     * Colocación de las casillas en la pantalla de inventario de Minecraft: tres filas de
     * nueve, la barra rápida debajo y la mano secundaria junto al muñeco.
     *
     * <p>Se usa cuando no hay una pantalla abierta de la que leer las posiciones reales,
     * sobre todo en las pruebas.
     */
    public static final Geometry VANILLA_GEOMETRY = new Geometry() {
        @Override
        public int x(int slot) {
            if (slot == OFFHAND_SLOT) {
                return 77;
            }
            return slot <= LAST_HOTBAR_SLOT ? 8 + 18 * slot : 8 + 18 * ((slot - 9) % 9);
        }

        @Override
        public int y(int slot) {
            if (slot == OFFHAND_SLOT) {
                return 62;
            }
            return slot <= LAST_HOTBAR_SLOT ? 142 : 84 + 18 * ((slot - 9) / 9);
        }
    };

    /**
     * Reglas del rellenado.
     *
     * @param hotbarSlot   casilla de la barra rápida que se mantiene llena (0-8)
     * @param fillHotbar   si se rellena esa casilla
     * @param fillOffhand  si se rellena la mano secundaria
     * @param offhandFirst si la mano secundaria se atiende antes que la barra rápida
     * @param order        orden de búsqueda del origen
     */
    public record Options(int hotbarSlot, boolean fillHotbar, boolean fillOffhand, boolean offhandFirst,
                          SourceOrder order) {

        /** Comportamiento del JAR 1.4.0: casilla 9, después mano secundaria, origen desde el inventario. */
        public static final Options LEGACY = new Options(LAST_HOTBAR_SLOT, true, true, false, SourceOrder.MAIN_FIRST);

        public Options {
            if (hotbarSlot < 0 || hotbarSlot > LAST_HOTBAR_SLOT) {
                throw new IllegalArgumentException("hotbarSlot must be 0-8");
            }
            Objects.requireNonNull(order, "order");
        }
    }

    private RefillPlanner() {
    }

    /** Planifica con las reglas del JAR 1.4.0. */
    public static Move next(boolean[] totems, boolean offhandTotem) {
        return next(totems, offhandTotem, Options.LEGACY);
    }

    /** Planifica con la colocación estándar de la pantalla de inventario. */
    public static Move next(boolean[] totems, boolean offhandTotem, Options options) {
        return next(totems, offhandTotem, options, VANILLA_GEOMETRY);
    }

    /**
     * @param totems       una posición por casilla del inventario principal: {@code true} si ya tiene el objeto
     * @param offhandTotem si la mano secundaria ya tiene el objeto
     * @param options      reglas de rellenado
     * @param geometry     dónde se dibuja cada casilla, para los órdenes por cercanía
     * @return el intercambio que toca, o {@code null} si no hay nada que hacer
     */
    public static Move next(boolean[] totems, boolean offhandTotem, Options options, Geometry geometry) {
        if (totems.length != MAIN_SLOTS) {
            throw new IllegalArgumentException("Expected 36 inventory slots");
        }
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(geometry, "geometry");

        int destination = destination(totems, offhandTotem, options);
        if (destination < 0) {
            return null;
        }
        // La casilla que se mantiene llena nunca sirve de origen: es el destino del otro intercambio.
        int reserved = options.fillHotbar() ? options.hotbarSlot() : -1;
        int source = findSource(totems, options.order(), destination, reserved, geometry);
        return source < 0 ? null : new Move(source, destination);
    }

    private static int destination(boolean[] totems, boolean offhandTotem, Options options) {
        boolean hotbarNeeds = options.fillHotbar() && !totems[options.hotbarSlot()];
        boolean offhandNeeds = options.fillOffhand() && !offhandTotem;
        if (options.offhandFirst()) {
            if (offhandNeeds) {
                return OFFHAND_SLOT;
            }
            return hotbarNeeds ? options.hotbarSlot() : -1;
        }
        if (hotbarNeeds) {
            return options.hotbarSlot();
        }
        return offhandNeeds ? OFFHAND_SLOT : -1;
    }

    private static int findSource(boolean[] totems, SourceOrder order, int destination, int reserved,
                                  Geometry geometry) {
        return switch (order) {
            case MAIN_FIRST -> firstOf(
                    scan(totems, LAST_HOTBAR_SLOT + 1, MAIN_SLOTS, destination, reserved),
                    () -> scan(totems, 0, LAST_HOTBAR_SLOT + 1, destination, reserved));
            case HOTBAR_FIRST -> firstOf(
                    scan(totems, 0, LAST_HOTBAR_SLOT + 1, destination, reserved),
                    () -> scan(totems, LAST_HOTBAR_SLOT + 1, MAIN_SLOTS, destination, reserved));
            case ASCENDING -> scan(totems, 0, MAIN_SLOTS, destination, reserved);
            case NEAREST -> nearest(totems, 0, MAIN_SLOTS, destination, reserved, geometry);
            case NEAREST_MAIN -> firstOf(
                    nearest(totems, LAST_HOTBAR_SLOT + 1, MAIN_SLOTS, destination, reserved, geometry),
                    () -> nearest(totems, 0, LAST_HOTBAR_SLOT + 1, destination, reserved, geometry));
        };
    }

    private static int firstOf(int found, java.util.function.IntSupplier fallback) {
        return found >= 0 ? found : fallback.getAsInt();
    }

    private static int scan(boolean[] totems, int from, int to, int destination, int reserved) {
        for (int slot = from; slot < to; slot++) {
            if (usable(totems, slot, destination, reserved)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * El tótem cuya casilla se dibuja más cerca del destino. Con la misma distancia gana el
     * índice menor, así que la elección no depende del orden de recorrido.
     */
    private static int nearest(boolean[] totems, int from, int to, int destination, int reserved, Geometry geometry) {
        int best = -1;
        long bestDistance = Long.MAX_VALUE;
        int destinationX = geometry.x(destination);
        int destinationY = geometry.y(destination);
        for (int slot = from; slot < to; slot++) {
            if (!usable(totems, slot, destination, reserved)) {
                continue;
            }
            long dx = geometry.x(slot) - (long) destinationX;
            long dy = geometry.y(slot) - (long) destinationY;
            long distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        return best;
    }

    private static boolean usable(boolean[] totems, int slot, int destination, int reserved) {
        return totems[slot] && slot != destination && slot != reserved;
    }

    /**
     * Un intercambio pendiente.
     *
     * @param source      casilla del inventario principal de la que sale el objeto (0-35)
     * @param destination casilla de destino: 0-8 en la barra rápida o {@value #OFFHAND_SLOT} para la mano secundaria
     */
    public record Move(int source, int destination) {
    }
}
