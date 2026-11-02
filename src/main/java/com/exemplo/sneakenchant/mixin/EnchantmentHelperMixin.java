package com.exemplo.sneakenchant.mixins;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getLevelFromNbt", at = @At("HEAD"), cancellable = true)
    private static void sneakenchant$removeCap(NbtCompound nbt, CallbackInfoReturnable<Integer> cir) {
        int level = nbt.getInt("lvl");
        cir.setReturnValue(Math.max(0, level));
    }
}
