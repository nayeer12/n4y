package dev.naturaltotem.mixin;

import com.mojang.brigadier.CommandDispatcher;
import dev.naturaltotem.NaturalTotem;
import dev.naturaltotem.command.CommandTree;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Atiende {@code /ntotem} en el cliente.
 *
 * <p>El comando se intercepta justo antes de enviarse, de modo que no sale ningún paquete
 * hacia el servidor: para el servidor ese comando nunca se escribió.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientCommandMixin {

    @Shadow
    public abstract CommandDispatcher<ClientCommandSource> getCommandDispatcher();

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void naturalTotem$interceptCommand(String command, CallbackInfo ci) {
        if (NaturalTotem.handleChatCommand(command)) {
            ci.cancel();
        }
    }

    /**
     * Añade el comando al árbol de sugerencias cada vez que el servidor envía el suyo.
     * Es solo comodidad al escribir, así que un fallo aquí no debe romper la conexión.
     */
    @Inject(method = "onCommandTree", at = @At("TAIL"), require = 0)
    private void naturalTotem$addSuggestions(CommandTreeS2CPacket packet, CallbackInfo ci) {
        try {
            CommandTree.register(getCommandDispatcher());
        } catch (RuntimeException error) {
            NaturalTotem.LOGGER.warn("No se pudo registrar el autocompletado del comando: {}", error.toString());
        }
    }
}
