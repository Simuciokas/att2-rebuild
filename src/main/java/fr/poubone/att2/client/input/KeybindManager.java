package fr.poubone.att2.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellSelectTriggers;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookScreen;
import fr.poubone.att2.client.screen.HUDConfigScreen;
import fr.poubone.att2.client.screen.RepairMenuScreen;
import fr.poubone.att2.client.screen.SpellLevelRadialScreen;
import fr.poubone.att2.client.screen.StatUpgradeScreen;
import fr.poubone.att2.client.sync.PartySync;
import fr.poubone.att2.client.util.BroadcastScanner;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;

public class KeybindManager {
    public static boolean showCustomHUD = true;
    private static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("att2", "att2"));
    private static KeyMapping toggleHUDKey;
    private static KeyMapping statUpgradeMenuKey;
    private static KeyMapping playerGlow;
    private static KeyMapping collectItems;
    private static KeyMapping questBook;
    private static KeyMapping repairItemMenuKey;
    private static boolean repairReleased = true;
    private static KeyMapping whistle;
    private static KeyMapping openHUDConfig;
    private static KeyMapping broadcastKey;
    private static KeyMapping pingKey;
    private static KeyMapping compareShopKey;
    private static KeyMapping weaponSkillKey;
    private static KeyMapping spellLevelMenuKey;
    private static boolean spellLevelReleased = true;
    private static boolean languageLoaded = false;
    private static long lastUsedTime = 0L;
    private static final long COOLDOWN_MS = 5000L;
    private static long lastPingTime = 0L;
    private static final long PING_COOLDOWN_MS = 250L;

    private static KeyMapping key(String name, int glfwKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.att2." + name, Type.KEYSYM, glfwKey, CATEGORY));
    }

    public static void register() {
        toggleHUDKey = key("toggle_hud", 72);
        statUpgradeMenuKey = key("stat_upgrade_menu", 85);
        playerGlow = key("player_glow", 71);
        collectItems = key("collect_items", 67);
        questBook = key("quest_main", 66);
        repairItemMenuKey = key("repair_menu", 73);
        whistle = key("whistle", 87);
        openHUDConfig = key("hud_config", 75);
        broadcastKey = key("broadcast", 79);
        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.att2.ping", Type.MOUSE, 4, CATEGORY));
        compareShopKey = key("compare_shop", 340);
        weaponSkillKey = key("weapon_skill", InputConstants.UNKNOWN.getValue());
        spellLevelMenuKey = key("spell_level_menu", InputConstants.UNKNOWN.getValue());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (!languageLoaded && client.getResourceManager() != null) {
                    languageLoaded = true;
                    ModLanguageManager.loadLanguage(client, HUDConfig.getModLanguage());
                }

                while (toggleHUDKey.consumeClick()) {
                    showCustomHUD = !showCustomHUD;
                    String key = showCustomHUD ? "keybind.toggle_hud.on" : "keybind.toggle_hud.off";
                    client.player.displayClientMessage(ModLanguageManager.get(key), true);
                }

                while (statUpgradeMenuKey.consumeClick()) {
                    client.setScreen(new StatUpgradeScreen());
                }

                while (playerGlow.consumeClick()) {
                    Att2Triggers.send(1085);
                }

                while (collectItems.consumeClick()) {
                    Att2Triggers.send(1072);
                }

                while (questBook.consumeClick()) {
                    if (client.screen == null) {
                        QuestBookScreen.open();
                    }
                }

                while (whistle.consumeClick()) {
                    Att2Triggers.send(1070);
                }

                while (openHUDConfig.consumeClick()) {
                    client.setScreen(new HUDConfigScreen());
                }

                while (broadcastKey.consumeClick()) {
                    handleBroadcastKey(client);
                }

                while (pingKey.consumeClick()) {
                    handlePingKey(client);
                }

                while (weaponSkillKey.consumeClick()) {
                    if (client.player != null && client.screen == null && !InputSequencer.isBusy()) {
                        triggerWeaponSkill(client);
                    }
                }

                if (repairItemMenuKey.isDown()) {
                    if (repairReleased && client.screen == null) {
                        client.setScreen(new RepairMenuScreen());
                        repairReleased = false;
                    }
                } else {
                    repairReleased = true;
                }

                if (spellLevelMenuKey.isDown()) {
                    if (spellLevelReleased && client.screen == null) {
                        SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(client.player.getMainHandItem());
                        if (launcher != null && SpellSelectTriggers.isSupported(launcher.spellId)) {
                            client.setScreen(new SpellLevelRadialScreen(launcher));
                            spellLevelReleased = false;
                        }
                    }
                } else {
                    spellLevelReleased = true;
                }

                BroadcastScanner.tick(client);
            }
        });
    }

    private static void triggerWeaponSkill(Minecraft client) {
        if (client.player.isBlocking()) {
            InputSequencer.run(List.of(InputSequencer.Step.forceSneak(8), InputSequencer.Step.wait(2), InputSequencer.Step.holdUse(4)));
        } else if (client.player.getMainHandItem().is(ItemTags.AXES)) {
            InputSequencer.run(List.of(InputSequencer.Step.clickSwap()));
        } else {
            InputSequencer.run(List.of(InputSequencer.Step.forceSneak(6)));
        }
    }

    public static KeyMapping getRepairItemMenuKey() {
        return repairItemMenuKey;
    }

    public static void blockRepairUntilRelease() {
        repairReleased = false;
    }

    public static KeyMapping getSpellLevelMenuKey() {
        return spellLevelMenuKey;
    }

    public static int compareKeyCode() {
        if (compareShopKey == null) {
            return 340;
        }

        int code = KeyBindingHelper.getBoundKeyOf(compareShopKey).getValue();
        return code == InputConstants.UNKNOWN.getValue() ? 340 : code;
    }

    public static Component compareKeyLabel() {
        return compareShopKey == null ? Component.translatable("key.keyboard.left.shift") : compareShopKey.getTranslatedKeyMessage();
    }

    public static void blockSpellLevelUntilRelease() {
        spellLevelReleased = false;
    }

    private static void handleBroadcastKey(Minecraft client) {
        if (client.player != null) {
            if (!PartySync.isEnabled()) {
                client.player.displayClientMessage(Component.literal("§c" + ModLanguageManager.getString("keybind.broadcast.no_api")), true);
            } else {
                long now = System.currentTimeMillis();
                if (now - lastUsedTime < 5000L) {
                    long secondsLeft = (5000L - (now - lastUsedTime)) / 1000L;
                    String msg = ModLanguageManager.format("keybind.broadcast.cooldown", "s", secondsLeft);
                    client.player.displayClientMessage(Component.literal("§c" + msg), true);
                } else {
                    lastUsedTime = now;
                    PartySync.shareHeldItem(client);
                }
            }
        }
    }

    private static void handlePingKey(Minecraft client) {
        if (client.player != null) {
            if (!PartySync.isEnabled()) {
                client.player.displayClientMessage(Component.literal("§c" + ModLanguageManager.getString("keybind.ping.no_api")), true);
            } else {
                long now = System.currentTimeMillis();
                if (now - lastPingTime >= 250L) {
                    lastPingTime = now;
                    PartySync.pingLookedAt(client);
                }
            }
        }
    }
}
