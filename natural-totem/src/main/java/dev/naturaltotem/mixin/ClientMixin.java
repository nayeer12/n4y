// Recuperado del JAR 1.4.0 con CFR y nombres Yarn.
package dev.naturaltotem.mixin;

import dev.naturaltotem.NaturalTotem;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Engancha el mod al bucle del cliente: la lógica en cada tick y el movimiento del cursor
 * en cada fotograma, para que no dependa de la frecuencia de ticks.
 */
@Mixin(MinecraftClient.class)
public abstract class ClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void naturalTotem$tick(CallbackInfo ci) {
        NaturalTotem.tick((MinecraftClient) (Object) this);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void naturalTotem$render(boolean tick, CallbackInfo ci) {
        NaturalTotem.animate((MinecraftClient) (Object) this);
    }
}
