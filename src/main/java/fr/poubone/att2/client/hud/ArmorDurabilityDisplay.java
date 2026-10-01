package fr.poubone.att2.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorDurabilityDisplay {
    public static void render(GuiGraphics ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.options != null) {
            HudLayout.Box box = HudLayout.box("armor");
            float boxScale = HudLayout.contentScale("armor", 100, 28);
            float iconYBase = box.y();
            int startX = box.x();
            ItemStack[] stacks = new ItemStack[]{
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
            };

            for (int i = 0; i < stacks.length; i++) {
                ItemStack stack = stacks[i];
                if (!stack.isEmpty() && stack.isDamageableItem()) {
                    int max = stack.getMaxDamage();
                    int current = max - stack.getDamageValue();
                    float ratio = (float)current / max;
                    int color = getColor(ratio);
                    int spacing = Math.max(16, Math.round(24.0F * boxScale));
                    float scale = 0.9F * boxScale;
                    float iconX = startX + i * spacing;
                    float iconY = iconYBase;
                    ctx.pose().pushMatrix();
                    ctx.pose().translate(iconX + 8.0F * (1.0F - scale), iconY + 8.0F * (1.0F - scale));
                    ctx.pose().scale(scale, scale);
                    ctx.renderItem(stack, 0, 0);
                    ctx.pose().popMatrix();
                    String text = String.valueOf(current);
                    int textWidth = mc.font.width(text);
                    float textX = iconX + 8.0F - textWidth * scale / 2.0F;
                    float textY = iconY + 16.0F * scale + 2.0F;
                    ctx.pose().pushMatrix();
                    ctx.pose().translate(textX, textY);
                    ctx.pose().scale(scale, scale);
                    ctx.drawString(mc.font, Component.literal(text), 0, 0, color, true);
                    ctx.pose().popMatrix();
                }
            }
        }
    }

    private static int getColor(float ratio) {
        if (ratio > 0.7F) {
            return -11141291;
        } else {
            return ratio > 0.3F ? -256 : -43691;
        }
    }
}
