package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.data.ScoreCache;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

public final class RuneStock {
    private static final Pattern LAST_INT = Pattern.compile("(\\d+)(?!.*\\d)");

    private RuneStock() {
    }

    public static RuneStock.Counts counts(LocalPlayer player, RuneCatalog.RuneDef rune) {
        return new RuneStock.Counts(countInventory(player, rune), pouch(player, rune));
    }

    public static int countInventory(LocalPlayer player, RuneCatalog.RuneDef rune) {
        if (player == null) {
            return 0;
        }

        int total = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += countStack(player.getInventory().getItem(i), rune);
        }

        return total;
    }

    public static OptionalInt pouch(LocalPlayer player, RuneCatalog.RuneDef rune) {
        OptionalInt live = ScoreCache.get(rune.scoreObjective());
        return live.isPresent() ? live : loreFallback(player, rune);
    }

    public static OptionalInt powder() {
        return ScoreCache.getHolder("RUNE_POWDER", "#stock");
    }

    public static int esc(LocalPlayer player) {
        if (player == null) {
            return 0;
        }

        int total = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += countEsc(player.getInventory().getItem(i));
        }

        return total;
    }

    public static boolean unlocked(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("RUNE", rune.unlockHolder()).orElse(0) >= 1;
    }

    public static OptionalInt price(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("PRICES", rune.priceHolder());
    }

    public static OptionalInt escPrice(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("PRICES", rune.escPriceHolder());
    }

    public static OptionalInt bonus(String holder) {
        return ScoreCache.getHolder("RUNE", holder);
    }

    static int countStack(ItemStack stack, RuneCatalog.RuneDef rune) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                CompoundTag tag = data.copyTag();
                if (!"rune".equals(tag.getString("EquipmentType").orElse(""))) {
                    return 0;
                } else {
                    return tag.getByteOr("Runelvl", (byte)-1) != (byte)rune.level() ? 0 : stack.getCount();
                }
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    static int countEsc(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                CompoundTag tag = data.copyTag();
                return !"esc".equals(tag.getString("Coin").orElse("")) ? 0 : stack.getCount();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    static boolean isBundle(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && !data.isEmpty() ? "rune_bundle".equals(data.copyTag().getString("EquipmentType").orElse("")) : false;
        } else {
            return false;
        }
    }

    static OptionalInt loreFallback(LocalPlayer player, RuneCatalog.RuneDef rune) {
        if (player == null) {
            return OptionalInt.empty();
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            OptionalInt parsed = parseBundleLore(player.getInventory().getItem(i), rune);
            if (parsed.isPresent()) {
                return parsed;
            }
        }

        return OptionalInt.empty();
    }

    static OptionalInt parseBundleLore(ItemStack stack, RuneCatalog.RuneDef rune) {
        if (!isBundle(stack)) {
            return OptionalInt.empty();
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return OptionalInt.empty();
        }

        String needle = "att2.rune_bundle.total." + rune.id();

        for (Component line : lore.lines()) {
            OptionalInt resolved = translatedValue(line, needle);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return OptionalInt.empty();
    }

    private static OptionalInt translatedValue(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (translated.getKey().equals(key) && translated.getArgs().length > 0) {
                Object arg = translated.getArgs()[0];
                if (!(arg instanceof Component nested && nested.getContents() instanceof ScoreContents)) {
                    String text = arg instanceof Component value ? value.getString() : String.valueOf(arg);
                    Matcher matcher = LAST_INT.matcher(text);
                    if (matcher.find()) {
                        return OptionalInt.of(Integer.parseInt(matcher.group(1)));
                    }
                }
            }

            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    OptionalInt found = translatedValue(nested, key);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }

        for (Component sibling : component.getSiblings()) {
            OptionalInt found = translatedValue(sibling, key);
            if (found.isPresent()) {
                return found;
            }
        }

        return OptionalInt.empty();
    }

    public record Counts(int inventory, OptionalInt pouch) {
    }
}
