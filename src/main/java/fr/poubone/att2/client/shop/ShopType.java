package fr.poubone.att2.client.shop;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum ShopType {
    BLACKSMITH("blacksmith", -1525654, 24, 20, 24, 20),
    FOOD("food", -863069, 24, 20, 24, 20),
    ALCHEMIST("alchemist", -4788024, 24, 20, 24, 20),
    FLETCHER("fletcher", -2758480, 24, 20, 24, 20),
    DAHAL("dahal", -3557121, 24, 20, 24, 20),
    TAILOR("tailor", -998188, 24, 20, 24, 20),
    FISH("fish", -6368040, 24, 20, 24, 20),
    STABLE("stable", -1653622, 24, 20, 24, 20),
    RUNES("runes", -7020818, 24, 20, 24, 20),
    CHARLES("charles", -1655184, 24, 20, 24, 20),
    MINER("miner", -5257009, 24, 20, 24, 20),
    GENERAL("general", -1518168, 24, 20, 24, 20);

    public final String id;
    public final int titleColor;
    public final int insetLeft;
    public final int insetTop;
    public final int insetRight;
    public final int insetBottom;

    ShopType(String id, int titleColor, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        this.id = id;
        this.titleColor = titleColor;
        this.insetLeft = insetLeft;
        this.insetTop = insetTop;
        this.insetRight = insetRight;
        this.insetBottom = insetBottom;
    }

    public Identifier panel() {
        return Identifier.fromNamespaceAndPath("att2", "textures/shop/" + this.id + ".png");
    }

    public String titleKey() {
        return "shop.type." + this.id;
    }

    static ShopType infer(ShopCatalog catalog) {
        int blacksmith = 0;
        int food = 0;
        int potion = 0;
        int bow = 0;
        int spell = 0;
        int cloth = 0;
        int fish = 0;
        int ride = 0;
        int misc = 0;

        for (String category : catalog.categories()) {
            String key = normalize(category);
            if (containsAny(key, "arme", "weapon", "armure", "armor", "smith", "forge")) {
                blacksmith += 3;
            } else if (containsAny(key, "nourriture", "food", "comida", "nahrung")) {
                food += 3;
            } else if (containsAny(key, "potion", "alchim")) {
                potion += 3;
            } else if (containsAny(key, "arc", "bow", "fleche", "arrow")) {
                bow += 3;
            } else if (containsAny(key, "sort", "spell", "dahal", "magie", "perfection", "enhancement", "aprimor", "verbesser", "усилен", "mejora")) {
                spell += 3;
            } else if (containsAny(key, "vetement", "cloth", "habit", "tailor")) {
                cloth += 3;
            } else if (containsAny(key, "poisson", "fish", "peche", "fishing")) {
                fish += 3;
            } else if (containsAny(key, "monture", "ride", "horse", "cheval")) {
                ride += 3;
            } else {
                misc++;
            }
        }

        for (ShopOffer offer : catalog.offers()) {
            switch (kindOf(offer)) {
                case WEAPON:
                case ARMOR:
                    blacksmith += 2;
                    break;
                case FOOD:
                    food += 2;
                    break;
                case POTION:
                    potion += 2;
                    break;
                case BOW:
                    bow += 2;
                    break;
                case SPELL:
                    spell += 2;
                    break;
                case CLOTH:
                    cloth += 2;
                    break;
                case FISH:
                    fish += 2;
                    break;
                case RIDE:
                    ride += 2;
                    break;
                default:
                    misc++;
            }
        }

        if (catalog.hasMending()) {
            blacksmith += 6;
        }

        boolean bowStall = hasCategory(catalog, "arc", "bow", "fleche", "arrow") && !hasCategory(catalog, "armure", "armor");
        if (bowStall) {
            bow += 12;
        }

        int best = blacksmith;
        ShopType type = blacksmith > 0 ? BLACKSMITH : GENERAL;
        if (spell > best) {
            best = spell;
            type = DAHAL;
        }

        if (potion > best) {
            best = potion;
            type = ALCHEMIST;
        }

        if (bow > best && (bowStall || blacksmith < bow + 2)) {
            best = bow;
            type = FLETCHER;
        }

        if (cloth > best) {
            best = cloth;
            type = TAILOR;
        }

        if (fish > best) {
            best = fish;
            type = FISH;
        }

        if (ride > best) {
            best = ride;
            type = STABLE;
        }

        if (food > best) {
            type = FOOD;
        }

        if (bowStall && type == BLACKSMITH) {
            return FLETCHER;
        } else {
            return best == 0 && type == GENERAL ? GENERAL : type;
        }
    }

    static ShopType.Kind kindOf(ShopOffer offer) {
        String category = normalize(offer.category());
        if (containsAny(category, "arme", "weapon")) {
            return ShopType.Kind.WEAPON;
        }

        if (containsAny(category, "armure", "armor")) {
            return ShopType.Kind.ARMOR;
        }

        if (containsAny(category, "nourriture", "food")) {
            return ShopType.Kind.FOOD;
        }

        if (containsAny(category, "potion")) {
            return ShopType.Kind.POTION;
        }

        if (containsAny(category, "arc", "bow")) {
            return ShopType.Kind.BOW;
        }

        if (containsAny(category, "sort", "spell", "perfection", "enhancement")) {
            return ShopType.Kind.SPELL;
        }

        if (containsAny(category, "vetement", "cloth", "habit")) {
            return ShopType.Kind.CLOTH;
        }

        if (containsAny(category, "poisson", "fish")) {
            return ShopType.Kind.FISH;
        }

        if (containsAny(category, "monture", "ride", "horse")) {
            return ShopType.Kind.RIDE;
        }

        String equipment = normalize(offer.equipmentType());
        if (!equipment.contains("range") && !equipment.contains("bow")) {
            String key = normalize(offer.translationKey());
            if (key.contains("bow") || key.contains("arrow") || key.contains("fleche")) {
                return ShopType.Kind.BOW;
            }

            if (key.startsWith("weapon") || key.contains("melee")) {
                return ShopType.Kind.WEAPON;
            }

            if (key.startsWith("armor") || key.contains("armure")) {
                return ShopType.Kind.ARMOR;
            }

            if (key.contains("spell") || key.contains("sort") || key.contains("dahal")) {
                return ShopType.Kind.SPELL;
            }

            if (key.contains("potion")) {
                return ShopType.Kind.POTION;
            }

            if (key.contains("shop.apple")
                || key.contains("shop.bread")
                || key.contains("shop.food")
                || key.contains("shop.beef")
                || key.contains("shop.pork")
                || key.contains("shop.chicken")
                || key.contains("shop.rabbit")
                || key.contains("shop.mutton")
                || key.contains("carrot")
                || key.contains("cookie")
                || key.contains("potato")
                || key.contains("pie")
                || key.contains("stew")
                || key.contains("pumpkin")
                || key.contains("beetroot")
                || key.contains("kelp")) {
                return ShopType.Kind.FOOD;
            }

            if (key.contains("cloth") || key.contains("habit")) {
                return ShopType.Kind.CLOTH;
            }

            if (key.contains("fish") || key.contains("poisson")) {
                return ShopType.Kind.FISH;
            }

            if (key.contains("horse") || key.contains("ride") || key.contains("camel") || key.contains("pig") || key.contains("mule")) {
                return ShopType.Kind.RIDE;
            }

            if (equipment.contains("melee") || equipment.contains("weapon")) {
                return ShopType.Kind.WEAPON;
            }

            if (equipment.contains("armor")) {
                return ShopType.Kind.ARMOR;
            }

            if (equipment.contains("spell")) {
                return ShopType.Kind.SPELL;
            }

            ItemStack stack = offer.stack();
            if (stack.is(Items.APPLE)
                || stack.is(Items.BREAD)
                || stack.is(Items.CARROT)
                || stack.is(Items.COOKIE)
                || stack.is(Items.POTATO)
                || stack.is(Items.BAKED_POTATO)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.MUSHROOM_STEW)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.BEEF)
                || stack.is(Items.PORKCHOP)
                || stack.is(Items.CHICKEN)
                || stack.is(Items.RABBIT)
                || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.MUTTON)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.BEETROOT_SOUP)
                || stack.is(Items.DRIED_KELP)
                || stack.is(Items.MELON_SLICE)
                || stack.is(Items.PUMPKIN)) {
                return ShopType.Kind.FOOD;
            }

            if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.GLASS_BOTTLE)) {
                return ShopType.Kind.POTION;
            }

            if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) {
                return ShopType.Kind.BOW;
            }

            if (!stack.is(Items.COD)
                && !stack.is(Items.SALMON)
                && !stack.is(Items.COOKED_COD)
                && !stack.is(Items.COOKED_SALMON)
                && !stack.is(Items.TROPICAL_FISH)
                && !stack.is(Items.PUFFERFISH)
                && !stack.is(Items.FISHING_ROD)) {
                if (!stack.is(Items.SADDLE)
                    && !stack.is(Items.LEATHER_HORSE_ARMOR)
                    && !stack.is(Items.IRON_HORSE_ARMOR)
                    && !stack.is(Items.GOLDEN_HORSE_ARMOR)
                    && !stack.is(Items.DIAMOND_HORSE_ARMOR)
                    && !stack.is(Items.CAMEL_SPAWN_EGG)
                    && !stack.is(Items.HORSE_SPAWN_EGG)
                    && (!stack.is(Items.ENCHANTED_BOOK) || !key.contains("horse"))) {
                    if (!stack.is(Items.ENCHANTED_BOOK) || !key.contains("spell") && !pathContainsSpell(offer)) {
                        return !stack.is(Items.LEATHER)
                                && !stack.is(Items.LEATHER_HELMET)
                                && !stack.is(Items.LEATHER_CHESTPLATE)
                                && !stack.is(Items.LEATHER_LEGGINGS)
                                && !stack.is(Items.LEATHER_BOOTS)
                            ? ShopType.Kind.OTHER
                            : ShopType.Kind.CLOTH;
                    } else {
                        return ShopType.Kind.SPELL;
                    }
                } else {
                    return ShopType.Kind.RIDE;
                }
            } else {
                return ShopType.Kind.FISH;
            }
        } else {
            return ShopType.Kind.BOW;
        }
    }

    private static boolean pathContainsSpell(ShopOffer offer) {
        return offer.sprite() != null && offer.sprite().getPath().contains("spell");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            switch (c) {
                case 'à':
                case 'â':
                case 'ä':
                    out.append('a');
                    break;
                case 'á':
                case 'ã':
                case 'å':
                case 'æ':
                case 'ì':
                case 'í':
                case 'ð':
                case 'ñ':
                case 'ò':
                case 'ó':
                case 'õ':
                case '÷':
                case 'ø':
                case 'ú':
                default:
                    out.append(c);
                    break;
                case 'ç':
                    out.append('c');
                    break;
                case 'è':
                case 'é':
                case 'ê':
                case 'ë':
                    out.append('e');
                    break;
                case 'î':
                case 'ï':
                    out.append('i');
                    break;
                case 'ô':
                case 'ö':
                    out.append('o');
                    break;
                case 'ù':
                case 'û':
                case 'ü':
                    out.append('u');
            }
        }

        return out.toString();
    }

    private static boolean hasCategory(ShopCatalog catalog, String... needles) {
        for (String category : catalog.categories()) {
            if (containsAny(normalize(category), needles)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }

        return false;
    }

    enum Kind {
        WEAPON,
        ARMOR,
        FOOD,
        POTION,
        BOW,
        SPELL,
        CLOTH,
        FISH,
        RIDE,
        OTHER;
    }
}
