package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.hud.ModToast;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class ShopPanelScreen extends Screen {
    protected final int panelWidth;
    protected final int panelHeight;
    private ShopViewport viewport = new ShopViewport(1.0F, 0.0F, 0.0F);

    protected ShopPanelScreen(Component title, int panelWidth, int panelHeight) {
        super(title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    protected final void initViewport() {
        this.viewport = ShopViewport.fit(this.width, this.height, this.panelWidth, this.panelHeight);
    }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fillGradient(0, 0, this.width, this.height, -1340596188, -620229616);
    }

    protected abstract void renderPanel(GuiGraphics var1, int var2, int var3, float var4);

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.viewport.left(), this.viewport.top());
        graphics.pose().scale(this.viewport.scale(), this.viewport.scale());
        this.renderPanel(graphics, (int)this.viewport.localX(mouseX), (int)this.viewport.localY(mouseY), partialTick);
        this.renderContent(graphics, (int)this.viewport.localX(mouseX), (int)this.viewport.localY(mouseY), partialTick);
        graphics.pose().popMatrix();
        ModToast.render(graphics);
    }

    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    protected final void showTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), this.viewport.screenX(mouseX), this.viewport.screenY(mouseY));
    }

    private MouseButtonEvent local(MouseButtonEvent event) {
        return new MouseButtonEvent(this.viewport.localX(event.x()), this.viewport.localY(event.y()), event.buttonInfo());
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.clickContent(this.local(event), doubleClick);
    }

    protected boolean clickContent(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(this.local(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(this.local(event), dx / this.viewport.scale(), dy / this.viewport.scale());
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(this.viewport.localX(x), this.viewport.localY(y));
    }

    protected final ShopActionButton chrome(
        int x, int y, int width, int height, Component label, ShopActionButton.Chrome style, ShopType theme, Runnable action
    ) {
        return this.addRenderableWidget(
            new ShopActionButton(
                x, y, width, height, new ShopAction(-1, label, null, ShopAction.Kind.OTHER), theme.titleColor, -6469, action, style, () -> false, theme
            )
        );
    }

    protected final void panel(GuiGraphics graphics, ShopType theme) {
        ShopSkin.texture(graphics, theme, "panel_main", 0, 0, this.panelWidth, this.panelHeight, 1440, 880);
        ShopIdentity.render(graphics, theme, this.panelWidth, this.panelHeight);
    }
}
