package fr.poubone.att2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.poubone.att2.client.data.MapStatBar;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow
    @Final
    Map<UUID, LerpingBossEvent> events;

    @Inject(method = "update", at = @At("RETURN"))
    private void att2$captureStatBar(ClientboundBossEventPacket packet, CallbackInfo ci) {
        MapStatBar.capture(this.events, true);
    }

    @WrapMethod(method = "render")
    private void att2$hideStatBar(GuiGraphics graphics, Operation<Void> original) {
        MapStatBar.capture(this.events, true);
        Map<UUID, LerpingBossEvent> parked = new LinkedHashMap<>();
        if (KeybindManager.showCustomHUD && HUDConfig.get().showStats) {
            this.events.entrySet().removeIf(entry -> {
                if (!MapStatBar.isStatBar(entry.getValue())) {
                    return false;
                }

                parked.put(entry.getKey(), entry.getValue());
                return true;
            });
        }

        try {
            original.call(graphics);
        } finally {
            if (!parked.isEmpty()) {
                this.events.putAll(parked);
            }
        }
    }
}
