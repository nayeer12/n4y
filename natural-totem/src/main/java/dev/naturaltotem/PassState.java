package dev.naturaltotem;

/**
 * En qué punto está el mod mientras el inventario está abierto.
 *
 * <p>Separa dos maneras de trabajar. Con {@code continuous} apagado se comporta como el JAR
 * 1.4.0: una sola pasada por apertura, y cualquier contratiempo la da por terminada hasta que
 * se vuelva a abrir el inventario. Con {@code continuous} encendido el mod se queda mirando:
 * si el tótem estalla o alguien vacía la casilla sin cerrar la pantalla, vuelve a rellenarla.
 *
 * <p>Que siga mirando no significa insistir sin parar. Un contratiempo pasajero —un objeto en
 * el cursor, el ratón pulsado, el foco perdido— solo aparta el intento durante un momento, y
 * los fallos de verdad se cuentan: después de varios seguidos se para hasta la siguiente
 * apertura, para no quedarse dando vueltas sobre un inventario que no responde como se espera.
 *
 * <p>No tiene nada de Minecraft, así que las pruebas recorren estos estados sin abrir el juego.
 */
public final class PassState {

    /** Fallos seguidos que se toleran en una misma apertura antes de parar. */
    public static final int MAX_FAILURES = 5;

    private boolean stopped;
    private boolean waiting;
    private long retryAt;
    private int failures;
    private boolean selectionDone;

    /** Pantalla nueva: empieza todo de cero. */
    public void beginOpening() {
        stopped = false;
        waiting = false;
        retryAt = 0L;
        failures = 0;
        selectionDone = false;
    }

    /**
     * @return {@code true} si toca mirar el inventario en este tick
     */
    public boolean canAct(long now) {
        if (stopped) {
            return false;
        }
        if (waiting) {
            if (now - retryAt < 0L) {
                return false;
            }
            waiting = false;
        }
        return true;
    }

    /**
     * Contratiempo pasajero: se deja pasar un momento y se vuelve a intentar.
     * Sin {@code continuous} no hay reintento: la pasada termina aquí.
     */
    public void pause(boolean continuous, long retryNanos, long now) {
        if (!continuous) {
            stopped = true;
            return;
        }
        waiting = true;
        retryAt = now + Math.max(0L, retryNanos);
    }

    /**
     * Fallo de verdad: el inventario no quedó como se esperaba. Se reintenta igual que una
     * pausa, pero se cuenta, y a los {@value #MAX_FAILURES} seguidos se para.
     *
     * @return {@code true} si este fallo ha parado el mod hasta la siguiente apertura
     */
    public boolean fail(boolean continuous, long retryNanos, long now) {
        failures++;
        if (!continuous || failures >= MAX_FAILURES) {
            stopped = true;
            return true;
        }
        waiting = true;
        retryAt = now + Math.max(0L, retryNanos);
        return false;
    }

    /** Un intercambio salió bien: se olvidan los fallos y se vuelve a mirar la casilla seleccionada. */
    public void succeeded() {
        failures = 0;
        selectionDone = false;
    }

    /**
     * No queda nada que rellenar. Sin {@code continuous} la pasada termina; con él, el mod se
     * queda mirando por si el tótem estalla.
     */
    public void finishedPass(boolean continuous) {
        if (!continuous) {
            stopped = true;
        }
    }

    public boolean selectionDone() {
        return selectionDone;
    }

    public void markSelectionDone() {
        selectionDone = true;
    }

    /** Vuelve a permitir intentos en la pantalla que ya está abierta, por ejemplo tras cambiar un ajuste. */
    public void restart() {
        stopped = false;
        waiting = false;
        retryAt = 0L;
        failures = 0;
    }

    public boolean stopped() {
        return stopped;
    }

    public int failures() {
        return failures;
    }
}
