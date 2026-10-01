package fr.poubone.att2.client.shop;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.lwjgl.glfw.GLFW;

final class ShopComparison {
    private ShopComparison() {
    }

    static EquipmentSlot slot(ShopOffer offer) {
        return slot(offer.stack(), offer.equipmentType(), offer.translationKey());
    }

    static EquipmentSlot slot(ItemStack stack) {
        return slot(stack, "", "");
    }

    static EquipmentSlot slot(ItemStack stack, String equipmentType, String translationKey) {
        if (stack != null && !stack.isEmpty()) {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null) {
                EquipmentSlot worn = armorEquipment(equippable.slot());
                if (worn != null) {
                    return worn;
                }
            }
        }

        return equipmentSlot(armorSlot(equipmentType, translationKey, itemPath(stack)));
    }

    static String armorSlot(String equipmentType, String translationKey, String itemId) {
        String text = normalize(safe(equipmentType) + " " + safe(translationKey) + " " + safe(itemId));
        if (containsAny(text, "helmet", "casque")) {
            return "HEAD";
        } else if (containsAny(text, "chestplate", "plastron")) {
            return "CHEST";
        } else if (containsAny(text, "leggings", "jambiere")) {
            return "LEGS";
        } else {
            return containsAny(text, "boots", "bottes") ? "FEET" : null;
        }
    }

    static boolean shouldShowHint(String armorSlot, boolean held) {
        return armorSlot != null && !held;
    }

    static boolean holdsCompare(boolean boundKeyDown, int boundGlfwKey, boolean leftShiftDown, boolean rightShiftDown) {
        if (boundKeyDown) {
            return true;
        } else {
            return boundGlfwKey != 340 && boundGlfwKey != 344 ? false : leftShiftDown || rightShiftDown;
        }
    }

    static boolean isHeld(long window, int boundKey) {
        boolean boundDown = boundKey != -1 && GLFW.glfwGetKey(window, boundKey) == 1;
        boolean left = GLFW.glfwGetKey(window, 340) == 1;
        boolean right = GLFW.glfwGetKey(window, 344) == 1;
        return holdsCompare(boundDown, boundKey, left, right);
    }

    static List<Component> wornLines(List<Component> equippedTooltip) {
        List<Component> worn = new ArrayList<>();
        worn.add(Component.translatable("att2.ui.equipped").withStyle(ChatFormatting.GOLD));
        if (equippedTooltip != null && !equippedTooltip.isEmpty()) {
            worn.addAll(equippedTooltip);
        } else {
            worn.add(Component.translatable("att2.ui.unequipped"));
        }

        return worn;
    }

    static void render(GuiGraphics g, List<Component> offer, ItemStack equipped, int mouseX, int mouseY, int width, int height) {
        Font font = Minecraft.getInstance().font;
        List<Component> worn = wornLines(equipped != null && !equipped.isEmpty() ? ShopTooltips.of(equipped) : List.of());
        int column = Math.max(80, Math.min(240, (width - 40) / 2));
        List<FormattedCharSequence> left = new ArrayList<>();
        List<FormattedCharSequence> right = new ArrayList<>();

        for (Component line : offer) {
            left.addAll(font.split(line, column));
        }

        for (Component line : worn) {
            right.addAll(font.split(line, column));
        }

        int contentHeight = Math.max(left.size(), right.size()) * 10 + 24;
        float scale = Math.min(1.0F, Math.min((height - 16.0F) / contentHeight, (width - 16.0F) / (2 * column + 52)));
        int totalWidth = Math.round((column * 2 + 52) * scale);
        int x = Math.max(8, Math.min(mouseX + 12, width - totalWidth - 8));
        int y = Math.max(8, Math.min(mouseY - 12, height - Math.round(contentHeight * scale) - 8));
        g.pose().pushMatrix();
        g.pose().translate(x + 12.0F * scale, y + 12.0F * scale);
        g.pose().scale(scale, scale);
        draw(g, left, 0, column);
        draw(g, right, column + 28, column);
        g.pose().popMatrix();
    }

    private static void draw(GuiGraphics g, List<FormattedCharSequence> lines, int x, int width) {
        TooltipRenderUtil.renderTooltipBackground(g, x, 0, width, lines.size() * 10, null);

        for (int i = 0; i < lines.size(); i++) {
            g.drawString(Minecraft.getInstance().font, lines.get(i), x, i * 10, -1, true);
        }
    }

    private static EquipmentSlot armorEquipment(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> slot;
            default -> null;
        };
    }

    private static EquipmentSlot equipmentSlot(String name) {
        if (name == null) {
            return null;
        }

        return switch (name) {
            case "HEAD" -> EquipmentSlot.HEAD;
            case "CHEST" -> EquipmentSlot.CHEST;
            case "LEGS" -> EquipmentSlot.LEGS;
            case "FEET" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static String itemPath(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            try {
                return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            } catch (RuntimeException e) {
                return "";
            }
        } else {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        StringBuilder out = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            switch (c) {
                case 'à':
                case 'â':
                case 'ä':
                    out.append('a');
                    break;
                case 'á':
                case 'ã':
                case 'å':
                case 'æ':
                case 'ì':
                case 'í':
                case 'ð':
                case 'ñ':
                case 'ò':
                case 'ó':
                case 'õ':
                case '÷':
                case 'ø':
                case 'ú':
                default:
                    out.append(c);
                    break;
                case 'ç':
                    out.append('c');
                    break;
                case 'è':
                case 'é':
                case 'ê':
                case 'ë':
                    out.append('e');
                    break;
                case 'î':
                case 'ï':
                    out.append('i');
                    break;
                case 'ô':
                case 'ö':
                    out.append('o');
                    break;
                case 'ù':
                case 'û':
                case 'ü':
                    out.append('u');
            }
        }

        return out.toString();
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }

        return false;
    }
}
