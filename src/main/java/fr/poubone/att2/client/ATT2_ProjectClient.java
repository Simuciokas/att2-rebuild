package fr.poubone.att2.client;

import com.lootbeams.LootBeams;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.ChronotonPickupTracker;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.data.LootShimmerTracker;
import fr.poubone.att2.client.data.MapStatBar;
import fr.poubone.att2.client.data.MapStatDisplayEnabler;
import fr.poubone.att2.client.data.PlayerInfoEnabler;
import fr.poubone.att2.client.data.PlayerInfoModel;
import fr.poubone.att2.client.data.ScoreboardSync;
import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellLevelSelector;
import fr.poubone.att2.client.data.StatUpgradeModel;
import fr.poubone.att2.client.discord.DiscordPresence;
import fr.poubone.att2.client.gambling.GamblingModel;
import fr.poubone.att2.client.hud.ChronotonDisplay;
import fr.poubone.att2.client.hud.CityToast;
import fr.poubone.att2.client.hud.HudFx;
import fr.poubone.att2.client.hud.HudRenderer;
import fr.poubone.att2.client.hud.ManaOrbDisplay;
import fr.poubone.att2.client.hud.ModToast;
import fr.poubone.att2.client.hud.StatIconsDisplay;
import fr.poubone.att2.client.hud.XPDisplay;
import fr.poubone.att2.client.input.InputSequencer;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.miner.MinerShopModel;
import fr.poubone.att2.client.quest.QuestModel;
import fr.poubone.att2.client.rune.RuneCodexModel;
import fr.poubone.att2.client.rune.WorkshopHopperCraft;
import fr.poubone.att2.client.shop.ShopModel;
import fr.poubone.att2.client.shop.ShopSeller;
import fr.poubone.att2.client.shop.ShopSellers;
import fr.poubone.att2.client.sync.PartySync;
import fr.poubone.att2.client.sync.PingMarkers;
import fr.poubone.att2.client.teleport.TeleportAnimationConfig;
import fr.poubone.att2.client.teleport.TeleportTransitionController;
import fr.poubone.att2.client.teleport.WaypointTeleportDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

@Environment(EnvType.CLIENT)
public class ATT2_ProjectClient implements ClientModInitializer {
    public static final String MOD_ID = "att2";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("att2", path);
    }

    @Override
    public void onInitializeClient() {
        TeleportAnimationConfig.load();
        new LootBeams().onInitializeClient();
        KeybindManager.register();
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, id("hud"), HudRenderer::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, id("pings"), PingMarkers::renderHud);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (ShopSellers.isEldric(entity)) {
                Minecraft.getInstance().execute(() -> MinerShopModel.get().openFromNpc());
                return InteractionResult.PASS;
            }

            ShopSeller seller = ShopSellers.matchEntity(entity);
            if (seller == null) {
                return InteractionResult.PASS;
            }

            ShopModel.get().openFromNpc(seller);
            return InteractionResult.PASS;
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> MinerShopModel.get().pollNpcUse(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Att2Triggers.tick(client);
            MapStatDisplayEnabler.tick(client);
            PlayerInfoEnabler.tick(client);
            InputSequencer.tick(client);
            ScoreboardSync.tick(client);
            WaypointTeleportDetector.tick(client);
            TeleportTransitionController.tick(client);
            QuestModel.get().tick(client);
            GamblingModel.get().tick(client);
            RuneCodexModel.get().tick();
            WorkshopHopperCraft.tick(client);
            MinerShopModel.get().tick();
            ShopModel.get().tick(client);
            CityToast.tick();
            ModToast.tick();
            HudFx.tick();
            ChronotonPickupTracker.tick(client);
            LootShimmerTracker.tick(client);
            SpellLauncherTracker.tick(client);
            SpellLevelSelector.tick();
            DiscordPresence.tick();
            PartySync.tick(client);
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay && ScoreboardSync.isCommandFeedback(message)) {
                return false;
            } else if (!overlay && GamblingModel.get().onSystemMessage(message)) {
                return false;
            } else if (!overlay && RuneCodexModel.get().onSystemMessage(message)) {
                return false;
            } else if (!overlay && MinerShopModel.get().onSystemMessage(message)) {
                return false;
            } else {
                return !overlay && ShopModel.get().onSystemMessage(message) ? false : QuestModel.get().onSystemMessage(message, overlay);
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ScoreboardSync.reset();
            MapStatBar.reset();
            PlayerInfoEnabler.reset();
            PlayerInfoModel.reset();
            CurrencyModel.reset();
            QuestModel.get().reset();
            StatUpgradeModel.reset();
            GamblingModel.get().reset();
            RuneCodexModel.get().reset();
            MinerShopModel.get().reset();
            ShopModel.get().reset();
            WaypointTeleportDetector.reset();
            Att2Triggers.reset();
            MapStatDisplayEnabler.reset();
            CityToast.reset();
            ModToast.reset();
            HudFx.reset();
            ManaOrbDisplay.reset();
            XPDisplay.reset();
            ChronotonDisplay.reset();
            StatIconsDisplay.reset();
            ChronotonPickupTracker.reset();
            LootShimmerTracker.reset();
            SpellLauncherTracker.reset();
            SpellLevelSelector.reset();
            DiscordPresence.onJoin();
            PartySync.onJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ScoreboardSync.reset();
            MapStatBar.reset();
            PlayerInfoEnabler.reset();
            PlayerInfoModel.reset();
            CurrencyModel.reset();
            QuestModel.get().reset();
            StatUpgradeModel.reset();
            GamblingModel.get().reset();
            RuneCodexModel.get().reset();
            MinerShopModel.get().reset();
            ShopModel.get().reset();
            WaypointTeleportDetector.reset();
            Att2Triggers.reset();
            MapStatDisplayEnabler.reset();
            CityToast.reset();
            ModToast.reset();
            HudFx.reset();
            ManaOrbDisplay.reset();
            XPDisplay.reset();
            ChronotonDisplay.reset();
            StatIconsDisplay.reset();
            ChronotonPickupTracker.reset();
            LootShimmerTracker.reset();
            SpellLauncherTracker.reset();
            SpellLevelSelector.reset();
            DiscordPresence.onDisconnect();
            PartySync.onDisconnect();
        });
    }
}
