package fr.poubone.att2.client.quest;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

public final class QuestModel {
    private static final QuestModel INSTANCE = new QuestModel();
    private static final String SIDEQUEST_TITLE_KEY = "consciousness.sidequest.title";
    private static final Pattern SIDEQUEST_NAME_KEY = Pattern.compile("consciousness\\.sidequest\\.(\\d+)");
    private static final String MAINQUEST_PROGRESS_KEY = "att2.mainquest.progress";
    private static final String DAILYQUEST_LIST_TITLE_KEY = "consciousness.dailyquest.title";
    private static final Pattern DAILYQUEST_CITY_KEY = Pattern.compile("consciousness\\.dailyquest\\.list\\.([a-z_]+)");
    private static final Pattern DAILYQUEST_CITY_TITLE_KEY = Pattern.compile("consciousness\\.dailyquest\\.([a-z_]+)\\.title");
    private static final Pattern DAILYQUEST_NAME_KEY = Pattern.compile("att2\\.dailyquest\\.([a-z_]+)\\.(\\d+)\\.name");
    private static final String DAILYQUEST_REMAINING_KEY = "att2.dailyquest.remaining_time";
    private static final int CITY_REQUEST_INTERVAL_TICKS = 8;
    private static final int MAIN_QUEST_MAX_STEP = 300;
    private static final int DIALOG_TIMEOUT_TICKS = 80;
    private static final int CAPTURE_TICKS = 30;
    private final List<QuestInfo> sideQuests = new ArrayList<>();
    private final Map<String, List<QuestInfo>> dailyQuests = new LinkedHashMap<>();
    private final Map<String, List<Component>> descriptions = new HashMap<>();
    private int sideCompleted = 0;
    private int sideTotal = 60;
    private int mainStep = -1;
    private long lastSideQuestUpdate = 0L;
    private int dialogWaitTicks = 0;
    private int dailyDialogWaitTicks = 0;
    private final ArrayDeque<Integer> pendingCityTriggers = new ArrayDeque<>();
    private int cityRequestCooldown = 0;
    private boolean hasDailyData = false;
    private int captureTicks = 0;
    private String captureKey = null;
    private final List<Component> captureBuffer = new ArrayList<>();
    private String selectedKey = null;

    private QuestModel() {
    }

    public static QuestModel get() {
        return INSTANCE;
    }

    public void reset() {
        this.sideQuests.clear();
        this.dailyQuests.clear();
        this.descriptions.clear();
        this.sideCompleted = 0;
        this.mainStep = -1;
        this.lastSideQuestUpdate = 0L;
        this.dialogWaitTicks = 0;
        this.dailyDialogWaitTicks = 0;
        this.pendingCityTriggers.clear();
        this.cityRequestCooldown = 0;
        this.hasDailyData = false;
        this.captureTicks = 0;
        this.captureKey = null;
        this.captureBuffer.clear();
        this.selectedKey = null;
    }

    public void requestSideQuests() {
        this.dialogWaitTicks = 80;
        Att2Triggers.send(2315);
    }

    public void requestDailyQuests() {
        this.dailyDialogWaitTicks = 80;
        this.pendingCityTriggers.clear();
        Att2Triggers.send(3280);
    }

    public void requestDetails(QuestInfo quest) {
        if (quest.isMain()) {
            this.beginCapture(quest.key());
            Att2Triggers.send(1068);
        } else if (quest.hasDetails()) {
            this.beginCapture(quest.key());
            Att2Triggers.send(quest.progressTrigger());
        }
    }

    private void beginCapture(String key) {
        this.captureKey = key;
        this.captureTicks = 30;
        this.captureBuffer.clear();
    }

