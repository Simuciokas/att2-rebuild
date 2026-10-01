package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.SpellLauncherTracker;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SpellBarDisplay {
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int PER_COLUMN = 8;

    public static void render(GuiGraphics ctx) {
        List<SpellLauncherTracker.LauncherState> states = SpellLauncherTracker.states();
        if (!states.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            HudLayout.Box box = HudLayout.box("spells");
            int columns = Math.max(1, (states.size() + 8 - 1) / 8);
            int rows = Math.min(8, states.size());
            float scale = HudLayout.contentScale("spells", columns * 20, rows * 20);
            int cell = Math.max(12, Math.round(18.0F * scale));
            int gap = Math.max(1, Math.round(2.0F * scale));
            int icon = Math.max(10, cell - 2);
            int stride = cell + gap;
            int screenW = Math.max(1, mc.getWindow().getGuiScaledWidth());
            int baseY = box.y() + box.h() - cell;

            for (int i = 0; i < states.size(); i++) {
                SpellLauncherTracker.LauncherState state = states.get(i);
                int row = i % 8;
                int col = i / 8;
                int x = SpellBarLayout.iconX(box.x(), box.w(), cell, screenW, col, stride);
                int y = baseY - row * stride;
                ctx.fill(x - 1, y - 1, x + cell - 1, y + cell - 1, 1711276032);
                ctx.pose().pushMatrix();
                ctx.pose().translate(x, y);
                float iconScale = icon / 16.0F;
                ctx.pose().scale(iconScale, iconScale);
                ctx.renderItem(state.stack, 0, 0);
                ctx.pose().popMatrix();
                if (!state.ready()) {
                    int coveredHeight = icon * state.cooldownTenths / 10;
                    ctx.fill(x, y + icon - coveredHeight, x + icon, y + icon, -1442840576);
                } else if (state.flashTicks > 0) {
                    int alpha = (int)(160.0F * state.flashTicks / 20.0F);
                    ctx.fill(x - 1, y - 1, x + cell - 1, y + cell - 1, alpha << 24 | 16766810);
                }

                if (state.level > 0) {
                    String level = String.valueOf(state.level);
                    float badge = Math.max(0.6F, 0.75F * scale);
                    ctx.pose().pushMatrix();
                    ctx.pose().translate(x + cell - 2 - mc.font.width(level) * badge, y + cell - 8);
                    ctx.pose().scale(badge, badge);
                    ctx.drawString(mc.font, level, 0, 0, -171, true);
                    ctx.pose().popMatrix();
                }
            }
        }
    }
}
