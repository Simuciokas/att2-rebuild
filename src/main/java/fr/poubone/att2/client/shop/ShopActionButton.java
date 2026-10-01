package fr.poubone.att2.client.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ShopActionButton extends AbstractWidget {
    private final ShopAction action;
    private final int idleColor;
    private final int hoverColor;
    private final Runnable onPress;
    private final ShopActionButton.Chrome chrome;
    private final BooleanSupplier selected;
    private final ShopType theme;

    public ShopActionButton(int x, int y, int width, int height, ShopAction action, int idleColor, int hoverColor, Runnable onPress) {
        this(x, y, width, height, action, idleColor, hoverColor, onPress, ShopActionButton.Chrome.ACTION, () -> false);
    }

    public ShopActionButton(
        int x,
        int y,
        int width,
        int height,
        ShopAction action,
        int idleColor,
        int hoverColor,
        Runnable onPress,
        ShopActionButton.Chrome chrome,
        BooleanSupplier selected
    ) {
        this(x, y, width, height, action, idleColor, hoverColor, onPress, chrome, selected, null);
    }

    public ShopActionButton(
        int x,
        int y,
        int width,
        int height,
        ShopAction action,
        int idleColor,
        int hoverColor,
        Runnable onPress,
        ShopActionButton.Chrome chrome,
        BooleanSupplier selected,
        ShopType theme
    ) {
        super(x, y, width, height, action.label());
        this.action = action;
        this.idleColor = idleColor;
        this.hoverColor = hoverColor;
        this.onPress = onPress;
        this.chrome = chrome;
        this.selected = selected;
        this.theme = theme;
    }

    public ShopAction action() {
        return this.action;
    }

    public List<Component> tooltipLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(this.action.label());
        if (this.action.tip() != null && !this.action.tip().getString().isBlank()) {
            lines.add(this.action.tip());
        }

        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.theme != null) {
            this.renderThemed(graphics);
        } else {
            boolean hover = this.isHovered();
            boolean on = this.selected.getAsBoolean();
            int color = !hover && !on ? this.idleColor : this.hoverColor;
            Font font = Minecraft.getInstance().font;
            String label = font.plainSubstrByWidth(this.action.label().getString(), this.width - 4);
            int tw = font.width(label);
            int tx = this.getX() + Math.max(1, (this.width - tw) / 2);
            int ty = this.getY() + Math.max(0, (this.height - 8) / 2);
            if (this.chrome == ShopActionButton.Chrome.NAV) {
                graphics.drawString(font, label, tx, ty, color, false);
            } else {
                if (hover && !on) {
                    graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 570425344);
                }

                graphics.drawString(font, label, tx, ty, color, false);
                if (on || hover) {
                    int lineY = this.getY() + this.height - 2;
                    int pad = Math.max(2, (this.width - tw) / 2);
                    graphics.fill(this.getX() + pad, lineY, this.getX() + this.width - pad, lineY + 1, color);
                    int mid = this.getX() + this.width / 2;
                    graphics.fill(mid - 1, lineY - 1, mid + 1, lineY + 2, color);
                }
            }
        }
    }

    private void renderThemed(GuiGraphics graphics) {
        String state = !this.active ? "disabled" : (this.selected.getAsBoolean() ? "selected" : (this.isHovered() ? "hover" : "normal"));

        String asset = switch (this.chrome) {
            case ACTION -> "button_reset_";
            case NAV -> "button_page_";
            case TAB -> "category_";
            case CLOSE -> "button_close_";
        };

        int sw = switch (this.chrome) {
            case ACTION -> 248;
            case NAV -> 34;
            case TAB -> 250;
            case CLOSE -> 42;
        };

        int sh = switch (this.chrome) {
            case ACTION -> 50;
            case NAV -> 34;
            case TAB -> 58;
            case CLOSE -> 42;
        };
        ShopSkin.texture(graphics, this.theme, asset + state, this.getX(), this.getY(), this.width, this.height, sw, sh);
        if (this.chrome != ShopActionButton.Chrome.CLOSE && this.chrome != ShopActionButton.Chrome.NAV) {
            float scale = Math.min(this.height / 16.0F, this.width / (this.chrome == ShopActionButton.Chrome.TAB ? 250.0F : 420.0F) * 2.7F);
            ShopTheme.text(
                graphics,
                this.action.label(),
                this.getX() + 3,
                this.getY() + Math.max(1, (int)((this.height - 8.0F * scale) / 2.0F)),
                this.width - 6,
                scale,
                this.active ? (this.selected.getAsBoolean() ? -660784 : -13096159) : -8947849,
                true
            );
        } else {
            String icon = this.chrome == ShopActionButton.Chrome.CLOSE
                ? "close_icon"
                : (this.action.label().getString().equals("‹") ? "arrow_left" : "arrow_right");
            int iconSize = Math.max(3, this.height * 2 / 5);
            ShopSkin.texture(
                graphics,
                this.theme,
                icon,
                this.getX() + (this.width - iconSize) / 2,
                this.getY() + (this.height - iconSize) / 2,
                iconSize,
                iconSize,
                this.chrome == ShopActionButton.Chrome.CLOSE ? 18 : 14,
                this.chrome == ShopActionButton.Chrome.CLOSE ? 18 : 14
            );
        }
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
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.85F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public enum Chrome {
        ACTION,
        NAV,
        TAB,
        CLOSE;
    }
}
