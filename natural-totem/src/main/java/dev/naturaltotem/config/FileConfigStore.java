package dev.naturaltotem.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Configuración guardada en un archivo de texto.
 *
 * <p>Recibe la ruta ya resuelta, así que no depende de Minecraft ni del cargador y las
 * pruebas la ejecutan sobre un directorio temporal.
 */
public final class FileConfigStore implements ConfigStore {

    private final Path file;
    private final TotemConfig config;
    private final Consumer<String> log;

    public FileConfigStore(Path file, TotemConfig config, Consumer<String> log) {
        this.file = file;
        this.config = config;
        this.log = log;
    }

    /**
     * Lee el archivo si existe y lo crea con los valores por defecto si no.
     *
     * @return avisos del archivo; una lista vacía significa que todo se leyó bien
     */
    public List<String> load() {
        List<String> warnings = new ArrayList<>();
        try {
            if (!Files.exists(file)) {
                writeFile();
                return warnings;
            }
            warnings.addAll(ConfigFormat.read(Files.readAllLines(file, StandardCharsets.UTF_8), config));
        } catch (IOException | UncheckedIOException error) {
            warnings.add("no se pudo leer " + file + ": " + error.getMessage());
        }
        return warnings;
    }

    @Override
    public String save() {
        try {
            writeFile();
            return "Configuración guardada en " + file + ".";
        } catch (IOException error) {
            log.accept("No se pudo guardar la configuración en " + file + ": " + error);
            return "No se pudo guardar: " + error.getMessage();
        }
    }

    @Override
    public String reload() {
        TotemConfig previous = config.copy();
        config.resetAll();
        List<String> warnings = load();
        if (warnings.isEmpty()) {
            return "Configuración recargada desde " + file + ".";
        }
        config.copyFrom(previous);
        for (String warning : warnings) {
            log.accept("Configuración: " + warning);
        }
        return "El archivo tiene errores, se conservan los ajustes actuales: " + warnings.getFirst();
    }

    @Override
    public void autoSave() {
        try {
            writeFile();
        } catch (IOException error) {
            log.accept("No se pudo guardar la configuración en " + file + ": " + error);
        }
    }

    @Override
    public String location() {
        return file.toString();
    }

    private void writeFile() throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, ConfigFormat.write(config), StandardCharsets.UTF_8);
    }
}
