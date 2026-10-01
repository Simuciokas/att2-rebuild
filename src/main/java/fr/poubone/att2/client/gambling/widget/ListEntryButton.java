package fr.poubone.att2.client.gambling.widget;

import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopType;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ListEntryButton extends QuestBookWidget {
    private final Component label;
    private final Component value;
    private final List<Component> tooltip;
    private final BooleanSupplier enabled;
    private final Runnable onPress;

    public ListEntryButton(
        int x, int y, int width, int height, Component label, Component value, List<Component> tooltip, BooleanSupplier enabled, Runnable onPress
    ) {
        super(x, y, width, height, label);
        this.label = label;
        this.value = value;
        this.tooltip = tooltip;
        this.enabled = enabled;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean active = this.enabled.getAsBoolean();
        ShopSkin.texture(
            graphics,
            ShopType.CHARLES,
            active && this.isHovered() ? "category_hover" : "category_normal",
            this.getX(),
            this.getY(),
            this.width,
            this.height,
            250,
            58
        );
        Font font = Minecraft.getInstance().font;
        int textColor = active ? -13096159 : -8025463;
        int valueWidth = this.value == null ? 0 : font.width(this.value) + 4;
        String name = font.plainSubstrByWidth(this.label.getString(), this.width - 4 - valueWidth);
        graphics.drawString(
            font,
            Component.literal(name).withStyle(this.label.getStyle()),
            this.getX() + 6,
            this.getY() + (this.height - 8) / 2,
            this.label.getStyle().getColor() == null ? textColor : -1,
            false
        );
        if (this.value != null) {
            graphics.drawString(font, this.value, this.getX() + this.width - valueWidth - 4, this.getY() + (this.height - 8) / 2, -9413049, false);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.enabled.getAsBoolean()) {
            this.onPress.run();
        }
    }

    @Override
    public List<Component> getTooltipLines() {
        return this.tooltip;
    }
}
