// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; reorganizado y conectado a la configuración.
package dev.naturaltotem;

import dev.naturaltotem.config.Options;
import dev.naturaltotem.config.TotemConfig;
import dev.naturaltotem.mixin.HandledScreenAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * El rellenado en sí: mira el inventario abierto, pide un plan y lo ejecuta.
 *
 * <p>Conserva el comportamiento del JAR 1.4.0 —una sola pasada por apertura del inventario,
 * cancelación si llevas algo en el cursor o se pierde el foco, intercambio con
 * {@link SlotActionType#SWAP}— pero lee de {@link TotemConfig} todo lo que antes estaba
 * escrito a mano: casilla, destinos, objeto y tiempos.
 *
 * <p>Toda la decisión está en {@link RefillPlanner} y toda la espera en {@link ActionPacer};
 * esta clase solo traduce entre esas dos y los objetos del juego.
 */
public final class TotemService {

    private final TotemConfig config;
    private final ActionPacer pacer;

    /** Pantalla a la que corresponde el estado actual; sirve para detectar una apertura nueva. */
    private InventoryScreen active;
    private Slot source;
    private Slot destination;

    /** Casilla hacia la que se está moviendo el cursor. */
    private Slot motionSlot;
    private long motionStarted;
    private double startX;
    private double startY;

    /** La pasada de esta apertura ya terminó (o se canceló): no se vuelve a intentar. */
    private boolean passDone;
    private boolean selectionDone;

    private String cachedItemId;
    private Item cachedItem = Items.TOTEM_OF_UNDYING;
    private boolean warnedAboutItem;

    public TotemService(TotemConfig config) {
        this.config = config;
        this.pacer = new ActionPacer(config.actionIntervalNanos());
    }

    /** Se llama al final de cada tick del cliente. */
    public void tick(MinecraftClient client) {
        if (!(client.currentScreen instanceof InventoryScreen inventory)
                || client.player == null
                || client.interactionManager == null
                || client.player.isSpectator()) {
            forget();
            return;
        }
        if (active != inventory) {
            active = inventory;
            selectionDone = false;
            passDone = false;
            resetTransaction();
        }
        if (!config.enabled() || config.nothingToFill()) {
            return;
        }
        pacer.setInterval(config.actionIntervalNanos());
        if (config.requireFocus() && !client.isWindowFocused()) {
            cancelPass("la ventana perdió el foco");
            return;
        }
        if (passDone) {
            return;
        }

        PlayerScreenHandler handler = inventory.getScreenHandler();
        HandledScreenAccess access = (HandledScreenAccess) inventory;
        Item target = targetItem();

        if (source == null && !plan(client, handler, target)) {
            return;
        }
        if (!handler.getCursorStack().isEmpty()) {
            cancelPass("llevas un objeto en el cursor");
            return;
        }
        if (!approach(client, source)) {
            return;
        }
        if (!pacer.ready(System.nanoTime())) {
            return;
        }
        if (config.skipWhileClicking() && isMouseHeld(client)) {
            return;
        }
        swap(client, handler, access, target);
    }

    /**
     * Busca el siguiente intercambio y localiza sus casillas.
     *
     * @return {@code true} si hay un intercambio listo para ejecutarse
     */
    private boolean plan(MinecraftClient client, PlayerScreenHandler handler, Item target) {
        if (!handler.getCursorStack().isEmpty()) {
            cancelPass("llevas un objeto en el cursor");
            return false;
        }
        PlayerInventory inventory = client.player.getInventory();
        if (inventory.size() < RefillPlanner.MAIN_SLOTS) {
            cancelPass("el inventario no tiene el tamaño esperado");
            return false;
        }
        boolean[] present = new boolean[RefillPlanner.MAIN_SLOTS];
        for (int slot = 0; slot < present.length; slot++) {
            present[slot] = inventory.getStack(slot).isOf(target);
        }
        RefillPlanner.Move move = RefillPlanner.next(present, client.player.getOffHandStack().isOf(target), config.plan());
        if (move == null) {
            finishPass(client, present);
            return false;
        }
        for (Slot slot : handler.slots) {
            if (slot.inventory != inventory) {
                continue;
            }
            if (slot.getIndex() == move.source()) {
                source = slot;
            }
            if (slot.getIndex() == move.destination()) {
                destination = slot;
            }
        }
        if (source == null || destination == null || !source.canTakeItems(client.player)) {
            cancelPass("no se encontraron las casillas del plan");
            return false;
        }
        return true;
    }

    /** No queda nada que rellenar: selecciona la casilla si procede y cierra la pasada. */
    private void finishPass(MinecraftClient client, boolean[] present) {
        int slot = config.hotbarSlotIndex();
        boolean wantsSelection = config.get(Options.SELECT_SLOT)
                && config.get(Options.FILL_HOTBAR)
                && present[slot]
                && !selectionDone
                && client.player.getInventory().getSelectedSlot() != slot;
        if (wantsSelection) {
            if (!pacer.ready(System.nanoTime())) {
                return;
            }
            client.player.getInventory().setSelectedSlot(slot);
            pacer.defer(System.nanoTime());
            selectionDone = true;
        }
        passDone = true;
    }

    /** Ejecuta el intercambio y comprueba que el resultado es el esperado. */
    private void swap(MinecraftClient client, PlayerScreenHandler handler, HandledScreenAccess access, Item target) {
        if (!source.getStack().isOf(target)
                || destination.getStack().isOf(target)
                || !source.canTakeItems(client.player)
                || !destination.canTakeItems(client.player)
                || !destination.canInsert(source.getStack())
                || (destination.hasStack() && !source.canInsert(destination.getStack()))) {
            cancelPass("las casillas cambiaron antes del intercambio");
            return;
        }
        int swapButton = destination.getIndex();
        if (swapButton != RefillPlanner.OFFHAND_SLOT && (swapButton < 0 || swapButton > 8)) {
            cancelPass("destino inesperado: " + swapButton);
            return;
        }
        ItemStack displaced = destination.getStack().copy();
        access.naturalTotem$click(source, source.id, swapButton, SlotActionType.SWAP);
        pacer.defer(System.nanoTime());
        boolean asExpected = destination.getStack().isOf(target)
                && ItemStack.areEqual(source.getStack(), displaced)
                && handler.getCursorStack().isEmpty();
        if (!asExpected) {
            cancelPass("el intercambio no dejó el inventario como se esperaba");
            return;
        }
        announce(client, swapButton);
        resetTransaction();
    }

    /**
     * Acerca el cursor a la casilla.
     *
     * @return {@code true} cuando ya ha pasado el tiempo de acercamiento
     */
    private boolean approach(MinecraftClient client, Slot slot) {
        if (motionSlot != slot) {
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(client.getWindow().getHandle(), x, y);
            startX = x[0];
            startY = y[0];
            motionStarted = System.nanoTime();
            motionSlot = slot;
        }
        long duration = config.moveNanos();
        if (duration <= 0L) {
            return true;
        }
        animate(client);
        return System.nanoTime() - motionStarted >= duration;
    }

    /**
     * Mueve el cursor un poco más hacia la casilla. Se llama en cada fotograma para que el
     * movimiento sea suave y no dependa de los ticks.
     */
    public void animate(MinecraftClient client) {
        Slot slot = motionSlot;
        if (slot == null
                || active == null
                || client.currentScreen != active
                || !client.isWindowFocused()
                || !config.enabled()
                || !config.moveCursor()) {
            return;
        }
        long duration = config.moveNanos();
        if (duration <= 0L) {
            return;
        }
        HandledScreenAccess access = (HandledScreenAccess) active;
        double targetX = CursorGlide.toWindowPixels(access.naturalTotem$x() + slot.x + 8.0,
                client.getWindow().getWidth(), client.getWindow().getScaledWidth());
        double targetY = CursorGlide.toWindowPixels(access.naturalTotem$y() + slot.y + 8.0,
                client.getWindow().getHeight(), client.getWindow().getScaledHeight());
        double eased = CursorGlide.ease(CursorGlide.progress(System.nanoTime() - motionStarted, duration));
        GLFW.glfwSetCursorPos(client.getWindow().getHandle(),
                CursorGlide.interpolate(startX, targetX, eased),
                CursorGlide.interpolate(startY, targetY, eased));
    }

    /** El objeto configurado, con el tótem como reserva si el identificador no existe. */
    private Item targetItem() {
        String id = config.itemId();
        if (id.equals(cachedItemId)) {
            return cachedItem;
        }
        Identifier identifier = Identifier.tryParse(id);
        Item item = identifier == null ? null : Registries.ITEM.get(identifier);
        if (item == null || item == Items.AIR) {
            if (!warnedAboutItem || !id.equals(cachedItemId)) {
                NaturalTotem.LOGGER.warn("El objeto \"{}\" no existe; se usa minecraft:totem_of_undying.", id);
                warnedAboutItem = true;
            }
            item = Items.TOTEM_OF_UNDYING;
        } else {
            warnedAboutItem = false;
        }
        cachedItemId = id;
        cachedItem = item;
        return item;
    }

    private void announce(MinecraftClient client, int swapButton) {
        if (!config.announce()) {
            return;
        }
        String place = swapButton == RefillPlanner.OFFHAND_SLOT
                ? "la mano secundaria"
                : "la casilla " + (swapButton + 1);
        ChatOutput.info(client, "Rellenada " + place + ".");
    }

    private static boolean isMouseHeld(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    /** Olvida la pantalla actual; se usa al cerrar el inventario o cambiar de mundo. */
    public void forget() {
        active = null;
        resetTransaction();
    }

    private void resetTransaction() {
        motionSlot = null;
        destination = null;
        source = null;
    }

    private void cancelPass(String reason) {
        if (config.debug()) {
            NaturalTotem.LOGGER.info("Pasada cancelada: {}", reason);
        }
        resetTransaction();
        passDone = true;
    }

    /** Vuelve a permitir una pasada en el inventario que ya está abierto. */
    public void restartPass() {
        passDone = false;
        selectionDone = false;
        resetTransaction();
        pacer.clear();
    }
}
