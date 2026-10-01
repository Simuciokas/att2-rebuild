package fr.poubone.att2.mixin.teleport;

import fr.poubone.att2.client.teleport.TeleportTransitionController;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NauseaSuppressMixin {
    @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
    private void att2$muteNauseaDuringTransition(Holder<MobEffect> effect, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (effect.is(MobEffects.NAUSEA) && TeleportTransitionController.isRunning()) {
            if ((Object)this == Minecraft.getInstance().player) {
                cir.setReturnValue(0.0F);
            }
        }
    }
}
