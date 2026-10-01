package fr.poubone.att2.client.renderer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public enum ItemRarity {
    COM("com", 10265519, false, false, false, 0.9F),
    UNC("unc", 4906624, false, false, false, 1.0F),
    RAR("rar", 6333946, true, false, false, 1.08F),
    EPI("epi", 12616956, true, false, false, 1.16F),
    EPI_ESC("epi_esc", 15235577, true, false, false, 1.2F),
    EPI_SET("epi_set", 16486972, true, false, false, 1.22F),
    LEG("leg", 16498468, true, true, false, 1.32F),
    LEG_ARMSET("leg_armset", 16096779, true, true, false, 1.34F),
    ULT("ult", 5635925, true, true, false, 1.45F),
    MYT("myt", 16118783, true, true, true, 1.7F),
    SPE("spe", 16020150, true, false, false, 1.18F),
    QUE("que", 2282478, false, false, false, 1.0F),
    MISC("misc", 13751771, false, false, false, 0.9F),
    UNK("unk", 15067115, false, false, false, 0.95F),
    CUR("cur", 15680580, false, false, false, 0.95F);

    public final String id;
    public final int rgb;
    public final boolean shimmer;
    public final boolean helix;
    public final boolean sparks;
    public final float pitch;

    ItemRarity(String id, int rgb, boolean shimmer, boolean helix, boolean sparks, float pitch) {
        this.id = id;
        this.rgb = rgb;
        this.shimmer = shimmer;
        this.helix = helix;
        this.sparks = sparks;
        this.pitch = pitch;
    }

    public static ItemRarity fromId(String id) {
        if (id != null && !id.isEmpty()) {
            for (ItemRarity rarity : values()) {
                if (rarity.id.equals(id)) {
                    return rarity;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static ItemRarity fromStack(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && !data.isEmpty() ? data.copyTag().getString("Rarity").map(ItemRarity::fromId).orElse(null) : null;
        } else {
            return null;
        }
    }

    public int argb(int alpha) {
        return alpha << 24 | this.rgb;
    }
}
