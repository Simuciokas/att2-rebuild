package fr.poubone.att2.mixin;

import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.data.MapStatDisplayEnabler;
import fr.poubone.att2.client.data.PlayerInfoModel;
import fr.poubone.att2.client.data.StatUpgradeModel;
import fr.poubone.att2.client.quest.QuestModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {
    @Inject(method = "handleShowDialog", at = @At("HEAD"), cancellable = true)
    private void att2$interceptDialog(ClientboundShowDialogPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            if (QuestModel.get().onDialog(packet.dialog())) {
                ci.cancel();
            } else if (StatUpgradeModel.onDialog(packet.dialog())) {
                ci.cancel();
            } else if (MapStatDisplayEnabler.onDialog(packet.dialog())) {
                ci.cancel();
            } else if (PlayerInfoModel.onDialog(packet.dialog())) {
                ci.cancel();
            } else {
                if (CurrencyModel.onDialog(packet.dialog())) {
                    ci.cancel();
                }
            }
        }
    }
}
