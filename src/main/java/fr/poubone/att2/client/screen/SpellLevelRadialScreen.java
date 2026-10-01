package fr.poubone.att2.client.screen;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import fr.poubone.att2.client.data.GrimoireXp;
import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellLevelSelector;
import fr.poubone.att2.client.data.SpellSelectTriggers;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SpellLevelRadialScreen extends Screen {
    private static final int RADIUS = 70;
    private final SpellLauncherTracker.LauncherState launcher;
    private final int levelCount;
    private int selectedOption = -1;

    public SpellLevelRadialScreen(SpellLauncherTracker.LauncherState launcher) {
        super(Component.translatable("att2.spell" + launcher.spellId + ".name"));
        this.launcher = launcher;
        this.levelCount = SpellSelectTriggers.levelCount(launcher.spellId);
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
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double fromTop = (Math.atan2(dy, dx) + (Math.PI / 2) + (Math.PI * 2)) % (Math.PI * 2);
        this.selectedOption = distance < 28.0 ? -1 : (int)(Math.round(fromTop / (Math.PI * 2) * this.levelCount) % this.levelCount);

        for (int i = 0; i < this.levelCount; i++) {
            double theta = (Math.PI * 2) / this.levelCount * i - (Math.PI / 2);
            int tx = (int)(cx + Math.cos(theta) * 70.0);
            int ty = (int)(cy + Math.sin(theta) * 70.0);
            int level = i + 1;
            boolean locked = SpellLevelSelector.isLocked(this.launcher.spellId, level);
            int color = level == this.launcher.level ? -10496 : (locked ? -11184811 : -1);
            context.pose().pushMatrix();
            context.pose().translate(tx, ty - 4);
            float scale = i == this.selectedOption ? 1.4F : 1.0F;
            context.pose().scale(scale, scale);
            context.drawCenteredString(this.font, String.valueOf(level), 0, 0, color);
            context.pose().popMatrix();
            if (i == this.selectedOption && locked) {
                context.setTooltipForNextFrame(this.font, ModLanguageManager.get("spell_radial.locked"), mouseX, mouseY);
            }
        }

        context.drawCenteredString(this.font, this.title, cx, cy - 20, -1);
        context.drawCenteredString(this.font, ModLanguageManager.format("spell_radial.level", "n", this.launcher.level), cx, cy - 6, -5592406);
        GrimoireXp.XpState xp = this.xp();
        if (xp != null) {
            context.drawCenteredString(this.font, xp.xp() + "/" + xp.nextThreshold(), cx, cy + 8, -11141291);
        } else {
            context.drawCenteredString(this.font, ModLanguageManager.get("spell_radial.xp_hint"), cx, cy + 8, -7829368);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            this.validate();
            Minecraft.getInstance().setScreen(null);
            KeybindManager.blockSpellLevelUntilRelease();
            return true;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
    }

    @Override
    public void tick() {
        if (!this.isMenuKeyHeld()) {
            this.onClose();
            this.validate();
        }
    }

    private GrimoireXp.XpState xp() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? null : GrimoireXp.read(client.player, this.launcher.spellId);
    }

    private void validate() {
        if (this.selectedOption >= 0) {
            int level = this.selectedOption + 1;
            if (level != this.launcher.level) {
                SpellLevelSelector.select(this.launcher.spellId, level);
            }
        }
    }

    private boolean isMenuKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        Key key = KeyBindingHelper.getBoundKeyOf(KeybindManager.getSpellLevelMenuKey());
        long window = client.getWindow().handle();
        return key.getType() == Type.MOUSE ? GLFW.glfwGetMouseButton(window, key.getValue()) == 1 : GLFW.glfwGetKey(window, key.getValue()) == 1;
    }
}
