package fr.poubone.att2.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lootbeams.helpers.ItemHelper;
import fr.poubone.att2.client.renderer.ItemRarity;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

public class HUDConfig {
    public boolean showChronoton = true;
    public boolean showXP = true;
    public boolean showMana = true;
    public boolean showStats = true;
    public boolean showArrows = true;
    public boolean showArmorDurability = true;
    public boolean showSpellBar = true;
    public Map<String, HudSlot> slots = new LinkedHashMap<>();
    public boolean dropLockEnabled = true;
    public List<String> dropLockRarities = new ArrayList<>(List.of("myt", "que"));
    public boolean discordRichPresence = true;
    public boolean discordShowQuest = true;
    public boolean discordRegionImage = true;
    public String discordApplicationId = "1545088290577981521";
    public String discordMapUrl = "https://adventquest.com/across-the-time-ii-time-for-regrets";
    public String discordModUrl = "https://github.com/Poubone/ATT2_Project";
    public String discordLargeImage = "logo-brillant";
    public List<String> discordUploadedAssets = new ArrayList<>(List.of("logo-brillant"));
    private static final String DEFAULT_DISCORD_APP_ID = "1545088290577981521";
    private static final String DEFAULT_DISCORD_MAP_URL = "https://adventquest.com/across-the-time-ii-time-for-regrets";
    private static final String DEFAULT_DISCORD_MOD_URL = "https://github.com/Poubone/ATT2_Project";
    private static final String DEFAULT_DISCORD_LARGE_IMAGE = "logo-brillant";
    public String modLanguage = "fr";
    public String partySyncUrl = "https://sync.guide-att2.com";
    public String partySyncToken = "";
    public List<String> renderRarities = new ArrayList<>(
        List.of("com", "cur", "epi", "epi_set", "leg", "leg_armset", "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc")
    );
    public boolean allItems = true;
    public boolean onlyEquipment = false;
    public boolean onlyRare = false;
    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>();
    public List<String> colorOverrides = new ArrayList<>();
    public boolean renderNameColor = true;
    public boolean renderRarityColor = true;
    public float beamRadius = 1.0F;
    public float beamHeight = 1.0F;
    public float beamYOffset = 0.0F;
    public float beamAlpha = 0.85F;
    public float renderDistance = 24.0F;
    public float fadeDistance = 2.0F;
    public boolean borders = true;
    public boolean renderNametags = true;
    public boolean renderNametagsOnlook = true;
    public boolean renderStackcount = true;
    public float nametagLookSensitivity = 0.018F;
    public float nametagTextAlpha = 1.0F;
    public float nametagBackgroundAlpha = 0.5F;
    public float nametagScale = 1.0F;
    public float nametagYOffset = 0.75F;
    public List<String> alwaysDrawRaritiesOn = List.of("#minecraft:music_discs");
    public boolean whiteRarities = false;
    public boolean questBookShowMain = true;
    public boolean questBookShowSide = true;
    public boolean questBookShowCompleted = true;
    public boolean questBookShowDaily = true;
    public boolean charlesAutoOpen = true;
    public boolean runeMenuEnabled = true;
    public boolean minerMenuEnabled = true;
    public boolean questMenuEnabled = true;
    public boolean shopAutoOpen = true;
    public Map<String, Boolean> shopMenus = null;
    public static final List<String> SHOP_MENU_IDS = List.of("blacksmith", "food", "alchemist", "fletcher", "dahal", "tailor", "fish", "stable", "general");
    public String questBookTheme = "minecraft";
    public String questBookTitleColor = "#FFFF55";
    public String questBookTextColor = "#000000";
    public String questBookSecondaryTextColor = "#404040";
    public String questBookEntryColor = "#B5AE97";
    public String questBookEntryHoverColor = "#797465";
    public String questBookSelectedColor = "#FFCE7F";
    public String questBookSelectedHoverColor = "#FFC432";
    public String questBookFilterColor = "#B5AE97";
    public String questBookFilterHoverColor = "#797465";
    public String questBookFilterEnabledColor = "#A4D48E";
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "att2_hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HUDConfig INSTANCE;

    public static int color(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                parsed |= 4278190080L;
            }

