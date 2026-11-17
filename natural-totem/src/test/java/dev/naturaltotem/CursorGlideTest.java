package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursorGlideTest {

    @Test
    void progressStaysBetweenZeroAndOne() {
        assertEquals(0.0, CursorGlide.progress(-10, 60_000_000L));
        assertEquals(0.0, CursorGlide.progress(0, 60_000_000L));
        assertEquals(0.5, CursorGlide.progress(30_000_000L, 60_000_000L));
        assertEquals(1.0, CursorGlide.progress(60_000_000L, 60_000_000L));
        assertEquals(1.0, CursorGlide.progress(Long.MAX_VALUE, 60_000_000L));
    }

    @Test
    void aDurationOfZeroArrivesImmediately() {
        assertEquals(1.0, CursorGlide.progress(0L, 0L));
        assertEquals(1.0, CursorGlide.progress(0L, -5L));
    }

    @Test
    void easingKeepsTheEndsAndIsSymmetric() {
        assertEquals(0.0, CursorGlide.ease(0.0));
        assertEquals(1.0, CursorGlide.ease(1.0));
        assertEquals(0.5, CursorGlide.ease(0.5), 1e-12);
        assertEquals(1.0, CursorGlide.ease(0.25) + CursorGlide.ease(0.75), 1e-12);
        assertEquals(0.0, CursorGlide.ease(-3.0));
        assertEquals(1.0, CursorGlide.ease(7.0));
    }

    @Test
    void easingMovesForwardAndSlowlyAtTheEnds() {
        double previous = -1.0;
        for (int step = 0; step <= 100; step++) {
            double eased = CursorGlide.ease(step / 100.0);
            assertTrue(eased > previous, "la curva debe crecer siempre");
            previous = eased;
        }
        assertTrue(CursorGlide.ease(0.05) < 0.05, "arranca despacio");
        assertTrue(CursorGlide.ease(0.95) > 0.95, "termina despacio");
    }

    @Test
    void interpolationReachesBothEnds() {
        assertEquals(100.0, CursorGlide.interpolate(100.0, 300.0, 0.0));
        assertEquals(300.0, CursorGlide.interpolate(100.0, 300.0, 1.0));
        assertEquals(200.0, CursorGlide.interpolate(100.0, 300.0, 0.5));
        assertEquals(300.0, CursorGlide.interpolate(300.0, 300.0, 0.3));
    }

    @Test
    void guiCoordinatesScaleToWindowPixels() {
        assertEquals(200.0, CursorGlide.toWindowPixels(100.0, 1920, 960));
        assertEquals(100.0, CursorGlide.toWindowPixels(100.0, 1920, 1920));
        assertEquals(100.0, CursorGlide.toWindowPixels(100.0, 1920, 0), "sin escala conocida no se toca");
    }
}
