package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas nuevas del planificador; la igualdad con el JAR 1.4.0 la comprueba BinaryParityTest. */
class RefillPlannerOptionsTest {

    private static boolean[] inventoryWith(int... slots) {
        boolean[] totems = new boolean[RefillPlanner.MAIN_SLOTS];
        for (int slot : slots) {
            totems[slot] = true;
        }
        return totems;
    }

    @Test
    void defaultOptionsRepeatTheLegacyBehavior() {
        Random random = new Random(20260417L);
        for (int example = 0; example < 2000; example++) {
            boolean[] totems = new boolean[RefillPlanner.MAIN_SLOTS];
            for (int slot = 0; slot < totems.length; slot++) {
                totems[slot] = random.nextBoolean();
            }
            boolean offhand = random.nextBoolean();
            assertEquals(RefillPlanner.next(totems, offhand),
                    RefillPlanner.next(totems, offhand, RefillPlanner.Options.LEGACY));
        }
    }

    @Test
    void fillsTheConfiguredHotbarSlotAndNeverUsesItAsSource() {
        RefillPlanner.Options options = new RefillPlanner.Options(0, true, true, false,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        assertEquals(new RefillPlanner.Move(20, 0), RefillPlanner.next(inventoryWith(20), false, options));

        // La casilla 0 ya está llena: el siguiente destino es la mano, y la 0 no vale de origen.
        assertEquals(new RefillPlanner.Move(20, RefillPlanner.OFFHAND_SLOT),
                RefillPlanner.next(inventoryWith(0, 20), false, options));
        assertNull(RefillPlanner.next(inventoryWith(0), false, options));
    }

    @Test
    void theLegacySlotCanBeUsedAsSourceWhenAnotherSlotIsConfigured() {
        RefillPlanner.Options options = new RefillPlanner.Options(2, true, true, false,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        assertEquals(new RefillPlanner.Move(8, 2), RefillPlanner.next(inventoryWith(8), false, options));
    }

    @Test
    void offhandFirstSwapsThePriority() {
        RefillPlanner.Options hotbarFirst = RefillPlanner.Options.LEGACY;
        RefillPlanner.Options offhandFirst = new RefillPlanner.Options(8, true, true, true,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        boolean[] totems = inventoryWith(12);
        assertEquals(8, RefillPlanner.next(totems, false, hotbarFirst).destination());
        assertEquals(RefillPlanner.OFFHAND_SLOT, RefillPlanner.next(totems, false, offhandFirst).destination());
    }

    @Test
    void disabledDestinationsAreSkipped() {
        RefillPlanner.Options onlyOffhand = new RefillPlanner.Options(8, false, true, false,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        // Sin rellenar la barra rápida, la casilla 9 deja de estar protegida y sirve de origen.
        assertEquals(new RefillPlanner.Move(8, RefillPlanner.OFFHAND_SLOT),
                RefillPlanner.next(inventoryWith(8), false, onlyOffhand));
        assertNull(RefillPlanner.next(inventoryWith(8), true, onlyOffhand));

        RefillPlanner.Options onlyHotbar = new RefillPlanner.Options(8, true, false, false,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        assertNull(RefillPlanner.next(inventoryWith(8), false, onlyHotbar));

        RefillPlanner.Options nothing = new RefillPlanner.Options(8, false, false, false,
                RefillPlanner.SourceOrder.MAIN_FIRST);
        assertNull(RefillPlanner.next(inventoryWith(12), false, nothing));
    }

    @Test
    void sourceOrderPicksDifferentSlots() {
        boolean[] totems = inventoryWith(3, 20);
        assertEquals(20, RefillPlanner.next(totems, false,
                new RefillPlanner.Options(8, true, true, false, RefillPlanner.SourceOrder.MAIN_FIRST)).source());
        assertEquals(3, RefillPlanner.next(totems, false,
                new RefillPlanner.Options(8, true, true, false, RefillPlanner.SourceOrder.HOTBAR_FIRST)).source());
        assertEquals(3, RefillPlanner.next(totems, false,
                new RefillPlanner.Options(8, true, true, false, RefillPlanner.SourceOrder.ASCENDING)).source());
    }

    @Test
    void repeatedPlansEmptyTheInventoryWithoutLoops() {
        for (RefillPlanner.SourceOrder order : RefillPlanner.SourceOrder.values()) {
            RefillPlanner.Options options = new RefillPlanner.Options(4, true, true, false, order);
            boolean[] totems = inventoryWith(4, 7, 15);
            boolean offhand = false;
            // Con la casilla 4 ya llena solo queda un movimiento: llenar la mano secundaria.
            RefillPlanner.Move first = RefillPlanner.next(totems, offhand, options);
            assertNotNull(first);
            assertEquals(RefillPlanner.OFFHAND_SLOT, first.destination());
            assertNotEquals(4, first.source());
            totems[first.source()] = false;
            assertNull(RefillPlanner.next(totems, true, options));
        }
    }

    @Test
    void rejectsImpossibleOptionsAndInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new RefillPlanner.Options(9, true, true, false, RefillPlanner.SourceOrder.MAIN_FIRST));
        assertThrows(IllegalArgumentException.class,
                () -> new RefillPlanner.Options(-1, true, true, false, RefillPlanner.SourceOrder.MAIN_FIRST));
        assertThrows(NullPointerException.class,
                () -> new RefillPlanner.Options(0, true, true, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> RefillPlanner.next(new boolean[35], false, RefillPlanner.Options.LEGACY));
        assertThrows(NullPointerException.class,
                () -> RefillPlanner.next(new boolean[RefillPlanner.MAIN_SLOTS], false, null));
    }
}