            return (int)parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static HUDConfig get() {
        if (INSTANCE == null) {
            load();
        }

        return INSTANCE;
    }

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE, StandardCharsets.UTF_8)) {
                    INSTANCE = (HUDConfig)GSON.fromJson(reader, HUDConfig.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (INSTANCE == null) {
            INSTANCE = new HUDConfig();
        }

        if (INSTANCE.renderRarities == null) {
            INSTANCE.renderRarities = new ArrayList<>(
                List.of("com", "cur", "epi", "epi_set", "leg", "leg_armset", "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc")
            );
        } else if (!INSTANCE.renderRarities.contains("esc")) {
            INSTANCE.renderRarities.add("esc");
        }

        if (INSTANCE.whitelist == null) {
            INSTANCE.whitelist = new ArrayList<>();
        }

        if (INSTANCE.blacklist == null) {
            INSTANCE.blacklist = new ArrayList<>();
        }

        if (INSTANCE.slots == null) {
            INSTANCE.slots = new LinkedHashMap<>();
        }

        if (INSTANCE.slots != null) {
            for (HudSlot slot : INSTANCE.slots.values()) {
                if (slot != null) {
                    if (slot.scale <= 0.0F) {
                        slot.scale = 1.0F;
                    }

                    if (slot.gap <= 0.0F) {
                        slot.gap = 1.0F;
                    }
                }
            }
        }

        if (INSTANCE.discordUploadedAssets == null) {
            INSTANCE.discordUploadedAssets = new ArrayList<>(List.of("logo-brillant"));
        }

        INSTANCE.modLanguage = ModLanguageManager.normalize(INSTANCE.modLanguage);
        if (INSTANCE.discordApplicationId == null || INSTANCE.discordApplicationId.isBlank()) {
            INSTANCE.discordApplicationId = "1545088290577981521";
        }

        if (INSTANCE.discordMapUrl == null || INSTANCE.discordMapUrl.isBlank()) {
            INSTANCE.discordMapUrl = "https://adventquest.com/across-the-time-ii-time-for-regrets";
        }

        if (INSTANCE.discordModUrl == null || INSTANCE.discordModUrl.isBlank()) {
            INSTANCE.discordModUrl = "https://github.com/Poubone/ATT2_Project";
        }

        if (INSTANCE.discordLargeImage == null || INSTANCE.discordLargeImage.isBlank()) {
            INSTANCE.discordLargeImage = "logo-brillant";
        }

        if (INSTANCE.partySyncUrl == null) {
            INSTANCE.partySyncUrl = "";
        }

        if (INSTANCE.partySyncToken == null) {
            INSTANCE.partySyncToken = "";
        }

        if (INSTANCE.shopMenus == null) {
            INSTANCE.shopMenus = new LinkedHashMap<>();

            for (String id : SHOP_MENU_IDS) {
                INSTANCE.shopMenus.put(id, INSTANCE.shopAutoOpen);
            }
        } else {
            for (String id : SHOP_MENU_IDS) {
                INSTANCE.shopMenus.putIfAbsent(id, true);
            }
        }
    }

    public boolean isShopMenuEnabled(String id) {
        return this.shopMenus != null && id != null ? this.shopMenus.getOrDefault(id, true) : this.shopAutoOpen;
    }

    public void setShopMenuEnabled(String id, boolean enabled) {
        if (this.shopMenus == null) {
            this.shopMenus = new LinkedHashMap<>();
        }

        this.shopMenus.put(id, enabled);
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setModLanguage(String langCode) {
        get().modLanguage = ModLanguageManager.normalize(langCode);
        save();
    }

    public static String getModLanguage() {
        return get().modLanguage;
    }

    public static TextColor getColorFromItemOverrides(Item item) {
        List<String> overrides = get().colorOverrides;
        if (overrides.isEmpty()) {
            return null;
        }

        for (String unparsed : overrides.stream().filter(s -> !s.isEmpty()).toList()) {
            String[] configValue = unparsed.split("=");
            if (configValue.length == 2) {
                String nameIn = configValue[0];
                Optional<TextColor> colorIn = TextColor.parseColor(configValue[1]).result();
                if (colorIn.isEmpty()) {
                    return null;
                }

                if (matchesItem(nameIn, item)) {
                    return colorIn.get();
                }
            }
        }

        return null;
    }

    public static boolean shouldRenderLootBeam(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            HUDConfig config = get();
            Item item = stack.getItem();
            boolean whitelistMatch = matchesAnyList(config.whitelist, item);
            boolean blacklistMatch = matchesAnyList(config.blacklist, item);
            boolean itemMatch = config.allItems
                || config.onlyEquipment && ItemHelper.isEquipmentItem(stack)
                || config.onlyRare && stack.getRarity() != Rarity.COMMON
                || whitelistMatch;
            if (itemMatch && !blacklistMatch) {
                String rarityId = lootRarityId(stack);
                return rarityId != null && config.renderRarities != null && config.renderRarities.contains(rarityId);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static String lootRarityId(ItemStack stack) {
        ItemRarity rarity = ItemRarity.fromStack(stack);
        if (rarity != null) {
            return rarity.id;
        } else {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                String coin = data.copyTag().getString("Coin").orElse("");
                return coin.equals("esc") ? coin : data.copyTag().getString("Rarity").orElse(null);
            } else {
                return null;
            }
        }
    }

    private static boolean matchesAnyList(List<String> entries, Item item) {
        if (entries != null && !entries.isEmpty()) {
            for (String entry : entries) {
                if (matchesItem(entry, item)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean matchesItem(String name, Item item) {
        if (name != null && !name.isEmpty()) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (!name.contains(":")) {
                return itemId.getNamespace().equals(name);
            }

            Identifier id = Identifier.tryParse(name.replace("#", ""));
            if (id == null) {
                return false;
            }

            if (!name.startsWith("#")) {
                return itemId.equals(id);
            }

            Optional<Named<Item>> tag = BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id));
            return tag.isPresent() && tag.get().contains(item.builtInRegistryHolder());
        } else {
            return false;
        }
    }
}
