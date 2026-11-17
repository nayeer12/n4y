/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_1723
 *  net.minecraft.class_1735
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_490
 *  org.lwjgl.glfw.GLFW
 */
package dev.naturaltotem;

import dev.naturaltotem.ActionPacer;
import dev.naturaltotem.RefillPlanner;
import dev.naturaltotem.mixin.HandledScreenAccess;
import net.minecraft.class_1657;
import net.minecraft.class_1713;
import net.minecraft.class_1723;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_490;
import org.lwjgl.glfw.GLFW;

public final class NaturalTotem {
    private static final ActionPacer PACER = new ActionPacer(100000000L);
    private static final long MOVE_NANOS = 60000000L;
    private static class_490 active;
    private static class_1735 source;
    private static class_1735 destination;
    private static class_1735 motionSlot;
    private static long motionStarted;
    private static double startX;
    private static double startY;
    private static boolean openedDone;
    private static boolean selectionDone;

    private static void resetTransaction() {
        motionSlot = null;
        destination = null;
        source = null;
    }

    private static void cancelOpening() {
        NaturalTotem.resetTransaction();
        openedDone = true;
    }

    public static void tick(class_310 c) {
        class_490 inventory;
        block23: {
            block22: {
                class_437 class_4372;
                if (c.field_1724 == null || c.field_1761 == null || !((class_4372 = c.field_1755) instanceof class_490)) break block22;
                inventory = (class_490)class_4372;
                if (!c.field_1724.method_7325()) break block23;
            }
            active = null;
            NaturalTotem.resetTransaction();
            return;
        }
        if (active != inventory) {
            active = inventory;
            selectionDone = false;
            openedDone = false;
            NaturalTotem.resetTransaction();
        }
        if (!c.method_1569()) {
            NaturalTotem.cancelOpening();
            return;
        }
        if (openedDone) {
            return;
        }
        class_1723 handler = (class_1723)inventory.method_17577();
        HandledScreenAccess access = (HandledScreenAccess)inventory;
        if (source == null) {
            if (!handler.method_34255().method_7960()) {
                NaturalTotem.cancelOpening();
                return;
            }
            boolean[] totems = new boolean[36];
            for (int i = 0; i < 36; ++i) {
                totems[i] = c.field_1724.method_31548().method_5438(i).method_31574(class_1802.field_8288);
            }
            RefillPlanner.Move move = RefillPlanner.next(totems, c.field_1724.method_6079().method_31574(class_1802.field_8288));
            if (move == null) {
                if (totems[8] && !selectionDone && c.field_1724.method_31548().method_67532() != 8) {
                    if (!PACER.ready(System.nanoTime())) {
                        return;
                    }
                    c.field_1724.method_31548().method_61496(8);
                    PACER.defer(System.nanoTime());
                    selectionDone = true;
                }
                openedDone = true;
                return;
            }
            for (class_1735 slot : handler.field_7761) {
                if (slot.field_7871 != c.field_1724.method_31548()) continue;
                if (slot.method_34266() == move.source()) {
                    source = slot;
                }
                if (slot.method_34266() != move.destination()) continue;
                destination = slot;
            }
            if (source == null || destination == null || !source.method_7674((class_1657)c.field_1724)) {
                NaturalTotem.cancelOpening();
                return;
            }
        }
        if (!handler.method_34255().method_7960()) {
            NaturalTotem.cancelOpening();
            return;
        }
        if (!NaturalTotem.moveCursor(c, access, source)) {
            return;
        }
        if (!PACER.ready(System.nanoTime())) {
            return;
        }
        if (GLFW.glfwGetMouseButton((long)c.method_22683().method_4490(), (int)0) == 1 || GLFW.glfwGetMouseButton((long)c.method_22683().method_4490(), (int)1) == 1) {
            return;
        }
        if (!source.method_7677().method_31574(class_1802.field_8288) || destination.method_7677().method_31574(class_1802.field_8288) || !source.method_7674((class_1657)c.field_1724) || !destination.method_7674((class_1657)c.field_1724) || !destination.method_7680(source.method_7677()) || destination.method_7681() && !source.method_7680(destination.method_7677())) {
            NaturalTotem.cancelOpening();
            return;
        }
        int swapButton = destination.method_34266();
        if (swapButton != 8 && swapButton != 40) {
            NaturalTotem.cancelOpening();
            return;
        }
        class_1799 displaced = destination.method_7677().method_7972();
        access.naturalTotem$click(source, NaturalTotem.source.field_7874, swapButton, class_1713.field_7791);
        PACER.defer(System.nanoTime());
        if (!(destination.method_7677().method_31574(class_1802.field_8288) && class_1799.method_7973((class_1799)source.method_7677(), (class_1799)displaced) && handler.method_34255().method_7960())) {
            NaturalTotem.cancelOpening();
            return;
        }
        NaturalTotem.resetTransaction();
    }

    private static boolean moveCursor(class_310 c, HandledScreenAccess access, class_1735 slot) {
        if (motionSlot != slot) {
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos((long)c.method_22683().method_4490(), (double[])x, (double[])y);
            startX = x[0];
            startY = y[0];
            motionStarted = System.nanoTime();
            motionSlot = slot;
        }
        NaturalTotem.animate(c);
        return System.nanoTime() - motionStarted >= 60000000L;
    }

    public static void animate(class_310 c) {
        if (motionSlot == null || active == null || c.field_1755 != active || !c.method_1569()) {
            return;
        }
        HandledScreenAccess access = (HandledScreenAccess)active;
        double tx = ((double)(access.naturalTotem$x() + NaturalTotem.motionSlot.field_7873) + 8.0) * (double)c.method_22683().method_4480() / (double)c.method_22683().method_4486();
        double ty = ((double)(access.naturalTotem$y() + NaturalTotem.motionSlot.field_7872) + 8.0) * (double)c.method_22683().method_4507() / (double)c.method_22683().method_4502();
        double t = Math.clamp((double)(System.nanoTime() - motionStarted) / 6.0E7, 0.0, 1.0);
        double eased = t * t * (3.0 - 2.0 * t);
        GLFW.glfwSetCursorPos((long)c.method_22683().method_4490(), (double)(startX + (tx - startX) * eased), (double)(startY + (ty - startY) * eased));
    }
}

