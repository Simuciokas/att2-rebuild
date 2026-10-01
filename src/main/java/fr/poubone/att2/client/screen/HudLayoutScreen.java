package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.hud.HudLayout;
import fr.poubone.att2.client.hud.HudRenderer;
import fr.poubone.att2.client.hud.HudSlot;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

public class HudLayoutScreen extends Screen {
    private static final int HANDLE = 8;
    private static final int MIN_W = 32;
    private static final int MIN_H = 16;
    private static final int SNAP = 2;
    private final Screen parent;
    private String selected = "xp";
    private String dragging;
    private boolean resizing;
    private int grabDx;
    private int grabDy;
    private int startW;
    private int startH;

    public HudLayoutScreen(Screen parent) {
        super(ModLanguageManager.get("screen.hud_layout.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int bw = 110;
        this.addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_layout.reset"), b -> {
            HudLayout.resetAll();
            HUDConfig.save();
        }).bounds(8, this.height - 28, bw, 20).build());
        this.addRenderableWidget(
            Button.builder(ModLanguageManager.get("screen.hud_layout.done"), b -> this.done()).bounds(this.width - bw - 8, this.height - 28, bw, 20).build()
        );
    }

    private void done() {
        HUDConfig.save();
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, 22, -1442840576);
        graphics.fill(0, this.height - 32, this.width, this.height, -1442840576);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HudRenderer.renderWidgets(graphics, true);

        for (String id : HudLayout.IDS) {
            this.drawBox(graphics, id, id.equals(this.selected));
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, ModLanguageManager.get("screen.hud_layout.hint"), this.width / 2, 7, -1518160);
        HudLayout.Box selectedBox = HudLayout.box(this.selected);
        HudSlot slot = HudLayout.slot(this.selected);
        float sc = slot.scale;
        if (sc <= 0.0F) {
            sc = 1.0F;
        }

        String size = selectedBox.w() + " x " + selectedBox.h() + " · ×" + String.format(Locale.ROOT, "%.1f", sc);
        if ("stats".equals(this.selected)) {
            float gap = slot.gap <= 0.0F ? 1.0F : slot.gap;
            size = size + " · " + ModLanguageManager.getString("screen.hud_layout.gap") + " ×" + String.format(Locale.ROOT, "%.1f", gap);
        }

        graphics.drawCenteredString(this.font, size, this.width / 2, this.height - 22, -3622752);
    }

    private void drawBox(GuiGraphics graphics, String id, boolean on) {
        HudLayout.Box box = HudLayout.box(id);
        int color = on ? -1521558 : -2000111456;
        int fill = on ? 870893674 : 570425344;
        graphics.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), fill);
        drawBorder(graphics, box.x(), box.y(), box.w(), box.h(), color);
        graphics.fill(box.x() + box.w() - 8, box.y() + box.h() - 8, box.x() + box.w(), box.y() + box.h(), color);
        String label = ModLanguageManager.getString(HudLayout.labelKey(id));
        int tw = this.font.width(label);
        int lx = box.x() + Math.max(2, (box.w() - tw) / 2);
        int ly = Math.max(box.y() - 10, 24);
        graphics.drawString(this.font, label, lx, ly, color, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() != 0) {
            return false;
        }

        int mx = (int)event.x();
        int my = (int)event.y();

        for (int i = HudLayout.IDS.length - 1; i >= 0; i--) {
            String id = HudLayout.IDS[i];
            HudLayout.Box box = HudLayout.box(id);
            if (contains(box, mx, my)) {
                this.selected = id;
                if (mx >= box.x() + box.w() - 8 && my >= box.y() + box.h() - 8) {
                    this.resizing = true;
                    this.dragging = id;
                    this.grabDx = mx;
                    this.grabDy = my;
                    this.startW = box.w();
                    this.startH = box.h();
                } else {
                    this.resizing = false;
                    this.dragging = id;
                    this.grabDx = mx - box.x();
                    this.grabDy = my - box.y();
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging == null) {
            return super.mouseDragged(event, dragX, dragY);
        }

        int mx = (int)event.x();
        int my = (int)event.y();
        HudLayout.Box box = HudLayout.box(this.dragging);
        if (this.resizing) {
            int w = snap(Mth.clamp(this.startW + (mx - this.grabDx), 32, this.width - box.x()));
            int h = snap(Mth.clamp(this.startH + (my - this.grabDy), 16, this.height - box.y()));
            HudLayout.writePixels(this.dragging, box.x(), box.y(), w, h);
        } else {
            int maxX = this.width - box.w();
            int maxY = this.height - box.h();
            int x = HudLayout.snapToEdge(Mth.clamp(mx - this.grabDx, 0, maxX), 0, maxX, 2);
            int y = HudLayout.snapToEdge(Mth.clamp(my - this.grabDy, 0, maxY), 0, maxY, 2);
            HudLayout.writePixels(this.dragging, x, y, box.w(), box.h());
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int mx = (int)mouseX;
        int my = (int)mouseY;
        String id = this.selected;
        HudLayout.Box box = HudLayout.box(id);
        if (!contains(box, mx, my)) {
            id = null;

            for (int i = HudLayout.IDS.length - 1; i >= 0; i--) {
                String candidate = HudLayout.IDS[i];
                if (contains(HudLayout.box(candidate), mx, my)) {
                    id = candidate;
                    this.selected = candidate;
                    break;
                }
            }

            if (id == null) {
                return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
            }
        }

        if (deltaY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        if ("stats".equals(id) && Minecraft.getInstance().hasShiftDown()) {
            float gapDelta = deltaY > 0.0 ? 0.1F : -0.1F;
            HudLayout.adjustGap(id, gapDelta);
        } else {
            float delta = deltaY > 0.0 ? 0.1F : -0.1F;
            HudLayout.adjustScale(id, delta);
        }

        HUDConfig.save();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging != null) {
            this.dragging = null;
            this.resizing = false;
            HUDConfig.save();
            return true;
        } else {
            return super.mouseReleased(event);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.done();
            return true;
        }

        int step = event.hasShiftDown() ? 8 : 2;
        HudLayout.Box box = HudLayout.box(this.selected);
        int x = box.x();
        int y = box.y();
        int w = box.w();
        int h = box.h();

        boolean moved = switch (event.key()) {
            case 262 -> {
                x = Mth.clamp(x + step, 0, this.width - w);
                yield true;
            }
            case 263 -> {
                x = Mth.clamp(x - step, 0, this.width - w);
                yield true;
            }
            case 264 -> {
                y = Mth.clamp(y + step, 0, this.height - h);
                yield true;
            }
            case 265 -> {
                y = Mth.clamp(y - step, 0, this.height - h);
                yield true;
            }
            default -> false;
        };
        if (moved) {
            HudLayout.writePixels(this.selected, x, y, w, h);
            HUDConfig.save();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    private static int snap(int value) {
        return value / 2 * 2;
    }

    private static boolean contains(HudLayout.Box box, int x, int y) {
        return x >= box.x() && x <= box.x() + box.w() && y >= box.y() && y <= box.y() + box.h();
    }
}
