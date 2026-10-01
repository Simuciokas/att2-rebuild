package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.AttributeDialogParser;
import fr.poubone.att2.client.data.HeldWeaponStats;
import fr.poubone.att2.client.data.StatCaps;
import fr.poubone.att2.client.data.StatEffectText;
import fr.poubone.att2.client.data.StatManager;
import fr.poubone.att2.client.data.StatUpgradeModel;
import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import fr.poubone.att2.client.util.ModTextures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public class StatUpgradeScreen extends Screen {
    private static final int OUTER_MARGIN = 24;
    private static final int PANEL_MAX_W = 640;
    private static final int PANEL_MAX_H = 400;
    private static final int PAD = 12;
    private static final int HEADER_H = 14;
    private static final int COLS = 3;
    private static final int CARD_W = 188;
    private static final int CARD_H = 76;
    private static final int MIN_CARD_H = 48;
    private static final int GAP = 10;
    private static final int ICON_SIZE = 16;
    private static final int PLUS_SIZE = 20;
    private final List<StatUpgradeScreen.CardHit> cards = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int cardH = 76;
    private int gap = 10;
    private int layoutPad = 12;
    private int layoutHeaderGap = 6;
    private int cachedSkillPoints;
    private Button closeButton;

    public StatUpgradeScreen() {
        super(ModLanguageManager.get("screen.stat_upgrade.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.cards.clear();
        StatManager.updateAll();
        if (!StatUpgradeModel.isListening()) {
            StatUpgradeModel.open();
        }

        this.layoutPanel();
        int i = 0;

        for (String key : StatManager.STAT_KEYS) {
            Button plus = new StatUpgradeScreen.KenneyPlusButton(0, 0, 20, 20, btn -> {
                Integer trigger = Att2Triggers.STAT_UPGRADE.get(key);
                if (trigger != null) {
                    Att2Triggers.send(trigger);
                }
            });
            this.addRenderableWidget(plus);
            this.cards.add(new StatUpgradeScreen.CardHit(key, 0, 0, 188, this.cardH, plus));
            i++;
        }

        this.closeButton = Button.builder(ModLanguageManager.get("screen.stat_upgrade.close"), btn -> Minecraft.getInstance().setScreen(null))
            .bounds(0, 0, 80, 20)
            .build();
        this.addRenderableWidget(this.closeButton);
        this.syncCardWidgets();
    }

    private void layoutPanel() {
        this.panelW = Math.min(this.width - 24, 640);
        int availH = this.height - 24;
        int maxPreferred = Math.min(availH, 400);
        this.cardH = 76;
        this.gap = 10;
        this.layoutPad = 12;
        this.layoutHeaderGap = 6;
        int needed = this.contentHeight();
        this.panelH = Math.min(availH, Math.max(needed, maxPreferred));

        while (this.panelH < this.contentHeight() && this.gap > 4) {
            this.gap--;
            needed = this.contentHeight();
        }

        while (this.panelH < this.contentHeight() && this.layoutHeaderGap > 2) {
            this.layoutHeaderGap--;
            needed = this.contentHeight();
        }

        while (this.panelH < this.contentHeight() && this.layoutPad > 6) {
            this.layoutPad--;
            needed = this.contentHeight();
        }

        if (this.panelH < this.contentHeight()) {
            int rows = (StatManager.STAT_KEYS.size() + 3 - 1) / 3;
            int fixed = this.layoutPad + 14 + this.layoutHeaderGap + this.layoutPad + 20 + (rows - 1) * this.gap;
            int forCards = this.panelH - fixed;
            if (rows > 0 && forCards > 0) {
                this.cardH = Math.max(48, (forCards - (rows - 1) * this.gap) / rows);
            }
        }

        needed = this.contentHeight();
        if (needed <= availH) {
            this.panelH = Math.max(needed, Math.min(this.panelH, maxPreferred));
            this.panelH = Math.min(availH, this.panelH);
        }

        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
    }

    private int contentHeight() {
        int rows = (StatManager.STAT_KEYS.size() + 3 - 1) / 3;
        return this.layoutPad + 14 + this.layoutHeaderGap + rows * this.cardH + (rows - 1) * this.gap + this.layoutPad + 20;
    }

    private void syncCardWidgets() {
        if (!this.cards.isEmpty()) {
            int gridW = 564 + 2 * this.gap;
            int gridX = this.panelX + (this.panelW - gridW) / 2;
            int gridY = this.panelY + this.layoutPad + 14 + this.layoutHeaderGap;
            int plusMargin = scaleCard(this.cardH, 4);

            for (int i = 0; i < this.cards.size(); i++) {
                StatUpgradeScreen.CardHit card = this.cards.get(i);
                int col = i % 3;
                int row = i / 3;
                int cx = gridX + col * (188 + this.gap);
                int cy = gridY + row * (this.cardH + this.gap);
                this.cards.set(i, new StatUpgradeScreen.CardHit(card.key, cx, cy, 188, this.cardH, card.plus));
                card.plus.setPosition(cx + 188 - 20 - plusMargin, cy + this.cardH - 20 - plusMargin);
            }

            if (this.closeButton != null) {
                int closeY = this.panelY + this.panelH - this.layoutPad - 20;
                this.closeButton.setPosition(this.panelX + (this.panelW - 80) / 2, closeY);
            }
        }
    }

    private static int scaleCard(int cardHeight, int nominalAt72) {
        return StatUpgradeCardLayout.scaleCard(cardHeight, nominalAt72);
    }

    @Override
    public void removed() {
        StatUpgradeModel.close();
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        this.drawPanelChrome(context, this.cachedSkillPoints);

        for (StatUpgradeScreen.CardHit card : this.cards) {
            this.drawCard(context, card, this.cachedSkillPoints);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        StatManager.updateAll();
        this.layoutPanel();
        this.syncCardWidgets();
        this.cachedSkillPoints = StatUpgradeModel.skillPoints();

        for (StatUpgradeScreen.CardHit card : this.cards) {
            this.updatePlusState(card, this.cachedSkillPoints);
        }

        super.render(context, mouseX, mouseY, delta);
        StatUpgradeScreen.CardHit hovered = null;

        for (StatUpgradeScreen.CardHit card : this.cards) {
            if (mouseX >= card.x && mouseX < card.x + card.w && mouseY >= card.y && mouseY < card.y + card.h) {
                hovered = card;
            }
        }

        if (hovered != null) {
            String key = hovered.key;
            int tot = StatManager.total(key);
            int base = StatUpgradeModel.base(key).orElse(tot);
            StatEffectText.WeaponStats weapon = HeldWeaponStats.of(Minecraft.getInstance().player);
            Component currentComp = wrapEffectLine("screen.stat_upgrade.effect.current", StatEffectText.current(key, tot, weapon));
            Component nextComp = StatEffectText.next(key, tot, base, weapon)
                .map(v -> wrapEffectLine("screen.stat_upgrade.effect.next", v))
                .orElse(ModLanguageManager.get("screen.stat_upgrade.maxed"));
            Component costComp = null;
            OptionalInt costOpt = StatUpgradeModel.upgradeCost(key);
            int max = StatCaps.maxBase(key);
            if (costOpt.isPresent() && costOpt.getAsInt() > 0 && base < max) {
                costComp = formatLang("screen.stat_upgrade.cost", costOpt.getAsInt());
            }

            StatHoverTooltip.render(context, this.width, this.height, mouseX, mouseY, currentComp, nextComp, costComp, sourceLines(key));
        }
    }

    private static List<Component> sourceLines(String key) {
        Map<String, Integer> sources = StatUpgradeModel.sources(key);
        if (sources.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();

        for (String suffix : AttributeDialogParser.SOURCE_SUFFIXES) {
            Integer value = sources.get(suffix);
            if (value != null) {
                lines.add(formatLang("screen.stat_upgrade.source." + suffix, value));
            }
        }

        return lines;
    }

    private void drawPanelChrome(GuiGraphics context, int skillPoints) {
        context.fill(this.panelX + 3, this.panelY + 4, this.panelX + this.panelW + 3, this.panelY + this.panelH + 4, 1342177280);
        QuestBookSkin.panel(context, "cover", this.panelX, this.panelY, this.panelW, this.panelH, 100, 100, 6);
        QuestBookSkin.panel(context, "page", this.panelX + 3, this.panelY + 3, this.panelW - 6, this.panelH - 6, 100, 100, 4);
        Component header = Component.literal(ModLanguageManager.format("screen.stat_upgrade.skill_points", "points", skillPoints));
        ShopTheme.text(context, header, this.panelX, this.panelY + this.layoutPad, this.panelW, 1.0F, -13096159, true);
    }

    private void drawCard(GuiGraphics context, StatUpgradeScreen.CardHit card, int skillPoints) {
        String key = card.key;
        int tot = StatManager.total(key);
        int base = StatUpgradeModel.base(key).orElse(tot);
        int max = StatCaps.maxBase(key);
        OptionalInt costOpt = StatUpgradeModel.upgradeCost(key);
        QuestBookSkin.panel(context, "page", card.x, card.y, card.w, card.h, 100, 100, 4);
        StatUpgradeCardLayout.Interior layout = StatUpgradeCardLayout.layout(card.h, tot != base);
        int iconX = card.x + layout.pad();
        int iconY = card.y + layout.pad();
        if (key.equals("DAR")) {
            context.pose().pushMatrix();
            context.pose().translate(iconX, iconY);
            context.renderItem(Items.BOOK.getDefaultInstance(), 0, 0);
            context.pose().popMatrix();
        } else {
            Identifier icon = ModTextures.STAT_ICONS.get(key);
            if (icon != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }

        int textX = card.x + layout.pad() + 16 + 4;
        int innerW = card.w - (textX - card.x) - 20 - layout.pad() - 2;
        Component name = ModLanguageManager.get("stat.label." + key.toLowerCase());
        ShopTheme.text(context, name, textX, card.y + layout.pad(), innerW, 1.0F, -13096159, false);
        Component baseLine = formatLang("screen.stat_upgrade.level", base, max);
        ShopTheme.text(context, baseLine, card.x + layout.pad(), card.y + layout.levelY(), card.w - layout.pad() * 2, 1.0F, -13096159, false);
        if (layout.showTot()) {
            Component totLine = formatLang("screen.stat_upgrade.tot", tot);
            ShopTheme.text(context, totLine, card.x + layout.pad(), card.y + layout.totY(), card.w - layout.pad() * 2, 1.0F, -9413049, false);
        }

        Component costLine;
        if (costOpt.isEmpty()) {
            costLine = ModLanguageManager.get("screen.stat_upgrade.cost_pending");
        } else if (costOpt.getAsInt() != 0 && base < max) {
            costLine = formatLang("screen.stat_upgrade.cost", costOpt.getAsInt());
        } else {
            costLine = ModLanguageManager.get("screen.stat_upgrade.maxed");
        }

        ShopTheme.text(
            context, costLine, card.x + layout.pad(), card.y + layout.costY(), card.w - 20 - layout.pad() - layout.plusMargin(), 1.0F, -9413049, false
        );
    }

    private void updatePlusState(StatUpgradeScreen.CardHit card, int skillPoints) {
        int base = StatUpgradeModel.base(card.key).orElse(StatManager.total(card.key));
        int max = StatCaps.maxBase(card.key);
        OptionalInt costOpt = StatUpgradeModel.upgradeCost(card.key);
        boolean canUpgrade = StatUpgradeModel.hasData() && costOpt.isPresent() && costOpt.getAsInt() > 0 && skillPoints >= costOpt.getAsInt() && base < max;
        card.plus.active = canUpgrade;
    }

    private static Component wrapEffectLine(String wrapperKey, StatEffectText.EffectView v) {
        return formatLang(wrapperKey, formatEffect(v));
    }

    private static String formatEffect(StatEffectText.EffectView v) {
        return formatLangString(v.langKey(), v.args());
    }

    private static Component formatLang(String key, Object... args) {
        return Component.literal(formatLangString(key, args));
    }

    private static String formatLangString(String key, Object... args) {
        String value = ModLanguageManager.getString(key);

        for (int i = 0; i < args.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return value;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CardHit(String key, int x, int y, int w, int h, Button plus) {
    }

    private static final class KenneyPlusButton extends Button {
        KenneyPlusButton(int x, int y, int w, int h, OnPress onPress) {
            super(x, y, w, h, Component.literal("+"), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
            QuestBookSkin.button(g, this.getX(), this.getY(), this.width, this.height, false, this.active && this.isHoveredOrFocused());
            int color = this.active ? -13096159 : -9413049;
            Font font = Minecraft.getInstance().font;
            g.drawString(
                font, this.getMessage(), this.getX() + (this.width - font.width(this.getMessage())) / 2, this.getY() + (this.height - 9) / 2, color, false
            );
            if (!this.active) {
                g.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 1894638280);
            }
        }
    }
}
