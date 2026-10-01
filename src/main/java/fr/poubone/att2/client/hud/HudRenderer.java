package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.StatManager;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.screen.HudLayoutScreen;
import fr.poubone.att2.client.shop.ShopPanelScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HudRenderer {
    public static void render(GuiGraphics context, DeltaTracker tickCounter) {
        CityToast.render(context);
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof ShopPanelScreen)) {
            ModToast.render(context);
        }

        if (KeybindManager.showCustomHUD) {
            if (client.player != null && client.level != null) {
                if (!(client.screen instanceof HudLayoutScreen)) {
                    renderWidgets(context, false);
                    HudFx.render(context);
                }
            }
        }
    }

    public static void renderWidgets(GuiGraphics context, boolean all) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            StatManager.updateAll();
            HUDConfig config = HUDConfig.get();
            if (all || config.showChronoton) {
                ChronotonDisplay.render(context);
            }

            if (all || config.showXP) {
                XPDisplay.render(context);
            }

            if (all || config.showMana) {
                ManaOrbDisplay.render(context);
            }

            if (all || config.showStats) {
                StatIconsDisplay.render(context);
            }

            if (all || config.showArrows) {
                ArrowDisplay.render(context);
            }

            if (all || config.showArmorDurability) {
                ArmorDurabilityDisplay.render(context);
            }

            if (all || config.showSpellBar) {
                SpellBarDisplay.render(context);
            }
        }
    }
}
