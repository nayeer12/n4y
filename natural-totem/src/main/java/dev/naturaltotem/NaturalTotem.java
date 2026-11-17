// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; ampliado con configuración y comando.
package dev.naturaltotem;

import dev.naturaltotem.command.TotemCommand;
import dev.naturaltotem.config.ConfigStore;
import dev.naturaltotem.config.FileConfigStore;
import dev.naturaltotem.config.StartupOverrides;
import dev.naturaltotem.config.TotemConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Punto de unión del mod: guarda la configuración, el servicio de rellenado y el comando,
 * y es lo único que llaman los mixins.
 */
public final class NaturalTotem {

    public static final String MOD_ID = "natural_totem";
    public static final String MOD_NAME = "Natural Totem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static final TotemConfig CONFIG = new TotemConfig();
    private static final TotemService SERVICE = new TotemService(CONFIG);

    private static volatile boolean initialized;
    private static ConfigStore store = new ConfigStore.InMemory(CONFIG);
    private static TotemCommand command = new TotemCommand(CONFIG, store);

    private NaturalTotem() {
    }

    public static TotemConfig config() {
        return CONFIG;
    }

    public static TotemCommand command() {
        ensureInitialized();
        return command;
    }

    public static ConfigStore store() {
        ensureInitialized();
        return store;
    }

    /**
     * Lee el archivo de configuración y aplica los parámetros de arranque.
     * La llama el punto de entrada del cliente; es idempotente.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Path file = configFile();
        FileConfigStore fileStore = new FileConfigStore(file, CONFIG, LOGGER::warn);
        for (String warning : fileStore.load()) {
            LOGGER.warn("Configuración: {}", warning);
        }
        store = fileStore;
        command = new TotemCommand(CONFIG, store);

        StartupOverrides.Result overrides = StartupOverrides.applyFromProcess(CONFIG);
        for (String applied : overrides.applied()) {
            LOGGER.info("Ajuste de arranque: {}", applied);
        }
        for (String warning : overrides.warnings()) {
            LOGGER.warn("Ajuste de arranque: {}", warning);
        }

        // Un cambio de ajustes vuelve a abrir la pasada: el efecto se nota sin cerrar el inventario.
        CONFIG.onChange(option -> SERVICE.restartPass());

        LOGGER.info("{} listo. Configuración: {}. Comando: /{}.", MOD_NAME, file, TotemCommand.NAME);
    }

    private static Path configFile() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
        } catch (RuntimeException error) {
            LOGGER.warn("No se pudo resolver el directorio de configuración: {}", error.toString());
            return Path.of("config", MOD_ID + ".properties");
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    /** Llamado al final de cada tick del cliente. */
    public static void tick(MinecraftClient client) {
        ensureInitialized();
        SERVICE.tick(client);
    }

    /** Llamado al principio de cada fotograma. */
    public static void animate(MinecraftClient client) {
        if (!initialized) {
            return;
        }
        SERVICE.animate(client);
    }

    /**
     * Atiende un comando escrito en el chat.
     *
     * @param rawCommand texto sin la barra inicial, tal como lo envía el cliente
     * @return {@code true} si era un comando del mod y no debe mandarse al servidor
     */
    public static boolean handleChatCommand(String rawCommand) {
        ensureInitialized();
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            return command.handle(rawCommand, ChatOutput.feedback(client));
        } catch (RuntimeException error) {
            LOGGER.error("Error al ejecutar /{}", rawCommand, error);
            ChatOutput.error(client, "El comando falló: " + error);
            return true;
        }
    }

    /** Alias reconocidos, para el autocompletado. */
    public static List<String> commandAliases() {
        return TotemCommand.ALIASES;
    }
}
