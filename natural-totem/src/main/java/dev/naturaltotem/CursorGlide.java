package dev.naturaltotem;

import java.util.random.RandomGenerator;

/**
 * Acercamiento del cursor a una casilla: solo aritmética, sin estado ni Minecraft.
 *
 * <p>El recorrido no es una línea recta. Arranca y termina despacio (suavizado
 * {@code 3t² − 2t³}, el del JAR 1.4.0), se curva un poco hacia un lado y no apunta siempre al
 * centro exacto de la casilla, que es como se mueve una mano. La duración de cada acercamiento
 * se sortea dentro del margen configurado.
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

    /**
     * Sortea un número entre {@code min} y {@code max}, ambos incluidos. Si el máximo es menor
     * que el mínimo se usa el mínimo, de modo que un margen mal escrito no deja el mod parado.
     */
    public static long randomBetween(long min, long max, RandomGenerator random) {
        long high = Math.max(min, max);
        if (high == min) {
            return min;
        }
        return random.nextLong(min, high + 1);
    }

    /**
     * Punto al que se apunta dentro de la casilla: el centro, movido al azar sin salirse.
     *
     * @param jitter desviación máxima en píxeles de interfaz
     */
    public static double aimOffset(double jitter, RandomGenerator random) {
        if (jitter <= 0.0) {
            return 0.0;
        }
        return (random.nextDouble() * 2.0 - 1.0) * jitter;
    }

    /**
     * Cuánto se separa el recorrido de la línea recta, en tanto por uno de su longitud, con el
     * lado sorteado. Un movimiento muy corto se deja recto: curvarlo se vería como un tirón.
     */
    public static double bend(double curvature, double distance, RandomGenerator random) {
        if (curvature <= 0.0 || distance < 24.0) {
            return 0.0;
        }
        double magnitude = curvature * (0.4 + 0.6 * random.nextDouble());
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    /**
     * Recorrido curvo entre dos puntos.
     *
     * <p>Es una curva de Bézier cuadrática cuyo punto de control es el centro del trayecto
     * apartado perpendicularmente. Con {@code bend} igual a 0 es exactamente la línea recta,
     * así que el movimiento del JAR 1.4.0 sigue siendo un caso de esta misma curva.
     *
     * @param bend separación del punto de control, en tanto por uno de la distancia; el signo es el lado
     */
    public record Curve(double startX, double startY, double endX, double endY, double bend) {

        public double x(double eased) {
            return at(startX, controlX(), endX, Math.clamp(eased, 0.0, 1.0));
        }

        public double y(double eased) {
            return at(startY, controlY(), endY, Math.clamp(eased, 0.0, 1.0));
        }

        private double controlX() {
            return (startX + endX) / 2.0 - (endY - startY) * bend;
        }

        private double controlY() {
            return (startY + endY) / 2.0 + (endX - startX) * bend;
        }

        private static double at(double from, double control, double to, double t) {
            double inverse = 1.0 - t;
            return inverse * inverse * from + 2.0 * inverse * t * control + t * t * to;
        }

        /** Longitud de la línea recta entre los extremos. */
        public double distance() {
            return Math.hypot(endX - startX, endY - startY);
        }
    }
}
