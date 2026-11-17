package dev.naturaltotem;

import dev.naturaltotem.command.TotemCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Mensajes del mod en el chat del cliente.
 *
 * <p>Nada de esto se envía al servidor: {@code ChatHud.addMessage} solo escribe en la ventana
 * de chat de quien juega.
 */
public final class ChatOutput {

    private static final Text PREFIX = Text.literal("[Natural Totem] ").formatted(Formatting.AQUA);

    private ChatOutput() {
    }

    public static void info(MinecraftClient client, String line) {
        write(client, line, Formatting.GRAY);
    }

    public static void success(MinecraftClient client, String line) {
        write(client, line, Formatting.GREEN);
    }

    public static void error(MinecraftClient client, String line) {
        write(client, line, Formatting.RED);
    }

    public static void header(MinecraftClient client, String line) {
        write(client, line, Formatting.WHITE);
    }

    private static void write(MinecraftClient client, String line, Formatting color) {
        if (client == null || client.inGameHud == null) {
            return;
        }
        client.inGameHud.getChatHud().addMessage(Text.empty().append(PREFIX).append(Text.literal(line).formatted(color)));
    }

    /** Respuestas del comando dirigidas al chat. */
    public static TotemCommand.Feedback feedback(MinecraftClient client) {
        return new TotemCommand.Feedback() {
            @Override
            public void info(String line) {
                ChatOutput.info(client, line);
            }

            @Override
            public void success(String line) {
                ChatOutput.success(client, line);
            }

            @Override
            public void error(String line) {
                ChatOutput.error(client, line);
            }

            @Override
            public void header(String line) {
                ChatOutput.header(client, line);
            }
        };
    }
}
