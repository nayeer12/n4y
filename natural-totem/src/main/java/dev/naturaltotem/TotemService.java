// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; reorganizado y conectado a la configuración.
package dev.naturaltotem;

import dev.naturaltotem.config.Options;
import dev.naturaltotem.config.TotemConfig;
import dev.naturaltotem.mixin.HandledScreenAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * El rellenado en sí: mira el inventario abierto, pide un plan y lo ejecuta.
 *
 * <p>Conserva lo que hacía el JAR 1.4.0 —intercambio con {@link SlotActionType#SWAP}, nada si
 * llevas un objeto en el cursor, comprobación del resultado— y añade dos cosas. El cursor se
 * acerca con un recorrido curvo, de duración sorteada y sin apuntar siempre al centro exacto.
 * Y, con {@code continuous} encendido, el mod no se detiene al terminar una pasada: sigue
 * mirando mientras el inventario esté abierto, así que un tótem que estalla se repone sin
 * cerrar y volver a abrir la pantalla.
 *
 * <p>Toda la decisión está en {@link RefillPlanner}, toda la espera en {@link ActionPacer} y
 * todo el "en qué punto vamos" en {@link PassState}; esta clase solo traduce entre esas tres
 * y los objetos del juego.
 */
public final class TotemService {

    /** Casillas del contenedor del jugador que nos interesan: el inventario y la mano secundaria. */
    private static final int TRACKED_SLOTS = RefillPlanner.OFFHAND_SLOT + 1;

    private final TotemConfig config;
    private final ActionPacer pacer;
    private final PassState pass = new PassState();
    private final RandomGenerator random = new SplittableRandom();

    /** Pantalla a la que corresponde el estado actual; sirve para detectar una apertura nueva. */
    private InventoryScreen active;
    private Slot source;
    private Slot destination;

    /** Casilla hacia la que se está moviendo el cursor, y cómo es ese movimiento. */
    private Slot motionSlot;
    private long motionStarted;
    private long motionDuration;
    private double startX;
    private double startY;
    private double aimOffsetX;
    private double aimOffsetY;
    private double bend;

    private String cachedItemId;
    private Item cachedItem = Items.TOTEM_OF_UNDYING;
    private boolean warnedAboutItem;

    public TotemService(TotemConfig config) {
        this.config = config;
        this.pacer = new ActionPacer(config.nextActionIntervalNanos(random));
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
            pass.beginOpening();
            resetTransaction();
        }
        if (!config.enabled() || config.nothingToFill()) {
            return;
        }
        if (config.requireFocus() && !client.isWindowFocused()) {
            hold("la ventana perdió el foco");
            return;
        }
        long now = System.nanoTime();
        if (!pass.canAct(now)) {
            return;
        }

        PlayerScreenHandler handler = inventory.getScreenHandler();
        HandledScreenAccess access = (HandledScreenAccess) inventory;
        Item target = targetItem();

        if (source == null && !plan(client, handler, target)) {
            return;
        }
        if (!handler.getCursorStack().isEmpty()) {
            hold("llevas un objeto en el cursor");
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
            hold("llevas un objeto en el cursor");
            return false;
        }
        PlayerInventory inventory = client.player.getInventory();
        if (inventory.size() < RefillPlanner.MAIN_SLOTS) {
            fail("el inventario no tiene el tamaño esperado");
            return false;
        }

        // Una sola pasada por las casillas de la pantalla: nos deja el objeto Slot de cada
        // índice, y con él su posición, que es lo que necesita el planificador por cercanía.
        Slot[] slots = new Slot[TRACKED_SLOTS];
        for (Slot slot : handler.slots) {
            int index = slot.getIndex();
            if (slot.inventory == inventory && index >= 0 && index < TRACKED_SLOTS) {
                slots[index] = slot;
            }
        }

        boolean[] present = new boolean[RefillPlanner.MAIN_SLOTS];
        for (int index = 0; index < present.length; index++) {
            present[index] = inventory.getStack(index).isOf(target);
        }
        RefillPlanner.Move move = RefillPlanner.next(
                present,
                client.player.getOffHandStack().isOf(target),
                config.plan(),
                geometryOf(slots));
        if (move == null) {
            finishPass(client, present);
            return false;
        }
        source = slots[move.source()];
        destination = slots[move.destination()];
        if (source == null || destination == null || !source.canTakeItems(client.player)) {
            fail("no se encontraron las casillas del plan");
            return false;
        }
        return true;
    }

    /** Posiciones reales de las casillas en la pantalla; las que falten se dejan a la colocación estándar. */
    private static RefillPlanner.Geometry geometryOf(Slot[] slots) {
        return new RefillPlanner.Geometry() {
            @Override
            public int x(int slot) {
                Slot found = slot >= 0 && slot < slots.length ? slots[slot] : null;
                return found != null ? found.x : RefillPlanner.VANILLA_GEOMETRY.x(slot);
            }

            @Override
            public int y(int slot) {
                Slot found = slot >= 0 && slot < slots.length ? slots[slot] : null;
                return found != null ? found.y : RefillPlanner.VANILLA_GEOMETRY.y(slot);
            }
        };
    }

    /**
     * No queda nada que rellenar: selecciona la casilla si procede. Con {@code continuous} el
     * mod se queda mirando por si el tótem estalla; sin él, la pasada termina aquí.
     */
    private void finishPass(MinecraftClient client, boolean[] present) {
        int slot = config.hotbarSlotIndex();
        boolean wantsSelection = config.get(Options.SELECT_SLOT)
                && config.get(Options.FILL_HOTBAR)
                && present[slot]
                && !pass.selectionDone()
                && client.player.getInventory().getSelectedSlot() != slot;
        if (wantsSelection) {
            if (!pacer.ready(System.nanoTime())) {
                return;
            }
            client.player.getInventory().setSelectedSlot(slot);
            acted();
            pass.markSelectionDone();
        }
        resetTransaction();
        pass.finishedPass(config.continuous());
    }

    /** Ejecuta el intercambio y comprueba que el resultado es el esperado. */
    private void swap(MinecraftClient client, PlayerScreenHandler handler, HandledScreenAccess access, Item target) {
        if (!source.getStack().isOf(target)
                || destination.getStack().isOf(target)
                || !source.canTakeItems(client.player)
                || !destination.canTakeItems(client.player)
                || !destination.canInsert(source.getStack())
                || (destination.hasStack() && !source.canInsert(destination.getStack()))) {
            // El inventario ha cambiado desde que se hizo el plan: se rehace, no es un fallo.
            hold("las casillas cambiaron antes del intercambio");
            return;
        }
        int swapButton = destination.getIndex();
        if (swapButton != RefillPlanner.OFFHAND_SLOT && (swapButton < 0 || swapButton > 8)) {
            fail("destino inesperado: " + swapButton);
            return;
        }
        ItemStack displaced = destination.getStack().copy();
        access.naturalTotem$click(source, source.id, swapButton, SlotActionType.SWAP);
        acted();
        boolean asExpected = destination.getStack().isOf(target)
                && ItemStack.areEqual(source.getStack(), displaced)
                && handler.getCursorStack().isEmpty();
        if (!asExpected) {
            fail("el intercambio no dejó el inventario como se esperaba");
            return;
        }
        announce(client, swapButton);
        pass.succeeded();
        resetTransaction();
    }

    /**
     * Acerca el cursor a la casilla.
     *
     * @return {@code true} cuando ya ha pasado el tiempo de acercamiento
     */
    private boolean approach(MinecraftClient client, Slot slot) {
        if (motionSlot != slot) {
            beginMotion(client, slot);
        }
        if (motionDuration <= 0L) {
            return true;
        }
        animate(client);
        return System.nanoTime() - motionStarted >= motionDuration;
    }

    /** Sortea cómo será este acercamiento: desde dónde, hacia qué punto, cuánto dura y hacia qué lado se curva. */
    private void beginMotion(MinecraftClient client, Slot slot) {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(client.getWindow().getHandle(), x, y);
        startX = x[0];
        startY = y[0];
        motionStarted = System.nanoTime();
        motionDuration = config.nextMoveNanos(random);
        motionSlot = slot;
        aimOffsetX = CursorGlide.aimOffset(config.aimJitter(), random);
        aimOffsetY = CursorGlide.aimOffset(config.aimJitter(), random);
        bend = CursorGlide.bend(config.curvature(), distanceTo(client, slot), random);
    }

    private double distanceTo(MinecraftClient client, Slot slot) {
        if (active == null) {
            return 0.0;
        }
        return Math.hypot(targetX(client, slot) - startX, targetY(client, slot) - startY);
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
                || !config.moveCursor()
                || motionDuration <= 0L) {
            return;
        }
        double eased = CursorGlide.ease(CursorGlide.progress(System.nanoTime() - motionStarted, motionDuration));
        CursorGlide.Curve curve = new CursorGlide.Curve(
                startX, startY, targetX(client, slot), targetY(client, slot), bend);
        GLFW.glfwSetCursorPos(client.getWindow().getHandle(), curve.x(eased), curve.y(eased));
    }

    /** Punto de llegada en píxeles de la ventana: el centro de la casilla con la desviación sorteada. */
    private double targetX(MinecraftClient client, Slot slot) {
        HandledScreenAccess access = (HandledScreenAccess) active;
        return CursorGlide.toWindowPixels(access.naturalTotem$x() + slot.x + 8.0 + aimOffsetX,
                client.getWindow().getWidth(), client.getWindow().getScaledWidth());
    }

    private double targetY(MinecraftClient client, Slot slot) {
        HandledScreenAccess access = (HandledScreenAccess) active;
        return CursorGlide.toWindowPixels(access.naturalTotem$y() + slot.y + 8.0 + aimOffsetY,
                client.getWindow().getHeight(), client.getWindow().getScaledHeight());
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

    /** Anota la acción y sortea la espera hasta la siguiente. */
    private void acted() {
        long now = System.nanoTime();
        pacer.defer(now);
        pacer.setInterval(config.nextActionIntervalNanos(random));
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
        motionDuration = 0L;
        destination = null;
        source = null;
    }

    /** Contratiempo pasajero: se deja el intento y se vuelve a mirar dentro de un momento. */
    private void hold(String reason) {
        log(reason);
        resetTransaction();
        pass.pause(config.continuous(), config.retryNanos(), System.nanoTime());
    }

    /** Fallo de verdad: se cuenta, y varios seguidos paran el mod hasta la siguiente apertura. */
    private void fail(String reason) {
        log(reason);
        resetTransaction();
        if (pass.fail(config.continuous(), config.retryNanos(), System.nanoTime()) && config.debug()) {
            NaturalTotem.LOGGER.info("Parado hasta que se vuelva a abrir el inventario.");
        }
    }

    private void log(String reason) {
        if (config.debug()) {
            NaturalTotem.LOGGER.info("Intento cancelado: {}", reason);
        }
    }

    /** Vuelve a permitir intentos en el inventario que ya está abierto. */
    public void restartPass() {
        pass.restart();
        resetTransaction();
        pacer.clear();
    }
}
