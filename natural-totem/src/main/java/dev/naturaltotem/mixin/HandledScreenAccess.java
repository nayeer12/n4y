// Recuperado del JAR 1.4.0 con CFR y nombres Yarn; los comentarios originales no están disponibles.
package dev.naturaltotem.mixin;

import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={HandledScreen.class})
public interface HandledScreenAccess {
    @Accessor(value="x")
    public int naturalTotem$x();

    @Accessor(value="y")
    public int naturalTotem$y();

    @Invoker(value="getSlotAt")
    public Slot naturalTotem$slotAt(double var1, double var3);

    @Invoker(value="onMouseClick")
    public void naturalTotem$click(Slot var1, int var2, int var3, SlotActionType var4);
}

