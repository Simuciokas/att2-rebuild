package fr.poubone.att2.client.data;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.scores.ScoreHolder;

public final class GrimoireXp {
    private static final Pattern XP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private GrimoireXp() {
    }

    public static boolean isGrimoire(ItemStack stack, int spellId) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                CompoundTag tag = data.copyTag();
                return !"book".equals(tag.getString("Dahal").orElse("")) ? false : tag.getIntOr("Spell", 0) == spellId;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static GrimoireXp.XpState read(LocalPlayer player, int spellId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            GrimoireXp.XpState state = parse(player.getInventory().getItem(slot), spellId, player);
            if (state != null) {
                return state;
            }
        }

        return parse(player.getOffhandItem(), spellId, player);
    }

    public static GrimoireXp.XpState parse(ItemStack stack, int spellId) {
        return parse(stack, spellId, null);
    }

    private static GrimoireXp.XpState parse(ItemStack stack, int spellId, LocalPlayer player) {
        if (!isGrimoire(stack, spellId)) {
            return null;
        }

        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            return null;
        }

        for (Filterable<Component> page : content.pages()) {
            GrimoireXp.XpState translated = parseProgress(page.raw());
            if (translated != null) {
                return translated;
            }

            GrimoireXp.XpState state = matchXp(flatten(page.raw(), player));
            if (state != null) {
                return state;
            }

            if (page.filtered().isPresent()) {
                translated = parseProgress(page.filtered().get());
                if (translated != null) {
                    return translated;
                }

                state = matchXp(flatten(page.filtered().get(), player));
                if (state != null) {
                    return state;
                }
            }
        }

        return null;
    }

    private static GrimoireXp.XpState parseProgress(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if ("att2.item.dahal.book.level_progress".equals(translated.getKey()) && translated.getArgs().length >= 2) {
                OptionalInt xp = resolvedInteger(translated.getArgs()[0]);
                OptionalInt threshold = resolvedInteger(translated.getArgs()[1]);
                if (xp.isPresent() && threshold.isPresent() && threshold.getAsInt() > 0) {
                    return new GrimoireXp.XpState(xp.getAsInt(), threshold.getAsInt());
                }
            }

            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    GrimoireXp.XpState found = parseProgress(nested);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }

        for (Component sibling : component.getSiblings()) {
            GrimoireXp.XpState found = parseProgress(sibling);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static OptionalInt resolvedInteger(Object value) {
        if (value instanceof Component component && component.getContents() instanceof ScoreContents) {
            return OptionalInt.empty();
        } else {
            String text = value instanceof Component component ? component.getString() : String.valueOf(value);
            Matcher matcher = Pattern.compile("-?\\d+").matcher(text);
            return matcher.find() ? OptionalInt.of(Integer.parseInt(matcher.group())) : OptionalInt.empty();
        }
    }

    private static GrimoireXp.XpState matchXp(String text) {
        if (text != null && !text.isEmpty()) {
            Matcher matcher = XP_PATTERN.matcher(text);
            if (!matcher.find()) {
                return null;
            }

            try {
                int xp = Integer.parseInt(matcher.group(1));
                int next = Integer.parseInt(matcher.group(2));
                return next <= 0 ? null : new GrimoireXp.XpState(xp, next);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static String flatten(Component component, LocalPlayer player) {
        StringBuilder sb = new StringBuilder();
        flatten(component, player, sb);
        return sb.toString();
    }

    private static void flatten(Component component, LocalPlayer player, StringBuilder sb) {
        ComponentContents contents = component.getContents();
        if (contents instanceof ScoreContents score) {
            OptionalInt value = resolveScore(score, player);
            if (value.isPresent()) {
                sb.append(value.getAsInt());
            }
        } else if (contents instanceof PlainTextContents text) {
            sb.append(text.text());
        }

        for (Component sibling : component.getSiblings()) {
            flatten(sibling, player, sb);
        }
    }

    private static OptionalInt resolveScore(ScoreContents score, LocalPlayer player) {
        ScoreHolder holder = (ScoreHolder)score.name().map(selector -> player, ScoreHolder::forNameOnly);
        return holder == null ? OptionalInt.empty() : ScoreCache.readHolder(score.objective(), holder);
    }

    public record XpState(int xp, int nextThreshold) {
    }
}
