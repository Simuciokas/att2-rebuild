package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.quest.QuestBookSkin;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class JournalButton extends QuestBookWidget {
    private final Consumer<MouseButtonEvent> press;
    private final BooleanSupplier selected;
    private final BooleanSupplier available;
    private final List<Component> tooltip;
    private final ItemStack item;
    private final String icon;

    public JournalButton(
        int x,
        int y,
        int w,
        int h,
        Component label,
        ItemStack item,
        String icon,
        List<Component> tooltip,
        BooleanSupplier selected,
        BooleanSupplier available,
        Consumer<MouseButtonEvent> press
    ) {
        super(x, y, w, h, label);
        this.item = item;
        this.icon = icon;
        this.tooltip = tooltip;
        this.selected = selected;
        this.available = available;
        this.press = press;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.active = this.available.getAsBoolean();
        boolean lit = this.selected.getAsBoolean();
        QuestBookSkin.button(g, this.getX(), this.getY(), this.width, this.height, lit, this.active && this.isHoveredOrFocused());
        Font font = Minecraft.getInstance().font;
        if (this.icon != null) {
            QuestBookSkin.icon(g, this.icon, this.getX() + (this.width - 12) / 2, this.getY() + (this.height - 12) / 2, 12, 12);
        } else if (font.width(this.getMessage()) <= this.width - 8) {
            g.drawString(
                font,
                this.getMessage(),
                this.getX() + (this.width - font.width(this.getMessage())) / 2,
                this.getY() + (this.height - 9) / 2,
                lit ? -660784 : -13096159,
                false
            );
        } else if (!this.item.isEmpty()) {
            g.renderItem(this.item, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2);
        } else {
            String shortText = font.plainSubstrByWidth(this.getMessage().getString(), this.width - 12);
            g.drawString(font, shortText, this.getX() + 6, this.getY() + (this.height - 9) / 2, -13096159, false);
        }

        if (!this.active) {
            g.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 1894638280);
        }

        if (this.isFocused()) {
            g.fill(this.getX() + 4, this.getY() + this.height - 4, this.getX() + this.width - 4, this.getY() + this.height - 3, -13096159);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.available.getAsBoolean()) {
            this.press.accept(event);
        }
    }

    @Override
    public List<Component> getTooltipLines() {
        return this.tooltip;
    }
}
