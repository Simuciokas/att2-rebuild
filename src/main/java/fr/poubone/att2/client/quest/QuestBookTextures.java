package fr.poubone.att2.client.quest;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum QuestBookTextures {
    BACKGROUND("content_book.png", 339, 220),
    TITLE("content_book_title.png", 168, 33),
    SEARCH("content_book_search.png", 133, 23),
    BACK_ARROW("back_arrow_icon_offset.png", 32, 9),
    BACKWARD_ARROW("backward_arrow_icon_offset.png", 36, 10),
    FORWARD_ARROW("forward_arrow_icon_offset.png", 36, 10),
    RELOAD("reload_icon_offset.png", 40, 20),
    QUESTS_SCROLL("quests_scroll_icon.png", 16, 16),
    SIGN("sign_icon.png", 17, 18),
    DIALOGUE("dialogue_icon.png", 14, 11),
    MAIN_QUEST("main_quest_icon.png", 18, 18),
    ACTIVITY_CAN_START("activity_can_start_icon.png", 11, 7),
    ACTIVITY_CANNOT_START("activity_cannot_start_icon.png", 7, 7),
    ACTIVITY_FINISHED("activity_finished_icon.png", 11, 7),
    ACTIVITY_STARTED("activity_started_icon.png", 7, 7);

    private static final Identifier BOOK = Identifier.withDefaultNamespace("textures/gui/book.png");
    private final String file;
    private final int width;
    private final int height;

    QuestBookTextures(String file, int width, int height) {
        this.file = file;
        this.width = width;
        this.height = height;
    }

    private static String theme() {
        String theme = HUDConfig.get().questBookTheme;
        return theme != null && !theme.isBlank() && !theme.equals("wynntils") && theme.matches("[a-z0-9_-]+") ? theme : "minecraft";
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        if (!theme().equals("minecraft")) {
            this.drawCustom(graphics, x, y, this.width, this.height, false, false);
        } else {
            switch (this) {
                case BACKGROUND:
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x + this.width / 2.0F, y);
                    graphics.pose().scale(-1.0F, 1.0F);
                    page(graphics, 0, 0, this.width / 2 + 1, this.height);
                    graphics.pose().popMatrix();
                    page(graphics, x + this.width / 2, y, this.width - this.width / 2, this.height);
                    break;
                case TITLE:
                    graphics.fill(x + 6, y, x + this.width - 6, y + this.height, -10927827);
                    break;
                case SEARCH:
                    graphics.fill(x, y + 3, x + this.width, y + this.height, -9414596);
                    graphics.fill(x + 1, y + 4, x + this.width - 1, y + this.height - 1, -793153);
                    sprite(graphics, "icon/search", x + 3, y + 8, 8, 8);
                    break;
                default:
                    this.drawItem(graphics, x, y, this.width, this.height);
            }
        }
    }

    private static void page(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK, x, y, 20.0F, 0.0F, w, h, 146, 180, 256, 256);
    }

    private void drawItem(GuiGraphics graphics, int x, int y, int w, int h) {
        Item item = switch (this) {
            case RELOAD -> Items.COMPASS;
            case QUESTS_SCROLL, ACTIVITY_CAN_START -> Items.PAPER;
            case SIGN, ACTIVITY_FINISHED -> Items.EMERALD;
            case DIALOGUE -> Items.CLOCK;
            case MAIN_QUEST -> Items.WRITTEN_BOOK;
            case ACTIVITY_CANNOT_START -> Items.BARRIER;
            default -> Items.FEATHER;
        };
        int size = Math.min(w, h);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + (w - size) / 2.0F, y + (h - size) / 2.0F);
        graphics.pose().scale(size / 16.0F, size / 16.0F);
        graphics.renderItem(new ItemStack(item), 0, 0);
        graphics.pose().popMatrix();
    }

    public void drawOffsetIcon(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
        if (!theme().equals("minecraft")) {
            this.drawCustom(graphics, x, y, w, h, true, hovered);
        } else if (this == RELOAD) {
            if (hovered) {
                graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 1433425980);
            }

            this.drawItem(graphics, x, y, w, h);
        } else {
            String sprite = this == BACK_ARROW ? "widget/cross_button" : (this == FORWARD_ARROW ? "widget/page_forward" : "widget/page_backward");
            sprite(graphics, sprite + (hovered ? "_highlighted" : ""), x, y, w, h);
        }
    }

    private static void sprite(GuiGraphics graphics, String path, int x, int y, int w, int h) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(path), x, y, w, h);
    }

    private void drawCustom(GuiGraphics graphics, int x, int y, int w, int h, boolean offset, boolean hovered) {
        Identifier resource = Identifier.fromNamespaceAndPath("att2", "textures/quest_book/" + theme() + "/" + this.file);
        int regionWidth = offset ? this.width / 2 : this.width;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, resource, x, y, offset && hovered ? regionWidth : 0.0F, 0.0F, w, h, regionWidth, this.height, this.width, this.height
        );
    }
}
