/*
 * Decompiled with CFR 0.152.
 */
package dev.naturaltotem;

public final class ActionPacer {
    private final long interval;
    private boolean waiting;
    private long lastAction;

    public ActionPacer(long interval) {
        if (interval <= 0L) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        this.interval = interval;
    }

    public boolean ready(long now) {
        return !this.waiting || now - this.lastAction >= this.interval;
    }

    public void defer(long now) {
        this.lastAction = now;
        this.waiting = true;
    }
}

