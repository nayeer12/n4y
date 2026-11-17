/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.naturaltotem.mixin;

import dev.naturaltotem.NaturalTotem;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_310.class})
public abstract class ClientMixin {
    @Inject(method={"method_1574"}, at={@At(value="TAIL")})
    private void naturalTotem$tick(CallbackInfo ci) {
        NaturalTotem.tick((class_310)this);
    }

    @Inject(method={"method_1523"}, at={@At(value="HEAD")})
    private void naturalTotem$render(boolean tick, CallbackInfo ci) {
        NaturalTotem.animate((class_310)this);
    }
}