    public void tick(Minecraft client) {
        if (this.dialogWaitTicks > 0) {
            this.dialogWaitTicks--;
        }

        if (this.dailyDialogWaitTicks > 0) {
            this.dailyDialogWaitTicks--;
        }

        if (this.cityRequestCooldown > 0) {
            this.cityRequestCooldown--;
        }

        if (this.cityRequestCooldown == 0 && !this.pendingCityTriggers.isEmpty() && this.dailyDialogWaitTicks == 0) {
            this.dailyDialogWaitTicks = 80;
            this.cityRequestCooldown = 8;
            Att2Triggers.send(this.pendingCityTriggers.pollFirst());
        }

        if (this.captureTicks > 0) {
            this.captureTicks--;
            if (this.captureTicks == 0) {
                this.finishCapture();
            }
        }
    }

    private void finishCapture() {
        if (this.captureKey != null && !this.captureBuffer.isEmpty()) {
            this.descriptions.put(this.captureKey, new ArrayList<>(this.captureBuffer));
        }

        this.captureKey = null;
        this.captureBuffer.clear();
    }

    public boolean onDialog(Holder<Dialog> holder) {
        if (!HUDConfig.get().questMenuEnabled) {
            return false;
        } else {
            Dialog dialog = holder.value();
            boolean requested = this.dialogWaitTicks > 0;
            if (dialog instanceof NoticeDialog notice && containsKey(notice.common().title(), "consciousness.sidequest.title")) {
                this.sideQuests.clear();
                this.sideCompleted = 0;
                this.lastSideQuestUpdate = System.currentTimeMillis();
                this.dialogWaitTicks = 0;
                return requested;
            } else if (dialog instanceof MultiActionDialog multi) {
                if (bodyContainsKey(multi.common(), "consciousness.sidequest.title")) {
                    this.parseSideQuestDialog(multi);
                    this.dialogWaitTicks = 0;
                    return requested;
                } else {
                    boolean dailyRequested = this.dailyDialogWaitTicks > 0;
                    if (bodyContainsKey(multi.common(), "consciousness.dailyquest.title")) {
                        this.parseDailyCityList(multi);
                        this.dailyDialogWaitTicks = 0;
                        return dailyRequested;
                    } else {
                        String city = findDailyCityTitle(multi.common());
                        if (city != null) {
                            this.parseDailyCityDialog(city, multi);
                            this.dailyDialogWaitTicks = 0;
                            return dailyRequested;
                        } else {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    private void parseDailyCityList(MultiActionDialog dialog) {
        this.pendingCityTriggers.clear();
        List<String> listedCities = new ArrayList<>();

        for (ActionButton button : dialog.actions()) {
            String city = findKeyGroup(button.button().label(), DAILYQUEST_CITY_KEY, 1);
            int trigger = triggerOf(button);
            if (city != null && !city.equals("error") && trigger >= 0) {
                listedCities.add(city);
                this.pendingCityTriggers.addLast(trigger);
            }
        }

        this.dailyQuests.keySet().retainAll(listedCities);

        for (String city : listedCities) {
            this.dailyQuests.putIfAbsent(city, new ArrayList<>());
        }

        this.hasDailyData = true;
        this.cityRequestCooldown = 0;
    }

    private void parseDailyCityDialog(String city, MultiActionDialog dialog) {
        List<QuestInfo> parsed = new ArrayList<>();

        for (ActionButton button : dialog.actions()) {
            Component label = button.button().label();
            String labelCity = findKeyGroup(label, DAILYQUEST_NAME_KEY, 1);
            String numberText = findKeyGroup(label, DAILYQUEST_NAME_KEY, 2);
            if (labelCity != null && numberText != null) {
                int number = Integer.parseInt(numberText);
                QuestStatus status = QuestStatus.fromColor(findColor(label));
                int remaining = -1;
                if (button.button().tooltip().isPresent()) {
                    TranslatableContents time = findTranslatable(button.button().tooltip().get(), "att2.dailyquest.remaining_time");
                    if (time != null && time.getArgs().length >= 2) {
                        int minutes = argToInt(time.getArgs()[0]).orElse(0);
                        int seconds = argToInt(time.getArgs()[1]).orElse(0);
                        remaining = minutes * 60 + seconds;
                    }
                }

                Component name = Component.translatable("att2.dailyquest." + labelCity + "." + number + ".name");
                parsed.add(new QuestInfo(QuestType.DAILY, number, name, status, triggerOf(button), labelCity, remaining));
            }
        }

        parsed.sort((a, b) -> Integer.compare(a.number(), b.number()));
        this.dailyQuests.put(city, parsed);
        this.hasDailyData = true;
    }

    private static int triggerOf(ActionButton button) {
        if (button.action().isPresent() && button.action().get() instanceof StaticAction staticAction && staticAction.value() instanceof RunCommand runCommand) {
            int trigger = Att2Triggers.parseTriggerCommand(runCommand.command());
            return trigger == 2310 ? -1 : trigger;
        } else {
            return -1;
        }
    }

    private static String findDailyCityTitle(CommonDialogData common) {
        String fromTitle = findKeyGroup(common.title(), DAILYQUEST_CITY_TITLE_KEY, 1);
        if (fromTitle != null) {
            return fromTitle;
        }

        for (DialogBody body : common.body()) {
            if (body instanceof PlainMessage message) {
                String city = findKeyGroup(message.contents(), DAILYQUEST_CITY_TITLE_KEY, 1);
                if (city != null) {
                    return city;
                }
            }
        }

        return null;
    }

    private static String findKeyGroup(Component component, Pattern pattern, int group) {
        String[] result = new String[]{null};
        visit(component, c -> {
            if (result[0] == null) {
                if (c.getContents() instanceof TranslatableContents translatable) {
                    Matcher matcher = pattern.matcher(translatable.getKey());
                    if (matcher.matches()) {
                        result[0] = matcher.group(group);
                    }
                }
            }
        });
        return result[0];
    }

    private void parseSideQuestDialog(MultiActionDialog dialog) {
        List<QuestInfo> parsed = new ArrayList<>();

        for (ActionButton button : dialog.actions()) {
            Component label = button.button().label();
            int number = findSideQuestNumber(label);
            if (number >= 0) {
                QuestStatus status = QuestStatus.fromColor(findColor(label));
                int trigger = -1;
                if (button.action().isPresent()
                    && button.action().get() instanceof StaticAction staticAction
                    && staticAction.value() instanceof RunCommand runCommand) {
                    trigger = Att2Triggers.parseTriggerCommand(runCommand.command());
                    if (trigger == 2310) {
                        trigger = -1;
                    }
                }

                Component name = Component.translatable("consciousness.sidequest." + number);
                parsed.add(new QuestInfo(QuestType.SIDE, number, name, status, trigger));
            }
        }

        parsed.sort((a, b) -> Integer.compare(a.number(), b.number()));
        this.sideQuests.clear();
        this.sideQuests.addAll(parsed);
        this.sideCompleted = (int)parsed.stream().filter(q -> q.status() == QuestStatus.COMPLETED).count();
        this.readTitleCounts(dialog.common());
        this.lastSideQuestUpdate = System.currentTimeMillis();
    }

    private void readTitleCounts(CommonDialogData common) {
        for (DialogBody body : common.body()) {
            if (body instanceof PlainMessage message) {
                TranslatableContents contents = findTranslatable(message.contents(), "consciousness.sidequest.title");
                if (contents != null && contents.getArgs().length >= 2) {
                    Optional<Integer> completed = argToInt(contents.getArgs()[0]);
                    Optional<Integer> total = argToInt(contents.getArgs()[1]);
                    completed.ifPresent(v -> this.sideCompleted = v);
                    total.ifPresent(v -> this.sideTotal = v);
                }
            }
        }
    }

    public boolean onSystemMessage(Component message, boolean overlay) {
        if (!HUDConfig.get().questMenuEnabled) {
            return true;
        }

        if (overlay) {
            return true;
        }

        TranslatableContents progress = findTranslatable(message, "att2.mainquest.progress");
        if (progress != null && progress.getArgs().length > 0) {
            argToInt(progress.getArgs()[0]).ifPresent(step -> this.mainStep = step);
        }

        if (this.captureTicks > 0 && this.captureKey != null) {
            if (progress == null) {
                this.captureBuffer.add(message);
            }

            this.captureTicks = Math.max(this.captureTicks, 5);
            return !(Minecraft.getInstance().screen instanceof QuestBookScreen);
        } else {
            return true;
        }
    }

    public List<QuestInfo> getSideQuests() {
        return Collections.unmodifiableList(this.sideQuests);
    }

    public boolean hasSideQuestData() {
        return this.lastSideQuestUpdate > 0L;
    }

    public List<QuestInfo> getDailyQuests() {
        List<QuestInfo> all = new ArrayList<>();
        this.dailyQuests.values().forEach(all::addAll);
        return all;
    }

    public boolean hasDailyQuestData() {
        return this.hasDailyData;
    }

    public boolean isWaitingForDailyQuests() {
        return this.dailyDialogWaitTicks > 0 || !this.pendingCityTriggers.isEmpty();
    }

    public boolean isWaitingForSideQuests() {
        return this.dialogWaitTicks > 0;
    }

    public boolean isCapturing() {
        return this.captureTicks > 0;
    }

    public int getSideCompleted() {
        return this.sideCompleted;
    }

    public int getSideTotal() {
        return this.sideTotal;
    }

    public int getMainStep() {
        return this.mainStep;
    }

    public int getMainMaxStep() {
        return 300;
    }

    public QuestInfo getMainQuest() {
        Component name = ModLanguageManager.get("quest_book.main_quest");
        return new QuestInfo(QuestType.MAIN, Math.max(this.mainStep, 0), name, this.mainStep >= 300 ? QuestStatus.COMPLETED : QuestStatus.STARTED, 1068);
    }

    public List<Component> getDescription(QuestInfo quest) {
        if (quest == null) {
            return List.of();
        } else {
            return this.captureKey != null && this.captureKey.equals(quest.key()) && !this.captureBuffer.isEmpty()
                ? Collections.unmodifiableList(this.captureBuffer)
                : this.descriptions.getOrDefault(quest.key(), List.of());
        }
    }

    public String getSelectedKey() {
        return this.selectedKey;
    }

    public void setSelected(QuestInfo quest) {
        this.selectedKey = quest == null ? null : quest.key();
    }

    public QuestInfo findByKey(String key) {
        if (key == null) {
            return null;
        }

        if (key.equals(this.getMainQuest().key())) {
            return this.getMainQuest();
        }

        QuestInfo side = this.sideQuests.stream().filter(q -> q.key().equals(key)).findFirst().orElse(null);
        return side != null ? side : this.getDailyQuests().stream().filter(q -> q.key().equals(key)).findFirst().orElse(null);
    }

    private static int findSideQuestNumber(Component component) {
        int[] result = new int[]{-1};
        visit(component, c -> {
            if (result[0] < 0) {
                if (c.getContents() instanceof TranslatableContents translatable) {
                    Matcher matcher = SIDEQUEST_NAME_KEY.matcher(translatable.getKey());
                    if (matcher.matches()) {
                        result[0] = Integer.parseInt(matcher.group(1));
                    }
                }
            }
        });
        return result[0];
    }

    private static TextColor findColor(Component component) {
        TextColor[] result = new TextColor[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getStyle().getColor() != null) {
                result[0] = c.getStyle().getColor();
            }
        });
        return result[0];
    }

    private static boolean bodyContainsKey(CommonDialogData common, String key) {
        if (containsKey(common.title(), key)) {
            return true;
        }

        for (DialogBody body : common.body()) {
            if (body instanceof PlainMessage message && containsKey(message.contents(), key)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsKey(Component component, String key) {
        return findTranslatable(component, key) != null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        TranslatableContents[] result = new TranslatableContents[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
                result[0] = translatable;
            }
        });
        return result[0];
    }

    private static void visit(Component component, Consumer<Component> visitor) {
        if (component != null) {
            visitor.accept(component);
            if (component.getContents() instanceof TranslatableContents translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component argComponent) {
                        visit(argComponent, visitor);
                    }
                }
            }

            for (Component sibling : component.getSiblings()) {
                visit(sibling, visitor);
            }
        }
    }

    private static Optional<Integer> argToInt(Object arg) {
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);

        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
