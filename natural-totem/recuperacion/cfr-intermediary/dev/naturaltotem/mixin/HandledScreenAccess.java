/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_465
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package dev.naturaltotem.mixin;

import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_465;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_465.class})
public interface HandledScreenAccess {
    @Accessor(value="field_2776")
    public int naturalTotem$x();

    @Accessor(value="field_2800")
    public int naturalTotem$y();

    @Invoker(value="method_64240")
    public class_1735 naturalTotem$slotAt(double var1, double var3);

    @Invoker(value="method_2383")
    public void naturalTotem$click(class_1735 var1, int var2, int var3, class_1713 var4);
}

