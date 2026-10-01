package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.discord.DiscordPresence;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HUDConfigScreen extends Screen {
    private static final List<String> RARITY_KEYS = List.of(
        "com", "cur", "epi", "epi_set", "leg", "leg_armset", "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc"
    );
    private HUDConfigScreen.Category category = HUDConfigScreen.Category.HUD;
    private int langIndex;

    public HUDConfigScreen() {
        super(ModLanguageManager.get("screen.hud_config.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        HUDConfig config = HUDConfig.get();
        this.langIndex = Math.max(0, ModLanguageManager.CODES.indexOf(config.modLanguage));
        boolean rtl = ModLanguageManager.isRightToLeft();
        int tabW = 112;
        int tabX = rtl ? this.width - tabW - 12 : 12;
        int tabY = 36;

        for (HUDConfigScreen.Category cat : HUDConfigScreen.Category.values()) {
            HUDConfigScreen.Category picked = cat;
            Component label = ModLanguageManager.get("screen.hud_config.cat." + cat.name().toLowerCase());
            if (cat == this.category) {
                label = Component.literal("▸ ").append(label);
            }

            this.addRenderableWidget(Button.builder(label, b -> {
                this.category = picked;
                this.init();
            }).bounds(tabX, tabY, tabW, 20).build());
            tabY += 24;
        }

        int contentX = rtl ? 16 : 140;
        int contentY = 40;
        int contentRight = rtl ? this.width - 140 : this.width;
        int colW = Math.min(360, Math.max(220, contentRight - contentX - 24));
        switch (this.category) {
            case HUD:
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_chronoton", config.showChronoton, v -> config.showChronoton = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_xp", config.showXP, v -> config.showXP = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_mana", config.showMana, v -> config.showMana = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_stats", config.showStats, v -> config.showStats = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_arrows", config.showArrows, v -> config.showArrows = v);
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.show_armor_durability", config.showArmorDurability, v -> config.showArmorDurability = v
                );
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.show_spell_bar", config.showSpellBar, v -> config.showSpellBar = v);
                this.addRenderableWidget(
                    Button.builder(ModLanguageManager.get("screen.hud_config.arrange"), b -> Minecraft.getInstance().setScreen(new HudLayoutScreen(this)))
                        .bounds(contentX, contentY + 8, 200, 20)
                        .build()
                );
                break;
            case MENUS:
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.quest_menu", config.questMenuEnabled, v -> config.questMenuEnabled = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.rune_menu", config.runeMenuEnabled, v -> config.runeMenuEnabled = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.miner_menu", config.minerMenuEnabled, v -> config.minerMenuEnabled = v);
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.charles_auto_open", config.charlesAutoOpen, v -> config.charlesAutoOpen = v
                );
                int shopX = contentX;
                int shopY = contentY;
                int shopIndex = 0;

                for (String shopId : HUDConfig.SHOP_MENU_IDS) {
                    String id = shopId;
                    this.addToggle(shopX, shopY, 170, "shop.type." + id, config.isShopMenuEnabled(id), selected -> config.setShopMenuEnabled(id, selected));
                    if (++shopIndex % 2 == 0) {
                        shopX = contentX;
                        shopY += 22;
                    } else {
                        shopX += 180;
                    }
                }
                break;
            case QUESTS:
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.quest_show_main", config.questBookShowMain, v -> config.questBookShowMain = v
                );
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.quest_show_side", config.questBookShowSide, v -> config.questBookShowSide = v
                );
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.quest_show_completed", config.questBookShowCompleted, v -> config.questBookShowCompleted = v
                );
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.quest_show_daily", config.questBookShowDaily, v -> config.questBookShowDaily = v
                );
                break;
            case LOOT:
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.render_allItems", config.allItems, v -> config.allItems = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.render_nametags", config.renderNametags, v -> config.renderNametags = v);
                contentY = this.addToggle(
                    contentX, contentY, colW, "screen.hud_config.render_stackcount", config.renderStackcount, v -> config.renderStackcount = v
                );
                contentY += 6;
                int rx = contentX;
                int ry = contentY;
                int i = 0;

                for (String rarity : RARITY_KEYS) {
                    boolean on = config.renderRarities.contains(rarity);
                    this.addToggle(rx, ry, 150, "screen.hud_config.render_rarity." + rarity, on, selected -> {
                        if (selected) {
                            if (!config.renderRarities.contains(rarity)) {
                                config.renderRarities.add(rarity);
                            }
                        } else {
                            config.renderRarities.remove(rarity);
                        }
                    });
                    if (++i % 3 == 0) {
                        rx = contentX;
                        ry += 22;
                    } else {
                        rx += 160;
                    }
                }
                break;
            case GENERAL:
                this.addRenderableWidget(
                    Button.builder(
                            ModLanguageManager.get("screen.hud_config.language_button")
                                .copy()
                                .append(Component.literal(" : " + ModLanguageManager.nativeName(config.modLanguage))),
                            b -> this.cycleLanguage()
                        )
                        .bounds(contentX, contentY, 200, 20)
                        .build()
                );
                contentY += 28;
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.drop_lock", config.dropLockEnabled, v -> config.dropLockEnabled = v);
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.discord_rpc", config.discordRichPresence, v -> {
                    config.discordRichPresence = v;
                    DiscordPresence.onConfigChanged();
                });
                contentY = this.addToggle(contentX, contentY, colW, "screen.hud_config.discord_show_quest", config.discordShowQuest, v -> {
                    config.discordShowQuest = v;
                    DiscordPresence.onConfigChanged();
                });
                this.addToggle(contentX, contentY, colW, "screen.hud_config.party_sync", config.partySyncEnabled, v -> config.partySyncEnabled = v);
        }

        this.addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_config.save_and_close"), b -> {
            HUDConfig.save();
            DiscordPresence.onConfigChanged();
            Minecraft.getInstance().setScreen(null);
        }).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private int addToggle(int x, int y, int width, String key, boolean selected, Consumer<Boolean> onChange) {
        Checkbox box = Checkbox.builder(ModLanguageManager.get(key), this.font)
            .pos(x, y)
            .selected(selected)
            .onValueChange((checkbox, value) -> onChange.accept(value))
            .build();
        this.addRenderableWidget(box);
        return y + 22;
    }

    private void cycleLanguage() {
        this.langIndex = (this.langIndex + 1) % ModLanguageManager.CODES.size();
        String newLang = ModLanguageManager.CODES.get(this.langIndex);
        HUDConfig.setModLanguage(newLang);
        ModLanguageManager.loadLanguage(Minecraft.getInstance(), newLang);
        this.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        boolean rtl = ModLanguageManager.isRightToLeft();
        if (rtl) {
            context.fill(this.width - 128, 0, this.width, this.height, -871100146);
            context.fill(0, 0, this.width - 128, this.height, -1726935540);
        } else {
            context.fill(0, 0, 128, this.height, -871100146);
            context.fill(128, 0, this.width, this.height, -1726935540);
        }

        String title = ModLanguageManager.getString("screen.hud_config.title");
        String cat = ModLanguageManager.getString("screen.hud_config.cat." + this.category.name().toLowerCase());
        if (rtl) {
            context.drawString(this.font, title, this.width - 12 - this.font.width(title), 12, -1521558, false);
            context.drawString(this.font, cat, 16, 14, -3622752, false);
            int markY = 36 + this.category.ordinal() * 24;
            context.fill(this.width - 11, markY, this.width - 8, markY + 20, -1521558);
        } else {
            context.drawString(this.font, title, 12, 12, -1521558, false);
            context.drawString(this.font, cat, 140, 14, -3622752, false);
            int markY = 36 + this.category.ordinal() * 24;
            context.fill(8, markY, 11, markY + 20, -1521558);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private enum Category {
        HUD,
        MENUS,
        QUESTS,
        LOOT,
        GENERAL;
    }
}
