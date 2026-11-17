package dev.naturaltotem.config;

/**
 * Persistencia de la configuración, vista desde el comando.
 *
 * <p>Está separada en una interfaz para que el comando no dependa del disco y las pruebas
 * puedan ejecutarlo entero en memoria.
 */
public interface ConfigStore {

    /** Guarda y devuelve un mensaje para el jugador. */
    String save();

    /** Vuelve a leer el archivo y devuelve un mensaje para el jugador. */
    String reload();

    /** Guarda sin molestar: se llama después de cada cambio del comando. */
    void autoSave();

    /** Ruta mostrada al jugador; puede ser cualquier texto descriptivo. */
    String location();

    /** Implementación que no toca el disco, útil para pruebas y como reserva si falla el archivo. */
    final class InMemory implements ConfigStore {
        private final TotemConfig config;
        private TotemConfig snapshot;
        private int saves;
        private int reloads;

        public InMemory(TotemConfig config) {
            this.config = config;
            this.snapshot = config.copy();
        }

        @Override
        public String save() {
            snapshot = config.copy();
            saves++;
            return "Configuración guardada en memoria.";
        }

        @Override
        public String reload() {
            config.copyFrom(snapshot);
            reloads++;
            return "Configuración recargada desde memoria.";
        }

        @Override
        public void autoSave() {
            snapshot = config.copy();
            saves++;
        }

        @Override
        public String location() {
            return "(memoria)";
        }

        public int saves() {
            return saves;
        }

        public int reloads() {
            return reloads;
        }
    }
}
