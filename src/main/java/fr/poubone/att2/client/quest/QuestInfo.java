package fr.poubone.att2.client.quest;

import net.minecraft.network.chat.Component;

public record QuestInfo(QuestType type, int number, Component name, QuestStatus status, int progressTrigger, String city, int remainingSeconds) {
    public QuestInfo(QuestType type, int number, Component name, QuestStatus status, int progressTrigger) {
        this(type, number, name, status, progressTrigger, null, -1);
    }

    public boolean isMain() {
        return this.type == QuestType.MAIN;
    }

    public boolean isDaily() {
        return this.type == QuestType.DAILY;
    }

    public boolean hasDetails() {
        return this.progressTrigger > 0;
    }

    public String key() {
        return switch (this.type) {
            case MAIN -> "MAIN:main";
            case SIDE -> "SIDE:" + this.number;
            case DAILY -> "DAILY:" + this.city + ":" + this.number;
        };
    }

    public String plainName() {
        return this.name.getString();
    }

    public Component cityName() {
        if (this.city == null) {
            return Component.empty();
        }

        Component translated = Component.translatable("consciousness.dailyquest.list." + this.city);
        return translated.getString().startsWith("consciousness.") ? Component.literal(this.city) : translated;
    }

    public String formatRemainingTime() {
        return this.remainingSeconds < 0 ? "" : String.format("%d:%02d", this.remainingSeconds / 60, this.remainingSeconds % 60);
    }
}
