package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopTooltips;
import fr.poubone.att2.client.shop.ShopType;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RuneRecipeRow extends AbstractWidget {
    public static final int SLOT = 32;
    public static final int GAP = 2;
    public static final int STOCK_W = 128;
    public static final int ROW_PAD = 2;
    public static final int STOCK_ICON = 16;
    private static final int PLUS_W = 8;
    private final RuneRecipeRow.Slot result;
    private final List<RuneRecipeRow.Slot> ingredients;
    private final List<Component> captionLines;
    private final List<RuneRecipeRow.StockLine> stockLines;
    private final boolean clickable;
    private final Runnable onCraft;
    private final ShopType theme;

    public RuneRecipeRow(
        int x,
        int y,
        int width,
        int height,
        RuneRecipeRow.Slot result,
        List<RuneRecipeRow.Slot> ingredients,
        List<Component> captionLines,
        List<RuneRecipeRow.StockLine> stockLines,
        boolean clickable,
        Runnable onCraft
    ) {
        this(x, y, width, height, result, ingredients, captionLines, stockLines, clickable, onCraft, ShopType.DAHAL);
    }

    public RuneRecipeRow(
        int x,
        int y,
        int width,
        int height,
        RuneRecipeRow.Slot result,
        List<RuneRecipeRow.Slot> ingredients,
        List<Component> captionLines,
        List<RuneRecipeRow.StockLine> stockLines,
        boolean clickable,
        Runnable onCraft,
        ShopType theme
    ) {
        super(x, y, width, height, result.stack().getHoverName());
        this.result = result;
        this.ingredients = ingredients;
        this.captionLines = captionLines;
        this.stockLines = stockLines;
        this.clickable = clickable;
        this.onCraft = onCraft;
        this.theme = theme;
    }

    public static int preferredHeight(int width, int ingredientCount, List<RuneRecipeRow.StockLine> stockLines) {
        int inner = Math.max(32, width - 128 - 32 - 2 - 10);
        int perLine = Math.max(1, inner / 42);
        int lines = ingredientCount == 0 ? 1 : Math.max(1, (int)Math.ceil((double)ingredientCount / perLine));
        int slotHeight = 4 + lines * 34 - 2;
        int stockHeight = stockLines.isEmpty() ? 0 : 18;
        int stockUsed = 0;

        for (RuneRecipeRow.StockLine line : stockLines) {
            int cellWidth = stockWidth(line);
            if (stockUsed + cellWidth > 128) {
                stockHeight += 18;
                stockUsed = 0;
            }

            stockUsed += cellWidth;
        }

        if (stockHeight > 0) {
            stockHeight += 4;
        }

        return Math.max(slotHeight, stockHeight);
    }

    public List<Component> tooltipAt(int mouseX, int mouseY) {
        int stockX = this.getX() + this.width - 128;
        int stockY = this.getY() + 2;

        for (RuneRecipeRow.StockLine line : this.stockLines) {
            int cellWidth = stockWidth(line);
            if (stockX + cellWidth > this.getX() + this.width) {
                stockX = this.getX() + this.width - 128;
                stockY += 18;
            }

            if (mouseX >= stockX && mouseX < stockX + cellWidth && mouseY >= stockY && mouseY < stockY + 16) {
                return List.of(stockName(line).copy().append(": ").append(line.text()));
            }

            stockX += cellWidth;
        }

        RuneRecipeRow.Slot hit = this.slotAt(mouseX, mouseY);
        if (hit == null) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        RuneItems.inventoryMatch(hit.stack()).ifPresentOrElse(matched -> lines.addAll(ShopTooltips.of(matched)), () -> lines.add(hit.stack().getHoverName()));
        lines.addAll(hit.extraTooltip());
        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ShopSkin.texture(
            graphics,
            this.theme,
            this.isHoveredOrFocused() && this.clickable ? "category_hover" : "category_normal",
            this.getX() - 3,
            this.getY(),
            this.width + 6,
            this.height,
            250,
            58
        );
        Font font = Minecraft.getInstance().font;
        int x = this.getX();
        int y = this.getY() + 2;
        this.drawSlot(graphics, x, y, this.result, this.contains(mouseX, mouseY, x, y));
        x += 34;
        graphics.drawString(font, "=", x, y + 12, -9413049, false);
        x += font.width("=") + 2;
        int wrapLeft = x;
        int wrapRight = this.getX() + this.width - 128 - 2;
        if (this.ingredients.isEmpty() && !this.captionLines.isEmpty()) {
            int cy = y + 12;

            for (Component line : this.captionLines) {
                ShopTheme.text(graphics, line, x, cy, wrapRight - x, 0.85F, -13096159, false);
                cy += 10;
            }
        } else {
            for (int i = 0; i < this.ingredients.size(); i++) {
                if (x + 32 > wrapRight && i > 0) {
                    x = wrapLeft;
                    y += 34;
                }

                if (i > 0) {
                    graphics.drawString(font, "+", x, y + 12, -9413049, false);
                    x += font.width("+") + 2;
                }

                RuneRecipeRow.Slot slot = this.ingredients.get(i);
                this.drawSlot(graphics, x, y, slot, this.contains(mouseX, mouseY, x, y));
                x += 34;
            }
        }

        int sx = this.getX() + this.width - 128;
        int sy = this.getY() + 2;

        for (RuneRecipeRow.StockLine line : this.stockLines) {
            int cellWidth = stockWidth(line);
            if (sx + cellWidth > this.getX() + this.width) {
                sx = this.getX() + this.width - 128;
                sy += 18;
            }

            if (!line.icon().isEmpty()) {
                graphics.renderItem(line.icon(), sx, sy);
                ShopTheme.text(graphics, line.text(), sx + 16 + 2, sy + 4, cellWidth - 20, 0.85F, -13096159, false);
            } else {
                ShopTheme.text(graphics, line.text(), sx, sy + 2, cellWidth - 2, 0.85F, -13096159, false);
            }

            sx += cellWidth;
        }
    }

    private static int stockWidth(RuneRecipeRow.StockLine line) {
        return line.icon().isEmpty() ? 128 : 64;
    }

    private static Component stockName(RuneRecipeRow.StockLine line) {
        if (line.icon().is(Items.CHEST)) {
            return Component.translatable("container.inventory");
        } else if (line.icon().is(Items.ENDER_EYE)) {
            return Component.translatable("att2.ui.rune_pouch");
        } else {
            return line.icon().isEmpty() ? Component.empty() : line.icon().getHoverName();
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, RuneRecipeRow.Slot slot, boolean hover) {
        ShopSkin.texture(graphics, this.theme, hover ? "card_hover" : "card_normal", x, y, 32, 32, 312, 252);
        graphics.renderItem(slot.stack(), x + 8, y + 8);
        graphics.renderItemDecorations(Minecraft.getInstance().font, slot.stack(), x + 8, y + 8);
    }

    private boolean contains(int mx, int my, int x, int y) {
        return mx >= x && my >= y && mx < x + 32 && my < y + 32;
    }

    private RuneRecipeRow.Slot slotAt(int mx, int my) {
        int x = this.getX();
        int y = this.getY() + 2;
        if (this.contains(mx, my, x, y)) {
            return this.result;
        }

        Font font = Minecraft.getInstance().font;
        x += 34 + font.width("=") + 2;
        int wrapLeft = x;
        int wrapRight = this.getX() + this.width - 128 - 2;

        for (int i = 0; i < this.ingredients.size(); i++) {
            if (x + 32 > wrapRight && i > 0) {
                x = wrapLeft;
                y += 34;
            }

            if (i > 0) {
                x += font.width("+") + 2;
            }

            if (this.contains(mx, my, x, y)) {
                return this.ingredients.get(i);
            }

            x += 34;
        }

        return null;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.clickable && !(event.x() >= this.getX() + this.width - 128)) {
            RuneRecipeRow.Slot hit = this.slotAt((int)event.x(), (int)event.y());
            if (hit == null || hit.craftClick()) {
                this.onCraft.run();
            }
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0;
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

    public record Slot(ItemStack stack, List<Component> extraTooltip, boolean craftClick) {
    }

    public record StockLine(ItemStack icon, Component text) {
        public static RuneRecipeRow.StockLine textOnly(Component text) {
            return new RuneRecipeRow.StockLine(ItemStack.EMPTY, text);
        }
    }
}
