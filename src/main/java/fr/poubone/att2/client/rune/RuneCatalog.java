package fr.poubone.att2.client.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RuneCatalog {
    public static final int MENU_LOW = 336;
    public static final int MENU_MID = 337;
    public static final int MENU_HIGH = 338;
    public static final int MENU_WORDS = 341;
    public static final int MENU_OTHER = 342;
    public static final int MENU_ARROWS = 3409;
    private static final RuneCatalog.RuneDef[] RUNES = new RuneCatalog.RuneDef[]{
        def("gal", 0, 375, "1_gal"),
        def("tha", 1, 376, "2_tha"),
        def("fus", 2, 377, "3_fus"),
        def("org", 3, 378, "4_org"),
        def("jo", 4, 379, "5_jo"),
        def("ra", 5, 380, "6_ra"),
        def("nym", 6, 381, "7_nym"),
        def("inu", 7, 382, "8_inu"),
        def("hal", 8, 383, "9_hal"),
        def("von", 9, 384, "10_von"),
        def("ehl", 10, 385, "11_ehl"),
        def("ave", 11, 386, "12_ave"),
        def("chu", 12, 387, "13_chu"),
        def("for", 13, 388, "14_for"),
        def("da", 14, 389, "15_da"),
        def("wej", 15, 390, "16_wej"),
        def("ust", 16, 391, "17_ust"),
        def("lya", 17, 392, "18_lya"),
        def("qi", 18, 366, "19_qi"),
        def("bex", 19, 367, "20_bex"),
        def("puh", 20, 368, "21_puh"),
        def("syl", 21, 369, "22_syl"),
        def("yog", 22, 370, "23_yog"),
        def("kan", 23, 371, "24_kan"),
        def("xul", 24, 372, "25_xul"),
        def("zen", 25, 373, "26_zen"),
        def("mot", 26, 374, "27_mot")
    };
    private static final List<RuneCatalog.WordDef> WORDS = List.of(
        word(0, 343, ing("gal", 1), ing("org", 1), ing("inu", 1)),
        word(1, 354, ing("fus", 1), ing("ra", 1), ing("jo", 1)),
        word(2, 358, ing("nym", 1), ing("ehl", 1), ing("ave", 1)),
        word(3, 359, ing("for", 1), ing("tha", 1), ing("gal", 1), ing("hal", 1)),
        word(4, 360, ing("inu", 1), ing("von", 1), ing("ust", 1)),
        word(5, 361, ing("jo", 1), ing("fus", 1), ing("nym", 1), ing("da", 1)),
        word(6, 362, ing("ehl", 1), ing("hal", 1), ing("ra", 2)),
        word(7, 363, ing("wej", 1), ing("ust", 1), ing("chu", 1), ing("tha", 1)),
        word(8, 364, ing("org", 1), ing("ave", 1), ing("hal", 1), ing("gal", 1), ing("ehl", 1)),
        word(9, 365, ing("da", 1), ing("for", 1), ing("inu", 1)),
        word(10, 344, ing("chu", 1), ing("lya", 1), ing("ehl", 1)),
        word(11, 345, ing("lya", 1), ing("nym", 1), ing("fus", 1), ing("von", 1)),
        word(12, 346, ing("ave", 1), ing("qi", 1), ing("gal", 1), ing("jo", 1), ing("wej", 1)),
        word(13, 347, ing("tha", 1), ing("bex", 1), ing("for", 1)),
        word(14, 348, ing("puh", 1), ing("wej", 1), ing("von", 1), ing("org", 1)),
        word(15, 349, ing("syl", 1), ing("da", 1), ing("ave", 1), ing("qi", 1), ing("ra", 1)),
        word(16, 350, ing("von", 1), ing("yog", 2)),
        word(17, 351, ing("kan", 1), ing("chu", 1), ing("bex", 1), ing("puh", 1)),
        word(18, 352, ing("ust", 1), ing("xul", 1), ing("lya", 1), ing("zen", 1), ing("da", 1)),
        word(19, 353, ing("mot", 1), ing("syl", 1), ing("kan", 1), ing("xul", 1), ing("zen", 1)),
        word(20, 355, ing("bex", 1), ing("lya", 1), ing("qi", 1), ing("wej", 1), ing("yog", 1)),
        word(21, 356, ing("hal", 1), ing("kan", 1), ing("puh", 1), ing("syl", 1), ing("chu", 1))
    );
    private static final List<RuneCatalog.ArrowDef> ARROWS = List.of(
        new RuneCatalog.ArrowDef("explosive_1", "att2.item.special_arrow.explosive_arrow_1.name", 4, List.of(ing("org", 1)), null),
        new RuneCatalog.ArrowDef("poisoned_1", "att2.item.special_arrow.poisoned_arrow_1.name", 4, List.of(ing("jo", 1)), null),
        new RuneCatalog.ArrowDef("tracking_1", "att2.item.special_arrow.tracking_arrow_1.name", 4, List.of(ing("ra", 1)), null),
        new RuneCatalog.ArrowDef(
            "explosive_2", "att2.item.special_arrow.explosive_arrow_2.name", 4, List.of(ing("nym", 1)), "att2.item.special_arrow.explosive_arrow_1.name"
        ),
        new RuneCatalog.ArrowDef(
            "poisoned_2", "att2.item.special_arrow.poisoned_arrow_2.name", 4, List.of(ing("inu", 1)), "att2.item.special_arrow.poisoned_arrow_1.name"
        ),
        new RuneCatalog.ArrowDef(
            "tracking_2", "att2.item.special_arrow.tracking_arrow_2.name", 4, List.of(ing("hal", 1)), "att2.item.special_arrow.tracking_arrow_1.name"
        ),
        new RuneCatalog.ArrowDef(
            "explosive_3", "att2.item.special_arrow.explosive_arrow_3.name", 4, List.of(ing("von", 1)), "att2.item.special_arrow.explosive_arrow_2.name"
        ),
        new RuneCatalog.ArrowDef(
            "poisoned_3", "att2.item.special_arrow.poisoned_arrow_3.name", 4, List.of(ing("ehl", 1)), "att2.item.special_arrow.poisoned_arrow_2.name"
        ),
        new RuneCatalog.ArrowDef(
            "tracking_3", "att2.item.special_arrow.tracking_arrow_3.name", 4, List.of(ing("ave", 1)), "att2.item.special_arrow.tracking_arrow_2.name"
        )
    );
    private static final List<RuneCatalog.InfoDef> OTHERS = List.of(
        new RuneCatalog.InfoDef("esc_2", "item.recipe.esc", null),
        new RuneCatalog.InfoDef("esc_7", "item.recipe.esc", null),
        new RuneCatalog.InfoDef("esc_25", "item.recipe.esc", null),
        new RuneCatalog.InfoDef("xp", "att2.runes.recipe.xp", "att2.runes.recipe.xp.tip"),
        new RuneCatalog.InfoDef("reforge", "att2.runes.recipe.item_reforging", "att2.runes.recipe.item_reforging.tip"),
        new RuneCatalog.InfoDef("loot_runes", "item.recipe.loot_runes", null),
        new RuneCatalog.InfoDef("extraloot", "item.recipe.extraloot", null),
        new RuneCatalog.InfoDef("elixir", "item.recipe.elixirvitae", null),
        new RuneCatalog.InfoDef("rune_bundle", "item.rune_bundle.name", null),
        new RuneCatalog.InfoDef("spell_bundle_1", "att2.misc.spell_bundle_1.name", null),
        new RuneCatalog.InfoDef("spell_bundle_2", "att2.misc.spell_bundle_2.name", null),
        new RuneCatalog.InfoDef("spell_bundle_3", "att2.misc.spell_bundle_3.name", null)
    );
    private static final List<RuneCatalog.BonusDef> BONUSES = List.of(
        new RuneCatalog.BonusDef("#XPTotal", "rune_codex.bonus.xp", "%"),
        new RuneCatalog.BonusDef("#ChronotonTotal", "rune_codex.bonus.chronoton", "%"),
        new RuneCatalog.BonusDef("#HealthTotal", "rune_codex.bonus.health", ""),
        new RuneCatalog.BonusDef("#BonusDahalMax_Total", "rune_codex.bonus.dahal", ""),
        new RuneCatalog.BonusDef("#BonusSpellXP", "rune_codex.bonus.spell_xp", ""),
        new RuneCatalog.BonusDef("#CooldownTotal", "rune_codex.bonus.cooldown", "%"),
        new RuneCatalog.BonusDef("#TimePotionTotal", "rune_codex.bonus.potion", "s"),
        new RuneCatalog.BonusDef("#BonusLootBoss", "rune_codex.bonus.loot", "")
    );
    private static final Map<Integer, RuneCatalog.Tab> MENU = Map.of(
        336,
        RuneCatalog.Tab.SYNTH_LOW,
        337,
        RuneCatalog.Tab.SYNTH_MID,
        338,
        RuneCatalog.Tab.SYNTH_HIGH,
        341,
        RuneCatalog.Tab.WORDS,
        342,
        RuneCatalog.Tab.OTHER,
        3409,
        RuneCatalog.Tab.ARROWS
    );

    private static RuneCatalog.RuneDef def(String id, int level, int trigger, String holder) {
        return new RuneCatalog.RuneDef(id, level, trigger, holder, holder, holder + "_esc", "RUNE_" + id.toUpperCase());
    }

    private static RuneCatalog.Ingredient ing(String id, int n) {
        return new RuneCatalog.Ingredient(id, n);
    }

    private static RuneCatalog.WordDef word(int id, int trigger, RuneCatalog.Ingredient... ings) {
        return new RuneCatalog.WordDef(id, trigger, List.of(ings));
    }

    private RuneCatalog() {
    }

    public static Optional<RuneCatalog.RuneDef> byId(String id) {
        for (RuneCatalog.RuneDef rune : RUNES) {
            if (rune.id().equals(id)) {
                return Optional.of(rune);
            }
        }

        return Optional.empty();
    }

    public static Optional<RuneCatalog.RuneDef> byLevel(int level) {
        return level >= 0 && level < RUNES.length ? Optional.of(RUNES[level]) : Optional.empty();
    }

    public static List<RuneCatalog.RuneDef> allRunes() {
        return List.of(RUNES);
    }

    public static List<RuneCatalog.RuneDef> runes(RuneCatalog.Tab tab) {
        List<RuneCatalog.RuneDef> out = new ArrayList<>();

        for (RuneCatalog.RuneDef rune : RUNES) {
            boolean match = switch (tab) {
                case SYNTH_LOW -> rune.level() <= 8;
                case SYNTH_MID -> rune.level() >= 9 && rune.level() <= 17;
                case SYNTH_HIGH -> rune.level() >= 18;
                default -> false;
            };
            if (match) {
                out.add(rune);
            }
        }

        return out;
    }

    public static List<RuneCatalog.WordDef> words() {
        return WORDS;
    }

    public static List<RuneCatalog.ArrowDef> arrows() {
        return ARROWS;
    }

    public static Optional<RuneCatalog.ArrowDef> arrowByNameKey(String nameKey) {
        if (nameKey == null) {
            return Optional.empty();
        }

        for (RuneCatalog.ArrowDef arrow : ARROWS) {
            if (arrow.nameKey().equals(nameKey)) {
                return Optional.of(arrow);
            }
        }

        return Optional.empty();
    }

    public static Optional<RuneCatalog.ArrowDef> arrowById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        for (RuneCatalog.ArrowDef arrow : ARROWS) {
            if (arrow.id().equals(id)) {
                return Optional.of(arrow);
            }
        }

        return Optional.empty();
    }

    public static List<RuneCatalog.InfoDef> others() {
        return OTHERS;
    }

    public static List<RuneCatalog.BonusDef> bonuses() {
        return BONUSES;
    }

    public static boolean isWorkshopTrigger(int trigger) {
        return isMenuTrigger(trigger) || trigger >= 343 && trigger <= 392;
    }

    public static boolean isMenuTrigger(int trigger) {
        return MENU.containsKey(trigger);
    }

    public static RuneCatalog.Tab menuTab(int trigger) {
        return MENU.get(trigger);
    }

    public static List<String> syncObjectives() {
        return List.of("PRICES");
    }

    public record ArrowDef(String id, String nameKey, int resultCount, List<RuneCatalog.Ingredient> runes, String priorArrowKey) {
        public int baseArrowCount() {
            return 4;
        }

        public boolean usesVanillaBaseArrow() {
            return this.priorArrowKey == null;
        }
    }

    public record BonusDef(String holder, String langKey, String suffix) {
    }

    public record InfoDef(String id, String nameKey, String tipKey) {
    }

    public record Ingredient(String runeId, int count) {
    }

    public record RuneDef(String id, int level, int craftTrigger, String unlockHolder, String priceHolder, String escPriceHolder, String scoreObjective) {
        public String translationKey() {
            return "item.rune.name." + this.level;
        }
    }

    public enum Tab {
        SYNTH_LOW,
        SYNTH_MID,
        SYNTH_HIGH,
        WORDS,
        OTHER,
        ARROWS,
        BONUS;
    }

    public record WordDef(int wordId, int trigger, List<RuneCatalog.Ingredient> ingredients) {
        public String nameKey() {
            return switch (this.wordId) {
                case 20 -> "item.runeword.stock_upgrade";
                case 21 -> "att2.spell31.name";
                default -> "item.runeword." + this.wordId + ".name";
            };
        }
    }
}
