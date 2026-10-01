package fr.poubone.att2.client.shop;

public record ShopSeller(int trigger, String npcId, ShopType type, boolean repair, boolean reset, boolean gambling) {
}
