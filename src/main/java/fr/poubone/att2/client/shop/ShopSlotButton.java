package fr.poubone.att2.client.shop;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class ShopSlotButton extends AbstractWidget {
    private final ShopOffer offer;
    private final Runnable onPress;
    private final ShopType theme;

    public ShopSlotButton(int x, int y, int width, int height, ShopOffer offer, Runnable onPress) {
        this(x, y, width, height, offer, onPress, ShopType.GENERAL);
    }

    public ShopSlotButton(int x, int y, int width, int height, ShopOffer offer, Runnable onPress, ShopType theme) {
        super(x, y, width, height, offer.name());
        this.offer = offer;
        this.onPress = onPress;
        this.theme = theme;
    }

    public ShopOffer offer() {
        return this.offer;
    }

    public List<Component> tooltipLines() {
        return ShopTooltips.forOffer(this.offer, this.displayName());
    }

    private Component displayName() {
        return this.offer.name().getContents() instanceof TranslatableContents
            ? ShopIcons.stripInlineObjects(this.offer.name().plainCopy().withStyle(this.offer.name().getStyle()))
            : ShopIcons.stripInlineObjects(this.offer.name());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = this.isHovered();
        ShopSkin.texture(graphics, this.theme, hover ? "card_hover" : "card_normal", this.getX(), this.getY(), this.width, this.height, 312, 252);
        float s = this.width / 312.0F;
        int iconSize = Math.max(1, Math.round(90.0F * s));
        int iconX = this.getX() + (this.width - iconSize) / 2;
        int iconY = this.getY() + Math.round(38.0F * s);
        int spellId = ShopIcons.spellId(this.offer);
        Identifier spriteTex = spellId <= 0 && ShopIcons.isPackTexture(this.offer.sprite()) ? ShopModel.spriteTexture(this.offer.sprite()) : null;
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(iconSize / 16.0F, iconSize / 16.0F);
        if (spriteTex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, spriteTex, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            graphics.renderItem(this.offer.stack(), 0, 0);
            graphics.renderItemDecorations(Minecraft.getInstance().font, this.offer.stack(), 0, 0);
        }

        graphics.pose().popMatrix();
        if (spellId > 0 && !ShopIcons.isLauncher(this.offer.stack())) {
            ItemStack launcher = ShopIcons.launcher(spellId);
            int badge = Math.max(8, Math.round(iconSize * 0.42F));
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX + iconSize - badge + Math.round(2.0F * s), iconY + iconSize - badge + Math.round(2.0F * s));
            graphics.pose().scale(badge / 16.0F, badge / 16.0F);
            graphics.renderItem(launcher, 0, 0);
            graphics.pose().popMatrix();
        }

        float textScale = s * 2.7F;
        ShopTheme.text(
            graphics,
            this.displayName(),
            this.getX() + Math.round(14.0F * s),
            this.getY() + Math.round(141.0F * s),
            this.width - Math.round(28.0F * s),
            textScale,
            -13096159,
            true
        );
        ShopTheme.text(
            graphics,
            Component.translatable("att2.shop.hover_event.buy"),
            this.getX() + Math.round(14.0F * s),
            this.getY() + Math.round(175.0F * s),
            this.width - Math.round(28.0F * s),
            s * 1.8F,
            -9413049,
            true
        );
        int buyX = this.getX() + Math.round(18.0F * s);
        int buyY = this.getY() + Math.round(202.0F * s);
        int buyW = Math.round(276.0F * s);
        int buyH = Math.round(38.0F * s);
        ShopSkin.texture(graphics, this.theme, hover ? "button_buy_hover" : "button_buy_normal", buyX, buyY, buyW, buyH, 276, 38);
        Component price = this.offer.price() == null ? Component.literal("?") : this.offer.price();
        float priceScale = s * 2.3F;
        ShopTheme.text(
            graphics, price, buyX + 2, buyY + Math.max(1, (int)((buyH - 8.0F * priceScale) / 2.0F)), buyW - 4, priceScale, hover ? -660784 : -13096159, true
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onPress.run();
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
