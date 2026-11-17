package dev.naturaltotem;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Differential checks against the uploaded original, not a recreated behavior specification. */
class BinaryParityTest {
    private static URLClassLoader originalLoader;
    private static Method originalNext, originalSource, originalDestination, originalReady, originalDefer;
    private static Constructor<?> originalPacer;

    @BeforeAll
    static void loadOriginal() throws Exception {
        Path jar = Path.of("original", "natural-totem-1.4.0.jar").toAbsolutePath();
        originalLoader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        Class<?> planner = originalLoader.loadClass("dev.naturaltotem.RefillPlanner");
        Class<?> move = originalLoader.loadClass("dev.naturaltotem.RefillPlanner$Move");
        Class<?> pacer = originalLoader.loadClass("dev.naturaltotem.ActionPacer");
        assertNotSame(RefillPlanner.class, planner, "The original must use an isolated class loader");
        originalNext = planner.getMethod("next", boolean[].class, boolean.class);
        originalSource = move.getMethod("source");
        originalDestination = move.getMethod("destination");
        originalPacer = pacer.getConstructor(long.class);
        originalReady = pacer.getMethod("ready", long.class);
        originalDefer = pacer.getMethod("defer", long.class);
    }

    @AfterAll
    static void closeOriginal() throws Exception {
        if (originalLoader != null) originalLoader.close();
    }

    private static void comparePlan(boolean[] inventory, boolean offhand) throws Exception {
        Object expected = originalNext.invoke(null, inventory, offhand);
        RefillPlanner.Move actual = RefillPlanner.next(inventory, offhand);
        if (expected == null) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
            assertEquals((int) originalSource.invoke(expected), actual.source());
            assertEquals((int) originalDestination.invoke(expected), actual.destination());
        }
    }

    @Test
    void plannerMatchesEveryInventoryWithZeroOneOrTwoTotems() throws Exception {
        for (boolean offhand : new boolean[]{false, true}) {
            comparePlan(new boolean[36], offhand);
            for (int first = 0; first < 36; first++) {
                boolean[] inventory = new boolean[36];
                inventory[first] = true;
                comparePlan(inventory, offhand);
                for (int second = first + 1; second < 36; second++) {
                    inventory[second] = true;
                    comparePlan(inventory, offhand);
                    inventory[second] = false;
                }
            }
        }
    }

    @Test
    void plannerMatchesRandomInventoriesAndSuccessiveRefills() throws Exception {
        Random random = new Random(0x14_00_2026L);
        for (int example = 0; example < 5000; example++) {
            boolean[] inventory = new boolean[36];
            double density = random.nextDouble();
            for (int slot = 0; slot < inventory.length; slot++) inventory[slot] = random.nextDouble() < density;
            boolean offhand = random.nextBoolean();
            for (int step = 0; step < 3; step++) {
                comparePlan(inventory, offhand);
                var move = RefillPlanner.next(inventory, offhand);
                if (move == null) break;
                if (move.destination() == 40) {
                    inventory[move.source()] = offhand;
                    offhand = true;
                } else {
                    inventory[move.source()] = inventory[move.destination()];
                    inventory[move.destination()] = true;
                }
            }
        }
    }

    @Test
    void plannerRejectsInvalidInputsExactlyLikeOriginal() {
        for (int length : new int[]{0, 1, 8, 35, 37, 72}) {
            boolean[] inventory = new boolean[length];
            Throwable expected = assertThrows(InvocationTargetException.class,
                    () -> originalNext.invoke(null, inventory, false)).getCause();
            Throwable actual = assertThrows(Throwable.class, () -> RefillPlanner.next(inventory, false));
            assertEquals(expected.getClass(), actual.getClass());
            assertEquals(expected.getMessage(), actual.getMessage());
        }
        Throwable expected = assertThrows(InvocationTargetException.class,
                () -> originalNext.invoke(null, (boolean[]) null, false)).getCause();
        assertEquals(expected.getClass(), assertThrows(Throwable.class, () -> RefillPlanner.next(null, false)).getClass());
    }

    @Test
    void pacerRejectsNonPositiveIntervalsExactlyLikeOriginal() {
        for (long interval : new long[]{Long.MIN_VALUE, -100, -1, 0}) {
            Throwable expected = assertThrows(InvocationTargetException.class,
                    () -> originalPacer.newInstance(interval)).getCause();
            Throwable actual = assertThrows(Throwable.class, () -> new ActionPacer(interval));
            assertEquals(expected.getClass(), actual.getClass());
            assertEquals(expected.getMessage(), actual.getMessage());
        }
    }

    private static void compareReady(Object expected, ActionPacer actual, long now) throws Exception {
        assertEquals((boolean) originalReady.invoke(expected, now), actual.ready(now), "time=" + now);
    }

    @Test
    void pacerMatchesBoundariesAndSignedClockWraparound() throws Exception {
        for (long interval : new long[]{1, 100_000_000L, Long.MAX_VALUE}) {
            for (long origin : new long[]{0, 12345, Long.MAX_VALUE - 20}) {
                Object expected = originalPacer.newInstance(interval);
                ActionPacer actual = new ActionPacer(interval);
                compareReady(expected, actual, Long.MIN_VALUE);
                originalDefer.invoke(expected, origin);
                actual.defer(origin);
                for (long offset : new long[]{0, interval - 1, interval, interval + 1}) {
                    compareReady(expected, actual, origin + offset);
                }
            }
        }
    }

    @Test
    void pacerMatchesRandomActionAndQueryTraces() throws Exception {
        Random random = new Random(0x50_100_140L);
        for (int trace = 0; trace < 20; trace++) {
            long interval = 1 + random.nextLong(500_000_000L);
            Object expected = originalPacer.newInstance(interval);
            ActionPacer actual = new ActionPacer(interval);
            long now = trace % 2 == 0 ? 0 : Long.MAX_VALUE - 500_000_000L;
            for (int event = 0; event < 2000; event++) {
                now += random.nextLong(200_000_000L);
                if (random.nextInt(5) == 0) {
                    originalDefer.invoke(expected, now);
                    actual.defer(now);
                }
                compareReady(expected, actual, now);
            }
        }
    }

    @Test
    void pacerQueriesAndLongStallsHaveNoExtraStateChanges() throws Exception {
        Object expected = originalPacer.newInstance(100_000_000L);
        ActionPacer actual = new ActionPacer(100_000_000L);
        originalDefer.invoke(expected, 0L);
        actual.defer(0L);
        for (int repeat = 0; repeat < 100; repeat++) compareReady(expected, actual, 100_000_000L);
        long afterStall = 10_000_000_000L;
        compareReady(expected, actual, afterStall);
        originalDefer.invoke(expected, afterStall);
        actual.defer(afterStall);
        for (int repeat = 0; repeat < 100; repeat++) compareReady(expected, actual, afterStall);
        compareReady(expected, actual, afterStall + 100_000_000L);
    }
}
