package fr.poubone.att2.client.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.item.component.CustomData;

public final class ShopTooltips {
    private ShopTooltips() {
    }

    public static List<Component> of(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (stack != null && !stack.isEmpty() && client.level != null) {
            TooltipFlag flag = client.options.advancedItemTooltips ? Default.ADVANCED : Default.NORMAL;

            try {
                return new ArrayList<>(stack.getTooltipLines(TooltipContext.of(client.level), client.player, flag));
            } catch (RuntimeException e) {
                return List.of(stack.getHoverName());
            }
        } else {
            return List.of();
        }
    }

    public static List<Component> forOffer(ShopOffer offer, Component displayName) {
        ItemStack stack = offer.stack();
        return merge(of(stack), isMapItem(stack), offer.lore(), offer.price(), displayName);
    }

    static List<Component> merge(List<Component> itemTooltip, boolean mapItem, Component lore, Component price, Component displayName) {
        List<Component> lines = new ArrayList<>();
        boolean itemHasStats = itemTooltip != null && itemTooltip.size() > 1;
        if (!mapItem && !itemHasStats) {
            if (displayName != null && !displayName.getString().isBlank()) {
                lines.add(displayName);
            }

            if (usefulLore(lore)) {
                lines.addAll(flattenLore(lore));
            } else if (itemTooltip != null && !itemTooltip.isEmpty()) {
                lines.clear();
                lines.addAll(itemTooltip);
            }
        } else {
            if (itemTooltip != null) {
                lines.addAll(itemTooltip);
            }

            if (lines.isEmpty() && displayName != null) {
                lines.add(displayName);
            }
        }

        if (price != null && !price.getString().isBlank() && lines.stream().noneMatch(c -> c.getString().equals(price.getString()))) {
            lines.add(price);
        }

        return lines;
    }

    static boolean isMapItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        } else if (stack.get(DataComponents.LORE) != null) {
            return true;
        } else {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                CompoundTag tag = data.copyTag();
                return tag.getString("EquipmentType").isPresent() || tag.getString("Rarity").isPresent() || tag.getString("Coin").isPresent();
            } else {
                return false;
            }
        }
    }

    static boolean usefulLore(Component lore) {
        if (lore == null) {
            return false;
        } else {
            String text = lore.getString().strip();
            if (text.isEmpty()) {
                return false;
            } else {
                String key = ShopModel.translationKey(lore).toLowerCase(Locale.ROOT);
                if (!key.contains("hover_event.buy") && !key.contains("chronotons.buy") && !key.endsWith(".buy")) {
                    String lower = text.toLowerCase(Locale.ROOT);
                    return !lower.contains("click") || !lower.contains("buy") && !lower.contains("acheter");
                } else {
                    return false;
                }
            }
        }
    }

    static List<Component> flattenLore(Component lore) {
        List<Component> lines = new ArrayList<>();
        if (lore == null) {
            return lines;
        }

        String raw = lore.getString();
        if (raw.contains("\n")) {
            for (String part : raw.split("\n", -1)) {
                if (!part.isBlank()) {
                    lines.add(Component.literal(part));
                }
            }

            return lines;
        } else {
            lines.add(lore);
            return lines;
        }
    }
}
