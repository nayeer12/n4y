package dev.naturaltotem;

/**
 * Acercamiento del cursor a una casilla: solo aritmética, sin estado ni Minecraft.
 *
 * <p>Es la misma curva del JAR 1.4.0 (suavizado {@code 3t² − 2t³}), extraída aquí para poder
 * comprobarla y para que la duración deje de estar escrita a mano en dos sitios.
 */
public final class CursorGlide {

    private CursorGlide() {
    }

    /**
     * @param elapsedNanos  tiempo transcurrido desde que empezó el movimiento
     * @param durationNanos duración total; {@code 0} o menos significa inmediato
     * @return avance entre 0 y 1
     */
    public static double progress(long elapsedNanos, long durationNanos) {
        if (durationNanos <= 0L) {
            return 1.0;
        }
        if (elapsedNanos <= 0L) {
            return 0.0;
        }
        if (elapsedNanos >= durationNanos) {
            return 1.0;
        }
        return (double) elapsedNanos / (double) durationNanos;
    }

    /** Suavizado: arranca y termina despacio. */
    public static double ease(double progress) {
        double clamped = Math.clamp(progress, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    /** Posición intermedia entre {@code from} y {@code to} para un avance ya suavizado. */
    public static double interpolate(double from, double to, double eased) {
        return from + (to - from) * eased;
    }

    /**
     * Pasa una coordenada de la interfaz (píxeles escalados) a píxeles reales de la ventana.
     *
     * @param guiCoordinate posición en la interfaz
     * @param windowSize    tamaño real de la ventana en píxeles
     * @param scaledSize    tamaño de la interfaz en píxeles escalados
     */
    public static double toWindowPixels(double guiCoordinate, int windowSize, int scaledSize) {
        if (scaledSize <= 0) {
            return guiCoordinate;
        }
        return guiCoordinate * (double) windowSize / (double) scaledSize;
    }
}
