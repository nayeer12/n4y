package dev.naturaltotem;

import net.fabricmc.api.ClientModInitializer;

/**
 * Punto de entrada del cliente: solo carga la configuración al arrancar el juego.
 *
 * <p>Usa la interfaz del cargador de Fabric, no Fabric API, así que el mod sigue sin
 * dependencias aparte del propio cargador.
 */
public final class NaturalTotemClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NaturalTotem.init();
    }
}
