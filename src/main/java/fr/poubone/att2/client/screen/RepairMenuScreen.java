package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class RepairMenuScreen extends Screen {
    private static final int OPTION_COUNT = 7;
    private int selectedOption = -1;

    public RepairMenuScreen() {
        super(ModLanguageManager.get("screen.repair.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int radius = 70;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);
        angle = (angle + (Math.PI * 2)) % (Math.PI * 2);
        double OFFSET = 1.7;
        this.selectedOption = distance < radius / 2.5 ? -1 : (int)((angle / (Math.PI * 2) * 7.0 + 1.7) % 7.0);
        ItemStack[] itemIcons = this.getRepairOptionIcons();

        for (int i = 0; i < 7; i++) {
            double theta = (Math.PI * 2.0 / 7.0) * i - (Math.PI / 3);
            int tx = (int)(cx + Math.cos(theta) * radius);
            int ty = (int)(cy + Math.sin(theta) * radius);
            ItemStack stack = itemIcons[i];
            context.pose().pushMatrix();
            context.pose().translate(tx - 8, ty - 8);
            float scale = i == this.selectedOption ? 1.2F : 1.0F;
            context.pose().scale(scale, scale);
            if (i == 6) {
                this.drawCompositeArmorIcon(context);
            } else {
                context.renderItem(stack, 0, 0);
            }

            context.pose().popMatrix();
            if (i == this.selectedOption) {
                context.setTooltipForNextFrame(this.font, ModLanguageManager.get("screen.repair.option." + i), mouseX, mouseY);
            }
        }

        context.drawCenteredString(this.font, ModLanguageManager.get("screen.repair.title"), cx, cy - 8, -1);
    }

    private void drawCompositeArmorIcon(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ItemStack helmet = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.HEAD));
            ItemStack chestplate = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.CHEST));
            ItemStack leggings = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.LEGS));
            ItemStack boots = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.FEET));
            context.renderItem(helmet, -6, -6);
            context.renderItem(chestplate, 2, -6);
            context.renderItem(leggings, -6, 2);
            context.renderItem(boots, 2, 2);
        }
    }

    private ItemStack[] getRepairOptionIcons() {
        Minecraft client = Minecraft.getInstance();
        ItemStack[] icons = new ItemStack[7];
        if (client.player != null) {
            icons[0] = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.HEAD));
            icons[1] = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.CHEST));
            icons[2] = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.LEGS));
            icons[3] = this.getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.FEET));
            icons[4] = this.getItemOrBarrier(client.player.getOffhandItem());
        }

        icons[5] = new ItemStack(Items.ANVIL);
        icons[6] = new ItemStack(Items.DIAMOND_HELMET);
        return icons;
    }

    private ItemStack getItemOrBarrier(ItemStack stack) {
        return stack != null && !stack.isEmpty() ? new ItemStack(stack.getItem()) : new ItemStack(Items.BARRIER);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            this.executeOption(this.selectedOption);
            Minecraft.getInstance().setScreen(null);
            KeybindManager.blockRepairUntilRelease();
            return true;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
    }

    @Override
    public void tick() {
        if (!this.isRepairKeyHeld()) {
            this.onClose();
            this.executeOption(this.selectedOption);
        }
    }

    private boolean isRepairKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        int keyCode = KeyBindingHelper.getBoundKeyOf(KeybindManager.getRepairItemMenuKey()).getValue();
        return GLFW.glfwGetKey(client.getWindow().handle(), keyCode) == 1;
    }

    private void executeOption(int index) {
        if (index >= 0) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                switch (index) {
                    case 0:
                        Att2Triggers.send(416);
                        break;
                    case 1:
                        Att2Triggers.send(417);
                        break;
                    case 2:
                        Att2Triggers.send(418);
                        break;
                    case 3:
                        Att2Triggers.send(419);
                        break;
                    case 4:
                        Att2Triggers.send(420);
                        break;
                    case 5:
                        Att2Triggers.send(3607);
                        break;
                    case 6:
                        Att2Triggers.send(422);
                }
            }
        }
    }
}
