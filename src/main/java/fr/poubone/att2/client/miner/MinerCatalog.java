package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.rune.RuneCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MinerCatalog {
    public static final int MENU_POWDER = 3488;
    public static final int MENU_BUY = 3489;
    public static final int ACTION_RESIN = 3490;
    public static final int ACTION_ORE = 3536;
    public static final int MENU_EXCHANGE = 3535;
    public static final int BUY_FIRST = 3495;
    public static final int BUY_LAST = 3521;
    public static final int POWDER_FIRST = 3491;
    public static final int POWDER_LAST = 3494;
    private static final List<MinerCatalog.BuyOffer> BUYS;
    private static final List<MinerCatalog.PowderOffer> POWDERS = List.of(
        new MinerCatalog.PowderOffer(50, 1, 3491),
        new MinerCatalog.PowderOffer(250, 5, 3492),
        new MinerCatalog.PowderOffer(500, 10, 3493),
        new MinerCatalog.PowderOffer(1000, 20, 3494)
    );

    private MinerCatalog() {
    }

    public static List<MinerCatalog.BuyOffer> buys() {
        return BUYS;
    }

    public static List<MinerCatalog.PowderOffer> powders() {
        return POWDERS;
    }

    public static Optional<RuneCatalog.RuneDef> runeByName(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String id = raw.strip().toLowerCase();
        return RuneCatalog.byId(id);
    }

    public static boolean isMenuTrigger(int trigger) {
        return trigger == 3488 || trigger == 3489 || trigger == 3535;
    }

    public static boolean isBuyTrigger(int trigger) {
        return trigger >= 3495 && trigger <= 3521;
    }

    public static boolean isPowderTrigger(int trigger) {
        return trigger >= 3491 && trigger <= 3494;
    }

    public static boolean isExchangeTrigger(int trigger) {
        return trigger == 3532 || trigger == 3533 || trigger == 3534 || trigger >= 3576 && trigger <= 3582;
    }

    public static boolean isActionTrigger(int trigger) {
        return trigger == 3490 || trigger == 3536;
    }

    public static boolean isPrisonerGameplayTrigger(int trigger) {
        return isMenuTrigger(trigger) || isBuyTrigger(trigger) || isPowderTrigger(trigger) || isExchangeTrigger(trigger) || isActionTrigger(trigger);
    }

    public static MinerCatalog.Tab tabForTrigger(int trigger) {
        if (isPowderTrigger(trigger) || trigger == 3488) {
            return MinerCatalog.Tab.POWDER;
        } else if (isExchangeTrigger(trigger) || trigger == 3535) {
            return MinerCatalog.Tab.EXCHANGE;
        } else {
            return isActionTrigger(trigger) ? MinerCatalog.Tab.ACTIONS : MinerCatalog.Tab.BUY;
        }
    }

    static {
        List<MinerCatalog.BuyOffer> buys = new ArrayList<>();

        for (RuneCatalog.RuneDef rune : RuneCatalog.allRunes()) {
            int n = rune.level() + 1;
            buys.add(new MinerCatalog.BuyOffer(rune, 3495 + rune.level(), n, n + "_" + rune.id() + "_chronoton"));
        }

        BUYS = List.copyOf(buys);
    }

    public record BuyOffer(RuneCatalog.RuneDef rune, int trigger, int resinRequired, String priceHolder) {
    }

    public record ExchangeOffer(int trigger, List<String> ingredients, String result) {
    }

    public record PowderOffer(int powder, int esc, int trigger) {
    }

    public enum Tab {
        BUY,
        POWDER,
        EXCHANGE,
        ACTIONS;
    }
}
