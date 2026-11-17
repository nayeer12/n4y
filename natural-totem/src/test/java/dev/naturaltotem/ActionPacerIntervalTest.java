package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Comportamiento del intervalo cambiable; la igualdad con el JAR 1.4.0 la cubre BinaryParityTest. */
class ActionPacerIntervalTest {

    @Test
    void anIntervalChangeAppliesToTheNextActionWithoutRestartingTheWait() {
        ActionPacer pacer = new ActionPacer(100_000_000L);
        pacer.defer(1_000L);
        assertFalse(pacer.ready(50_000_000L));

        pacer.setInterval(10_000_000L);
        assertEquals(10_000_000L, pacer.interval());
        assertTrue(pacer.ready(50_000_000L), "la espera ya cumplida cuenta con el intervalo nuevo");

        pacer.setInterval(500_000_000L);
        assertFalse(pacer.ready(50_000_000L));
    }

    @Test
    void refusesNonPositiveIntervals() {
        ActionPacer pacer = new ActionPacer(1L);
        for (long interval : new long[]{Long.MIN_VALUE, -1L, 0L}) {
            assertThrows(IllegalArgumentException.class, () -> pacer.setInterval(interval));
        }
        assertEquals(1L, pacer.interval());
    }

    @Test
    void clearLetsTheNextActionHappenAtOnce() {
        ActionPacer pacer = new ActionPacer(100_000_000L);
        pacer.defer(0L);
        assertFalse(pacer.ready(1_000L));
        pacer.clear();
        assertTrue(pacer.ready(1_000L));
    }

    @Test
    void aFreshPacerIsReadyAndQueriesDoNotChangeThat() {
        ActionPacer pacer = new ActionPacer(100_000_000L);
        assertTrue(pacer.ready(0L));
        assertTrue(pacer.ready(0L));
        pacer.defer(0L);
        assertFalse(pacer.ready(99_999_999L));
        assertTrue(pacer.ready(100_000_000L));
    }
}
