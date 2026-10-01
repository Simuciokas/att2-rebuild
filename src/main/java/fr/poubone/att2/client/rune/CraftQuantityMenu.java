package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class CraftQuantityMenu {
    private static final int CELL_W = 36;
    private static final int CELL_H = 22;
    private static final int GAP = 4;
    private static final int PAD = 10;
    private static final int TITLE_H = 14;
    private final int panelW;
    private final int panelH;
    private int max;
    private int menuX;
    private int menuY;
    private IntConsumer onPick;
    private boolean open;
    private int hoverIndex = -1;

    public CraftQuantityMenu(int panelW, int panelH) {
        this.panelW = panelW;
        this.panelH = panelH;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void open(int max, int anchorX, int anchorY, IntConsumer onPick) {
        this.max = max;
        this.onPick = onPick;
        this.hoverIndex = -1;
        int totalW = this.rowWidth();
        int totalH = 60;
        int x = anchorX;
        int y = anchorY + 6;
        if (x + totalW > this.panelW) {
            x = this.panelW - totalW;
        }

        if (y + totalH > this.panelH) {
            y = this.panelH - totalH;
        }

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        this.menuX = x;
        this.menuY = y;
        this.open = true;
    }

    public void close() {
        this.open = false;
        this.onPick = null;
        this.hoverIndex = -1;
    }

    public boolean keyEscape() {
        if (!this.open) {
            return false;
        }

        this.close();
        return true;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (this.open && button == 0) {
            if (!this.containsMenu(mx, my)) {
                return false;
            }

            for (int i = 0; i < 5; i++) {
                if (this.cellHit(i, mx, my)) {
                    if (this.cellEnabled(i)) {
                        int preset = i < CraftQuantity.PRESETS.length ? CraftQuantity.PRESETS[i] : 0;
                        int qty = CraftQuantity.resolveChoice(preset, this.max);
                        if (qty > 0 && this.onPick != null) {
                            this.onPick.accept(qty);
                        }

                        this.close();
                    }

                    return true;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.open) {
            this.hoverIndex = -1;

            for (int i = 0; i < 5; i++) {
                if (this.cellEnabled(i) && this.cellHit(i, mouseX, mouseY)) {
                    this.hoverIndex = i;
                    break;
                }
            }

            int totalW = this.rowWidth();
            int totalH = 60;
            graphics.fill(this.menuX + 2, this.menuY + 3, this.menuX + totalW + 2, this.menuY + totalH + 3, 1342177280);
            QuestBookSkin.panel(graphics, "cover", this.menuX, this.menuY, totalW, totalH, 100, 100, 6);
            QuestBookSkin.panel(graphics, "page", this.menuX + 3, this.menuY + 3, totalW - 6, totalH - 6, 100, 100, 4);
            Component title = ModLanguageManager.get("rune_codex.qty.title");
            ShopTheme.text(graphics, title, this.menuX + 10, this.menuY + 10 - 1, totalW - 20, 1.0F, -13096159, true);

            for (int i = 0; i < 5; i++) {
                int cx = this.cellX(i);
                int cy = this.cellY();
                boolean enabled = this.cellEnabled(i);
                boolean hovered = i == this.hoverIndex;
                QuestBookSkin.button(graphics, cx, cy, 36, 22, false, hovered && enabled);
                if (!enabled) {
                    graphics.fill(cx + 2, cy + 2, cx + 36 - 2, cy + 22 - 2, -1997675832);
                }

                int color = enabled ? -13096159 : -9413049;
                ShopTheme.text(graphics, this.cellLabel(i), cx, cy + 6, 36, 1.0F, color, true);
            }
        }
    }

    private int rowWidth() {
        return 216;
    }

    private int cellX(int index) {
        return this.menuX + 10 + index * 40;
    }

    private int cellY() {
        return this.menuY + 10 + 14;
    }

    private boolean cellHit(int index, double mx, double my) {
        int cx = this.cellX(index);
        int cy = this.cellY();
        return mx >= cx && mx < cx + 36 && my >= cy && my < cy + 22;
    }

    private boolean containsMenu(double mx, double my) {
        int totalW = this.rowWidth();
        int totalH = 60;
        return mx >= this.menuX && mx < this.menuX + totalW && my >= this.menuY && my < this.menuY + totalH;
    }

    private boolean cellEnabled(int index) {
        return index < CraftQuantity.PRESETS.length ? CraftQuantity.presetEnabled(CraftQuantity.PRESETS[index], this.max) : this.max > 0;
    }

    private Component cellLabel(int index) {
        return index < CraftQuantity.PRESETS.length
            ? Component.literal(String.valueOf(CraftQuantity.PRESETS[index]))
            : ModLanguageManager.get("rune_codex.qty.all");
    }
}
