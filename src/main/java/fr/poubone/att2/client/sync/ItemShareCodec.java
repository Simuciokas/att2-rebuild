package fr.poubone.att2.client.sync;

import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ItemShareCodec {
    public static final int MAX_SNBT_CHARS = 20000;

    private ItemShareCodec() {
    }

    public static Optional<String> encode(ItemStack stack, Provider registries) {
        if (stack != null && !stack.isEmpty() && registries != null) {
            try {
                Tag tag = (Tag)ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).result().orElse(null);
                if (tag == null) {
                    return Optional.empty();
                }

                String snbt = tag.toString();
                return snbt.length() > 20000 ? Optional.empty() : Optional.of(snbt);
            } catch (RuntimeException ignored) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static ItemStack decode(String snbt, Provider registries) {
        if (snbt != null && !snbt.isBlank() && snbt.length() <= 20000 && registries != null) {
            try {
                Tag tag = TagParser.parseCompoundFully(snbt);
                return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
            } catch (Exception ignored) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    public static String itemId(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString();
    }

    public static String displayName(ItemStack stack) {
        return stack.getHoverName().getString();
    }
}
