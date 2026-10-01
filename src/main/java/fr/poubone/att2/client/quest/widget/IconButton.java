package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookTextures;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class IconButton extends QuestBookWidget {
    private final QuestBookTextures texture;
    private final boolean offsetIcon;
    private final Consumer<MouseButtonEvent> onPress;
    private final BooleanSupplier enabled;
    private final BooleanSupplier visibleSupplier;
    private final List<Component> tooltip;

    public static IconButton offset(int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress) {
        return new IconButton(x, y, width, height, texture, true, tooltip, event -> onPress.run(), null, () -> true);
    }

    public static IconButton offset(
        int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress, BooleanSupplier visible
    ) {
        return new IconButton(x, y, width, height, texture, true, tooltip, event -> onPress.run(), null, visible);
    }

    public static IconButton filter(
        int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress, BooleanSupplier enabled
    ) {
        return new IconButton(x, y, width, height, texture, false, tooltip, event -> onPress.run(), enabled, () -> true);
    }

    public static IconButton filter(
        int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Consumer<MouseButtonEvent> onPress, BooleanSupplier enabled
    ) {
        return new IconButton(x, y, width, height, texture, false, tooltip, onPress, enabled, () -> true);
    }

    private IconButton(
        int x,
        int y,
        int width,
        int height,
        QuestBookTextures texture,
        boolean offsetIcon,
        List<Component> tooltip,
        Consumer<MouseButtonEvent> onPress,
        BooleanSupplier enabled,
        BooleanSupplier visible
    ) {
        super(x, y, width, height, Component.literal("Icon Button"));
        this.texture = texture;
        this.offsetIcon = offsetIcon;
        this.tooltip = tooltip;
        this.onPress = onPress;
        this.enabled = enabled;
        this.visibleSupplier = visible;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.visible = this.visibleSupplier.getAsBoolean();
        if (this.visible) {
            if (this.offsetIcon) {
                this.texture.drawOffsetIcon(graphics, this.getX(), this.getY(), this.width, this.height, this.isHovered());
            } else {
                HUDConfig config = HUDConfig.get();
                int color = this.enabled != null && this.enabled.getAsBoolean()
                    ? HUDConfig.color(config.questBookFilterEnabledColor, -5974898)
                    : (this.isHovered() ? HUDConfig.color(config.questBookFilterHoverColor, -8817563) : HUDConfig.color(config.questBookFilterColor, -4870505));
                graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
                this.texture.draw(graphics, this.getX() + (this.width - this.texture.width()) / 2, this.getY() + (this.height - this.texture.height()) / 2);
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.visibleSupplier.getAsBoolean()) {
            this.onPress.accept(event);
        }
    }

    @Override
    public List<Component> getTooltipLines() {
        return this.visibleSupplier.getAsBoolean() ? this.tooltip : List.of();
    }
}
