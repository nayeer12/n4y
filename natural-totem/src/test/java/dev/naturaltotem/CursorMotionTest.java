package dev.naturaltotem;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/** Recorrido curvo, punto de llegada y duración sorteada del acercamiento del cursor. */
class CursorMotionTest {

    @Test
    void aStraightCurveIsExactlyTheOldLinearMovement() {
        CursorGlide.Curve straight = new CursorGlide.Curve(100.0, 50.0, 400.0, 250.0, 0.0);
        for (int step = 0; step <= 20; step++) {
            double eased = step / 20.0;
            assertEquals(CursorGlide.interpolate(100.0, 400.0, eased), straight.x(eased), 1e-9);
            assertEquals(CursorGlide.interpolate(50.0, 250.0, eased), straight.y(eased), 1e-9);
        }
    }

    @Test
    void everyCurveStartsAndEndsWhereItShould() {
        for (double bend : new double[]{-0.4, -0.12, 0.0, 0.12, 0.4}) {
            CursorGlide.Curve curve = new CursorGlide.Curve(10.0, 20.0, 310.0, 220.0, bend);
            assertEquals(10.0, curve.x(0.0), 1e-9);
            assertEquals(20.0, curve.y(0.0), 1e-9);
            assertEquals(310.0, curve.x(1.0), 1e-9);
            assertEquals(220.0, curve.y(1.0), 1e-9);
        }
    }

    @Test
    void aBentCurveLeavesTheStraightLineButStaysNearIt() {
        CursorGlide.Curve curve = new CursorGlide.Curve(0.0, 0.0, 400.0, 0.0, 0.15);
        double middle = curve.y(0.5);
        assertNotEquals(0.0, middle, "con curvatura el recorrido no es recto");
        assertEquals(30.0, middle, 1e-9, "la mitad se aparta la mitad de lo que dice el punto de control");

        double maximum = 0.0;
        for (int step = 0; step <= 100; step++) {
            maximum = Math.max(maximum, Math.abs(curve.y(step / 100.0)));
        }
        assertTrue(maximum <= 0.15 * 400.0, "no debe irse más lejos que el punto de control");
    }

    @Test
    void oppositeBendsAreMirrorImages() {
        CursorGlide.Curve left = new CursorGlide.Curve(0.0, 0.0, 200.0, 100.0, 0.2);
        CursorGlide.Curve right = new CursorGlide.Curve(0.0, 0.0, 200.0, 100.0, -0.2);
        for (int step = 1; step < 20; step++) {
            double eased = step / 20.0;
            double straightX = CursorGlide.interpolate(0.0, 200.0, eased);
            double straightY = CursorGlide.interpolate(0.0, 100.0, eased);
            assertEquals(straightX - left.x(eased), right.x(eased) - straightX, 1e-9);
            assertEquals(straightY - left.y(eased), right.y(eased) - straightY, 1e-9);
        }
    }

    @Test
    void theCurveIsClampedOutsideItsRange() {
        CursorGlide.Curve curve = new CursorGlide.Curve(5.0, 5.0, 95.0, 95.0, 0.3);
        assertEquals(curve.x(0.0), curve.x(-2.0), 1e-9);
        assertEquals(curve.y(1.0), curve.y(7.0), 1e-9);
        assertEquals(Math.hypot(90.0, 90.0), curve.distance(), 1e-9);
    }

    @Test
    void shortHopsAreNotBentAndLongOnesAre() {
        RandomGenerator random = new Random(11);
        for (int attempt = 0; attempt < 50; attempt++) {
            assertEquals(0.0, CursorGlide.bend(0.12, 10.0, random), "un salto corto se deja recto");
            assertEquals(0.0, CursorGlide.bend(0.0, 500.0, random), "sin curvatura configurada, recto");
        }

        boolean left = false;
        boolean right = false;
        for (int attempt = 0; attempt < 200; attempt++) {
            double bend = CursorGlide.bend(0.12, 300.0, random);
            assertTrue(Math.abs(bend) <= 0.12 + 1e-9, "no pasa de la curvatura configurada: " + bend);
            assertTrue(Math.abs(bend) >= 0.12 * 0.4 - 1e-9, "tampoco es prácticamente recto: " + bend);
            left |= bend < 0;
            right |= bend > 0;
        }
        assertTrue(left && right, "debe curvarse a un lado y a otro");
    }

    @Test
    void theAimStaysInsideTheSlotAndMoves() {
        RandomGenerator random = new Random(12);
        assertEquals(0.0, CursorGlide.aimOffset(0.0, random), "sin desviación se apunta al centro");

        Set<Double> seen = new HashSet<>();
        for (int attempt = 0; attempt < 500; attempt++) {
            double offset = CursorGlide.aimOffset(3.0, random);
            assertTrue(Math.abs(offset) <= 3.0, "la desviación se sale de la casilla: " + offset);
            seen.add(offset);
        }
        assertTrue(seen.size() > 100, "el punto de llegada debería variar");
    }

    @Test
    void randomDurationsStayInsideTheRangeAndCoverIt() {
        RandomGenerator random = new Random(13);
        Set<Long> seen = new HashSet<>();
        for (int attempt = 0; attempt < 2000; attempt++) {
            long value = CursorGlide.randomBetween(50L, 100L, random);
            assertTrue(value >= 50L && value <= 100L, "fuera del margen: " + value);
            seen.add(value);
        }
        assertTrue(seen.contains(50L) && seen.contains(100L), "los extremos deben poder salir");
        assertEquals(51, seen.size(), "deberían salir todos los valores del margen");
    }

    @Test
    void anEmptyOrUpsideDownRangeGivesTheMinimum() {
        RandomGenerator random = new Random(14);
        assertEquals(60L, CursorGlide.randomBetween(60L, 60L, random));
        assertEquals(60L, CursorGlide.randomBetween(60L, 10L, random));
        assertEquals(0L, CursorGlide.randomBetween(0L, 0L, random));
    }
}
