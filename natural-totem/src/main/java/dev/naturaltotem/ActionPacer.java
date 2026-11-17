// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; el intervalo ahora se puede cambiar en caliente.
package dev.naturaltotem;

/**
 * Impone un tiempo mínimo entre acciones.
 *
 * <p>Las comparaciones usan restas de {@code System.nanoTime()}, de modo que siguen siendo
 * correctas cuando el reloj desborda. {@link #ready(long)} no cambia el estado: solo
 * {@link #defer(long)} marca que acaba de hacerse una acción.
 */
public final class ActionPacer {

    private volatile long interval;
    private boolean waiting;
    private long lastAction;

    /** @param interval espera mínima en nanosegundos, siempre positiva */
    public ActionPacer(long interval) {
        if (interval <= 0L) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        this.interval = interval;
    }

    /** @return espera mínima en nanosegundos */
    public long interval() {
        return this.interval;
    }

    /**
     * Cambia la espera mínima sin perder la hora de la última acción, para que un cambio de
     * configuración se note en la siguiente acción y no reinicie la cuenta.
     *
     * @throws IllegalArgumentException si el intervalo no es positivo
     */
    public void setInterval(long interval) {
        if (interval <= 0L) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        this.interval = interval;
    }

    /** @return {@code true} si ya puede hacerse la siguiente acción */
    public boolean ready(long now) {
        return !this.waiting || now - this.lastAction >= this.interval;
    }

    /** Anota que se acaba de actuar; la siguiente acción esperará el intervalo completo. */
    public void defer(long now) {
        this.lastAction = now;
        this.waiting = true;
    }

    /** Olvida la última acción: la siguiente puede ocurrir de inmediato. */
    public void clear() {
        this.waiting = false;
    }
}
