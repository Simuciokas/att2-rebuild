package fr.poubone.att2.client.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;

public final class ShopCatalog {
    private final List<ShopOffer> offers = new ArrayList<>();
    private final List<ShopAction> actions = new ArrayList<>();
    private final LinkedHashSet<String> categories = new LinkedHashSet<>();
    private Component discount;
    private Component remaining;
    private Component powderStock;
    private ShopType type = ShopType.GENERAL;
    private ShopType lockedType;
    private boolean lastUsesEsc;

    void addOffer(ShopOffer offer) {
        this.offers.removeIf(existing -> existing.trigger() == offer.trigger());
        this.offers.add(offer);
        if (offer.category() != null && !offer.category().isBlank()) {
            this.categories.add(offer.category());
        }

        this.lastUsesEsc = this.computeUsesEsc();
    }

    void addAction(ShopAction action) {
        this.actions.removeIf(existing -> existing.trigger() == action.trigger());
        this.actions.add(action);
    }

    void addCategory(String category) {
        if (category != null && !category.isBlank()) {
            this.categories.add(category.trim());
        }
    }

    void setDiscount(Component discount) {
        this.discount = discount;
    }

    void setPowderStock(Component powderStock) {
        this.powderStock = powderStock;
    }

    void setRemaining(Component remaining) {
        this.remaining = remaining;
    }

    void lockType(ShopType type) {
        if (type != null) {
            this.lockedType = type;
            this.type = type;
        }
    }

    void inferType() {
        if (this.lockedType != null) {
            this.type = this.lockedType;
        } else {
            this.type = ShopType.infer(this);
        }
    }

    void clearOffers() {
        this.offers.clear();
    }

    void clear() {
        this.offers.clear();
        this.actions.clear();
        this.categories.clear();
        this.discount = null;
        this.remaining = null;
        this.powderStock = null;
        this.type = this.lockedType != null ? this.lockedType : ShopType.GENERAL;
        this.lastUsesEsc = false;
    }

    void clearLock() {
        this.lockedType = null;
        this.type = ShopType.GENERAL;
    }

    boolean isEmpty() {
        return this.offers.isEmpty() && this.actions.isEmpty();
    }

    boolean hasMending() {
        return this.actions.stream().anyMatch(ShopAction::isMending);
    }

    public List<ShopOffer> offers() {
        return Collections.unmodifiableList(this.offers);
    }

    public List<ShopAction> actions() {
        return Collections.unmodifiableList(this.actions);
    }

    public Set<String> categories() {
        return Collections.unmodifiableSet(this.categories);
    }

    public Component discount() {
        return this.discount;
    }

    public Component remaining() {
        return this.remaining;
    }

    public Component powderStock() {
        return this.powderStock;
    }

    public ShopType type() {
        return this.type;
    }

    public boolean usesEsc() {
        return this.offers.isEmpty() ? this.lastUsesEsc : this.computeUsesEsc();
    }

    private boolean computeUsesEsc() {
        int esc = 0;

        for (ShopOffer offer : this.offers) {
            if (priceLooksEsc(offer)) {
                esc++;
            }
        }

        return esc > 0 && esc * 2 >= this.offers.size();
    }

    private static boolean priceLooksEsc(ShopOffer offer) {
        String key = ShopModel.translationKey(offer.price()).toLowerCase();
        if (!key.contains("shop.esc") && !key.endsWith(".esc")) {
            String text = offer.price() == null ? "" : offer.price().getString().toLowerCase();
            return text.contains("esc") && !text.contains("chronoton");
        } else {
            return true;
        }
    }
}
