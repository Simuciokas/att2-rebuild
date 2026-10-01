package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;

public record ShopAction(int trigger, Component label, Component tip, ShopAction.Kind kind) {
    public boolean isMending() {
        return this.kind == ShopAction.Kind.MENDING_PRICE
            || this.kind == ShopAction.Kind.MENDING_REPAIR
            || this.kind == ShopAction.Kind.MENDING_TOOLS
            || this.kind == ShopAction.Kind.MENDING_SLOT;
    }

    public enum Kind {
        RESET,
        BACK,
        MENDING_PRICE,
        MENDING_REPAIR,
        MENDING_TOOLS,
        MENDING_SLOT,
        OTHER;
    }
}
