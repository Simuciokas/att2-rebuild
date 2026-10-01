package fr.poubone.att2.client.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record ShopOffer(ItemStack stack, Component name, Component price, int trigger, String category, String translationKey, Identifier sprite, Component lore) {
    public String equipmentType() {
        CustomData data = this.stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty() ? data.copyTag().getString("EquipmentType").orElse("") : "";
    }

    public String rarityId() {
        CustomData data = this.stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty() ? data.copyTag().getString("Rarity").orElse("") : "";
    }
}
