package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopTooltips;
import fr.poubone.att2.client.shop.ShopType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class RuneCardButton extends AbstractWidget {
    private final ItemStack result;
    private final Component displayName;
    private final Component priceLabel;
    private final List<ItemStack> ingredients;
    private final List<Component> extraTooltip;
    private final boolean clickable;
    private final CraftClickHandler onCraft;
    private final ShopType theme;

    public RuneCardButton(
        int x,
        int y,
        int width,
        int height,
        ItemStack result,
        Component displayName,
        Component priceLabel,
        List<ItemStack> ingredients,
        List<Component> extraTooltip,
        boolean clickable,
        CraftClickHandler onCraft,
        ShopType theme
    ) {
        super(x, y, width, height, displayName);
        this.result = result;
        this.displayName = displayName;
        this.priceLabel = priceLabel;
        this.ingredients = List.copyOf(ingredients);
        this.extraTooltip = List.copyOf(extraTooltip);
        this.clickable = clickable;
        this.onCraft = onCraft;
        this.theme = theme;
    }

    public List<Component> tooltipLines() {
        List<Component> lines = new ArrayList<>(ShopTooltips.of(this.result));
        if (lines.isEmpty()) {
            lines.add(this.displayName);
        }

        lines.addAll(this.extraTooltip);
        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = this.isHovered() && this.clickable;
        ShopSkin.texture(graphics, this.theme, hover ? "card_hover" : "card_normal", this.getX(), this.getY(), this.width, this.height, 312, 252);
        float s = this.width / 312.0F;
        int iconSize = Math.max(1, Math.round(72.0F * s));
        int iconX = this.getX() + (this.width - iconSize) / 2;
        int iconY = this.getY() + Math.round(22.0F * s);
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(iconSize / 16.0F, iconSize / 16.0F);
        graphics.renderItem(this.result, 0, 0);
        graphics.renderItemDecorations(Minecraft.getInstance().font, this.result, 0, 0);
        graphics.pose().popMatrix();
        ShopTheme.text(
            graphics,
            this.displayName,
            this.getX() + Math.round(10.0F * s),
            this.getY() + Math.round(100.0F * s),
            this.width - Math.round(20.0F * s),
            s * 2.0F,
            -13096159,
            true
        );
        int buyX = this.getX() + Math.round(14.0F * s);
        int buyY = this.getY() + Math.round(128.0F * s);
        int buyW = Math.round(284.0F * s);
        int buyH = Math.round(32.0F * s);
        ShopSkin.texture(graphics, this.theme, hover ? "button_buy_hover" : "button_buy_normal", buyX, buyY, buyW, buyH, 276, 38);
        ShopTheme.text(
            graphics,
            this.priceLabel,
            buyX + 2,
            buyY + Math.max(1, (int)((buyH - 8.0F * s * 1.8F) / 2.0F)),
            buyW - 4,
            s * 1.8F,
            hover ? -660784 : -13096159,
            true
        );
        int ingSize = Math.max(8, Math.round(18.0F * s));
        int gap = Math.max(1, Math.round(2.0F * s));
        int maxPerRow = Math.max(1, (this.width - Math.round(16.0F * s)) / (ingSize + gap));
        int ingY0 = this.getY() + Math.round(168.0F * s);

        for (int i = 0; i < this.ingredients.size(); i++) {
            ItemStack stack = this.ingredients.get(i);
            int col = i % maxPerRow;
            int row = i / maxPerRow;
            int ix = this.getX() + Math.round(8.0F * s) + col * (ingSize + gap);
            int iy = ingY0 + row * (ingSize + gap);
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(ingSize / 16.0F, ingSize / 16.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.clickable) {
            this.onCraft.onCraftClick(event.buttonInfo().button(), Minecraft.getInstance().hasShiftDown());
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (this.clickable) {
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
