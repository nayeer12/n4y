package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Órdenes de búsqueda por cercanía: el tótem que menos recorrido le cuesta al cursor. */
class NearestSourceTest {

    private static boolean[] inventoryWith(int... slots) {
        boolean[] totems = new boolean[RefillPlanner.MAIN_SLOTS];
        for (int slot : slots) {
            totems[slot] = true;
        }
        return totems;
    }

    private static RefillPlanner.Options order(RefillPlanner.SourceOrder order) {
        return new RefillPlanner.Options(8, true, true, false, order);
    }

    private static double distanceToHotbarSlot(int slot, int hotbarSlot) {
        return Math.hypot(RefillPlanner.VANILLA_GEOMETRY.x(slot) - RefillPlanner.VANILLA_GEOMETRY.x(hotbarSlot),
                RefillPlanner.VANILLA_GEOMETRY.y(slot) - RefillPlanner.VANILLA_GEOMETRY.y(hotbarSlot));
    }

    @Test
    void vanillaGeometryMatchesTheInventoryLayout() {
        assertEquals(8, RefillPlanner.VANILLA_GEOMETRY.x(0));
        assertEquals(142, RefillPlanner.VANILLA_GEOMETRY.y(0));
        assertEquals(8 + 18 * 8, RefillPlanner.VANILLA_GEOMETRY.x(8));
        // La casilla 9 es la primera del inventario principal: misma columna que la 0, fila de arriba.
        assertEquals(8, RefillPlanner.VANILLA_GEOMETRY.x(9));
        assertEquals(84, RefillPlanner.VANILLA_GEOMETRY.y(9));
        assertEquals(84 + 36, RefillPlanner.VANILLA_GEOMETRY.y(27));
        assertEquals(77, RefillPlanner.VANILLA_GEOMETRY.x(RefillPlanner.OFFHAND_SLOT));
    }

    @Test
    void nearestMainPicksTheClosestTotemOfTheInventory() {
        // Para la casilla 9 (índice 8, abajo a la derecha) la fila de abajo del inventario está más cerca.
        boolean[] totems = inventoryWith(9, 26, 35);
        RefillPlanner.Move move = RefillPlanner.next(totems, true, order(RefillPlanner.SourceOrder.NEAREST_MAIN));
        assertNotNull(move);
        assertEquals(35, move.source(), "la 35 comparte columna con la 9 y está en la fila de abajo");
        assertTrue(distanceToHotbarSlot(35, 8) < distanceToHotbarSlot(9, 8));

        // El orden del 1.4.0 cogería la primera por índice, mucho más lejos.
        assertEquals(9, RefillPlanner.next(totems, true, order(RefillPlanner.SourceOrder.MAIN_FIRST)).source());
    }

    @Test
    void nearestMainIgnoresTheHotbarWhileTheInventoryHasTotems() {
        boolean[] totems = inventoryWith(7, 20);
        assertEquals(20, RefillPlanner.next(totems, true, order(RefillPlanner.SourceOrder.NEAREST_MAIN)).source(),
                "la 7 está al lado, pero vaciar la barra rápida estropea la mano del jugador");
        assertEquals(7, RefillPlanner.next(totems, true, order(RefillPlanner.SourceOrder.NEAREST)).source(),
                "nearest sí coge la más cercana venga de donde venga");
    }

    @Test
    void nearestMainFallsBackToTheHotbarWhenTheInventoryIsEmpty() {
        boolean[] totems = inventoryWith(0, 5);
        assertEquals(5, RefillPlanner.next(totems, true, order(RefillPlanner.SourceOrder.NEAREST_MAIN)).source(),
                "sin tótems arriba coge el más cercano de la barra rápida");
    }

    @Test
    void theNearestSourceForTheOffhandIsMeasuredFromTheOffhandSlot() {
        // La mano secundaria se dibuja arriba a la izquierda, junto al muñeco.
        boolean[] totems = inventoryWith(9, 17);
        RefillPlanner.Options options = new RefillPlanner.Options(8, false, true, false,
                RefillPlanner.SourceOrder.NEAREST);
        RefillPlanner.Move move = RefillPlanner.next(totems, false, options);
        assertEquals(RefillPlanner.OFFHAND_SLOT, move.destination());
        assertEquals(9, move.source(), "la 9 está a la izquierda, bajo la mano secundaria");
    }

    @Test
    void aTieIsBrokenByTheLowestIndexSoThePlanIsStable() {
        // Las casillas 18 y 36-... no existen; 17 y 19 no equidistan, así que se usan 11 y 29,
        // simétricas respecto a la fila del medio para el destino 20.
        boolean[] totems = inventoryWith(11, 29);
        RefillPlanner.Options options = new RefillPlanner.Options(1, true, true, false,
                RefillPlanner.SourceOrder.NEAREST);
        assertEquals(RefillPlanner.next(totems, true, options), RefillPlanner.next(totems, true, options));
    }

    @Test
    void realScreenPositionsAreUsedWhenTheyAreAvailable() {
        // Con la colocación estándar gana la casilla 30; aquí se le da la vuelta a la pantalla.
        RefillPlanner.Geometry geometry = new RefillPlanner.Geometry() {
            @Override
            public int x(int slot) {
                return switch (slot) {
                    case 8 -> 100;
                    case 10 -> 104;
                    default -> 900;
                };
            }

            @Override
            public int y(int slot) {
                return 100;
            }
        };
        boolean[] totems = inventoryWith(10, 30);
        assertEquals(30, RefillPlanner.next(totems, true,
                        order(RefillPlanner.SourceOrder.NEAREST_MAIN), RefillPlanner.VANILLA_GEOMETRY).source(),
                "con la colocación estándar la 30 cae más cerca de la casilla 9");
        assertEquals(10, RefillPlanner.next(totems, true,
                        order(RefillPlanner.SourceOrder.NEAREST_MAIN), geometry).source(),
                "con las posiciones de la pantalla gana la otra, así que se están usando de verdad");
    }

    @Test
    void nearestNeverTakesTheProtectedSlotOrTheDestination() {
        for (RefillPlanner.SourceOrder sourceOrder :
                new RefillPlanner.SourceOrder[]{RefillPlanner.SourceOrder.NEAREST, RefillPlanner.SourceOrder.NEAREST_MAIN}) {
            RefillPlanner.Options options = new RefillPlanner.Options(4, true, true, false, sourceOrder);
            // Solo hay tótem en la casilla protegida: no puede salir de ahí para la mano secundaria.
            assertNull(RefillPlanner.next(inventoryWith(4), false, options));
        }
    }

    @Test
    void geometryIsRequired() {
        assertThrows(NullPointerException.class, () -> RefillPlanner.next(
                new boolean[RefillPlanner.MAIN_SLOTS], false, RefillPlanner.Options.LEGACY, null));
    }
}
