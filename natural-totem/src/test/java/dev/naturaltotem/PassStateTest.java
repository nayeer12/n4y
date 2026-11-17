package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El mod sigue vigilando con el inventario abierto, pero sin insistir sin parar. */
class PassStateTest {

    private static final long RETRY = 200_000_000L;
    private static final boolean CONTINUOUS = true;
    private static final boolean ONE_PASS = false;

    @Test
    void withoutContinuousOnePassPerOpeningIsAllThereIs() {
        PassState state = new PassState();
        state.beginOpening();
        assertTrue(state.canAct(0L));

        state.finishedPass(ONE_PASS);
        assertFalse(state.canAct(0L));
        assertFalse(state.canAct(Long.MAX_VALUE / 2), "ni esperando vuelve a actuar");
        assertTrue(state.stopped());

        state.beginOpening();
        assertTrue(state.canAct(0L), "al reabrir el inventario empieza otra vez");
    }

    @Test
    void withContinuousItKeepsWatchingAfterFinishingAPass() {
        PassState state = new PassState();
        state.beginOpening();

        for (int tick = 0; tick < 100; tick++) {
            assertTrue(state.canAct(tick * 50_000_000L), "debe seguir mirando por si estalla el tótem");
            state.finishedPass(CONTINUOUS);
        }
        assertFalse(state.stopped());
    }

    @Test
    void aPauseOnlyWaitsTheRetryTime() {
        PassState state = new PassState();
        state.beginOpening();

        state.pause(CONTINUOUS, RETRY, 1_000L);
        assertFalse(state.canAct(1_000L));
        assertFalse(state.canAct(1_000L + RETRY - 1L));
        assertTrue(state.canAct(1_000L + RETRY));
        assertTrue(state.canAct(1_000L + RETRY), "una vez cumplida la espera ya no hace falta repetirla");
    }

    @Test
    void pausesDoNotCountAsFailures() {
        PassState state = new PassState();
        state.beginOpening();
        long now = 0L;
        for (int attempt = 0; attempt < PassState.MAX_FAILURES * 3; attempt++) {
            state.pause(CONTINUOUS, RETRY, now);
            now += RETRY;
            assertTrue(state.canAct(now));
        }
        assertEquals(0, state.failures());
        assertFalse(state.stopped());
    }

    @Test
    void severalRealFailuresInARowStopItUntilTheInventoryIsReopened() {
        PassState state = new PassState();
        state.beginOpening();
        long now = 0L;

        for (int attempt = 1; attempt < PassState.MAX_FAILURES; attempt++) {
            assertFalse(state.fail(CONTINUOUS, RETRY, now), "todavía no debería rendirse");
            now += RETRY;
            assertTrue(state.canAct(now));
            assertEquals(attempt, state.failures());
        }
        assertTrue(state.fail(CONTINUOUS, RETRY, now), "al llegar al límite se para");
        assertTrue(state.stopped());
        assertFalse(state.canAct(now + RETRY * 100));
    }

    @Test
    void aSuccessForgetsThePreviousFailures() {
        PassState state = new PassState();
        state.beginOpening();
        state.fail(CONTINUOUS, RETRY, 0L);
        state.fail(CONTINUOUS, RETRY, 0L);
        assertEquals(2, state.failures());

        state.succeeded();
        assertEquals(0, state.failures());
        assertFalse(state.selectionDone(), "tras rellenar hay que volver a mirar la casilla seleccionada");
    }

    @Test
    void withoutContinuousTheFirstFailureIsTheLast() {
        PassState state = new PassState();
        state.beginOpening();
        assertTrue(state.fail(ONE_PASS, RETRY, 0L));
        assertTrue(state.stopped());
    }

    @Test
    void theSlotIsSelectedOnlyOnceUntilSomethingChanges() {
        PassState state = new PassState();
        state.beginOpening();
        assertFalse(state.selectionDone());

        state.markSelectionDone();
        assertTrue(state.selectionDone());

        state.succeeded();
        assertFalse(state.selectionDone());

        state.markSelectionDone();
        state.beginOpening();
        assertFalse(state.selectionDone());
    }

    @Test
    void restartingClearsAStopWithoutTouchingTheSelection() {
        PassState state = new PassState();
        state.beginOpening();
        state.markSelectionDone();
        state.fail(ONE_PASS, RETRY, 0L);
        assertTrue(state.stopped());

        state.restart();
        assertFalse(state.stopped());
        assertTrue(state.canAct(0L));
        assertEquals(0, state.failures());
        assertTrue(state.selectionDone(), "reiniciar no es reabrir: la selección ya se hizo");
    }

    @Test
    void waitingSurvivesAClockThatWrapsAround() {
        PassState state = new PassState();
        state.beginOpening();
        long nearOverflow = Long.MAX_VALUE - RETRY / 2;

        state.pause(CONTINUOUS, RETRY, nearOverflow);
        assertFalse(state.canAct(nearOverflow));
        assertTrue(state.canAct(nearOverflow + RETRY), "la resta de nanoTime sigue valiendo al desbordar");
    }
}
