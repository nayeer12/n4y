/*
 * Decompiled with CFR 0.152.
 */
package dev.naturaltotem;

public final class RefillPlanner {
    public static Move next(boolean[] totems, boolean offhandTotem) {
        int i;
        int destination;
        if (totems.length != 36) {
            throw new IllegalArgumentException("Expected 36 inventory slots");
        }
        int n = !totems[8] ? 8 : (destination = !offhandTotem ? 40 : -1);
        if (destination == -1) {
            return null;
        }
        for (i = 9; i < 36; ++i) {
            if (!totems[i]) continue;
            return new Move(i, destination);
        }
        for (i = 0; i < 8; ++i) {
            if (!totems[i]) continue;
            return new Move(i, destination);
        }
        return null;
    }

    public record Move(int source, int destination) {
    }
}

