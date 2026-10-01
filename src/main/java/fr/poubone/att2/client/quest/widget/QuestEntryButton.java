package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.quest.QuestBookScreen;
import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.quest.QuestInfo;
import fr.poubone.att2.client.quest.QuestModel;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class QuestEntryButton extends QuestBookWidget {
    private final QuestInfo quest;
    private final QuestBookScreen screen;
    private final ItemStack icon;

    public QuestEntryButton(int x, int y, int width, int height, QuestInfo quest, QuestBookScreen screen) {
        super(x, y, width, height, quest.name());
        this.quest = quest;
        this.screen = screen;
        this.icon = new ItemStack(quest.isMain() ? Items.WRITTEN_BOOK : (quest.isDaily() ? Items.CLOCK : Items.PAPER));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = this.quest.key().equals(this.screen.getSelectedKey());
        boolean hovered = this.isHoveredOrFocused();
        int background = selected ? -2571635 : (hovered ? -2043218 : 1170716559);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, background);
        graphics.fill(
            this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, selected ? -7970772 : QuestBookSkin.statusColor(this.quest.status())
        );
        graphics.fill(this.getX() + 3, this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 1082548268);
        graphics.renderItem(this.icon, this.getX() + 6, this.getY() + (this.height - 16) / 2);
        Font font = Minecraft.getInstance().font;
        float scale = this.screen.textScale();
        int maxWidth = (int)((this.width - 32) / scale);
        String name = this.quest.plainName();
        if (font.width(name) > maxWidth) {
            name = font.plainSubstrByWidth(name, maxWidth - font.width("...")) + "...";
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(this.getX() + 27, this.getY() + (this.height < 25 ? (this.height - 9) / 2 : 3));
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, name, 0, 0, -13096159, false);
        graphics.pose().popMatrix();
        Component status = ModLanguageManager.get("quest_book.status." + this.quest.status().name().toLowerCase(Locale.ROOT));
        if (this.height >= 25) {
            graphics.drawString(font, status, this.getX() + 27, this.getY() + this.height - 9 - 2, QuestBookSkin.statusColor(this.quest.status()), false);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.screen.selectQuest(this.quest);
    }

    @Override
    public List<Component> getTooltipLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(this.quest.name().copy().withStyle(ChatFormatting.BOLD).withStyle(this.quest.status().formatting()));
        if (this.quest.isMain()) {
            int step = QuestModel.get().getMainStep();
            String progress = step < 0 ? "?" : String.valueOf(step);
            lines.add(
                Component.literal(ModLanguageManager.format("quest_book.main_progress", "step", progress, "max", QuestModel.get().getMainMaxStep()))
                    .withStyle(ChatFormatting.GRAY)
            );
        } else if (this.quest.isDaily()) {
            lines.add(
                Component.literal(ModLanguageManager.getString("quest_book.daily_city") + " ").append(this.quest.cityName()).withStyle(ChatFormatting.GRAY)
            );
            if (this.quest.remainingSeconds() >= 0) {
                lines.add(
                    Component.literal(ModLanguageManager.format("quest_book.daily_remaining", "time", this.quest.formatRemainingTime()))
                        .withStyle(ChatFormatting.GRAY)
                );
            }
        } else {
            lines.add(Component.literal(ModLanguageManager.format("quest_book.side_number", "n", this.quest.number())).withStyle(ChatFormatting.GRAY));
        }

        lines.add(ModLanguageManager.get("quest_book.status." + this.quest.status().name().toLowerCase()).withStyle(this.quest.status().formatting()));
        if (this.quest.hasDetails() || this.quest.isMain()) {
            lines.add(Component.literal(""));
            lines.add(ModLanguageManager.get("quest_book.tooltip.left_click").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
        }

        return lines;
    }

    public QuestInfo getQuest() {
        return this.quest;
    }
}
