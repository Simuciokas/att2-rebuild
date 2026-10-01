package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.quest.QuestTextures;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public class QuestChromeButton extends QuestBookWidget {
    private final Identifier normal;
    private final Identifier hover;
    private final List<Component> tooltip;
    private final Consumer<MouseButtonEvent> onPress;
    private final BooleanSupplier lit;
    private final BooleanSupplier visibleSupplier;

    public static QuestChromeButton icon(int x, int y, int size, String iconName, List<Component> tooltip, Runnable onPress) {
        return new QuestChromeButton(x, y, size, iconName, tooltip, event -> onPress.run(), () -> true, () -> true);
    }

    public static QuestChromeButton filter(
        int x, int y, int size, String iconName, List<Component> tooltip, Consumer<MouseButtonEvent> onPress, BooleanSupplier enabled
    ) {
        return new QuestChromeButton(x, y, size, iconName, tooltip, onPress, enabled, () -> true);
    }

    private QuestChromeButton(
        int x, int y, int size, String iconName, List<Component> tooltip, Consumer<MouseButtonEvent> onPress, BooleanSupplier lit, BooleanSupplier visible
    ) {
        super(x, y, size, size, Component.literal(iconName));
        this.normal = QuestTextures.icon(iconName, false);
        this.hover = QuestTextures.icon(iconName, true);
        this.tooltip = tooltip;
        this.onPress = onPress;
        this.lit = lit;
        this.visibleSupplier = visible;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.visible = this.visibleSupplier.getAsBoolean();
        if (this.visible) {
            Identifier tex = this.isHoveredOrFocused() ? this.hover : this.normal;
            int src = 64;
            int tint = !this.lit.getAsBoolean() ? 1724428432 : -1;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, src, src, src, src, tint);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.visibleSupplier.getAsBoolean()) {
            this.onPress.accept(event);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public List<Component> getTooltipLines() {
        return this.visibleSupplier.getAsBoolean() ? this.tooltip : List.of();
    }
}
