package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.hud.ModToast;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class WorkshopHopperCraft {
    private static final int OPEN_TIMEOUT_TICKS = 40;
    private static WorkshopHopperCraft.Phase phase = WorkshopHopperCraft.Phase.IDLE;
    private static BlockPos hopperPos;
    private static List<RuneCatalog.Ingredient> pendingIngredients = List.of();
    private static RuneCatalog.ArrowDef pendingArrow;
    private static int pendingBaseArrowCount;
    private static int waitTicks;

    private WorkshopHopperCraft() {
    }

    public static void craftArrow(RuneCatalog.ArrowDef arrow) {
        craftArrow(arrow, 1);
    }

    public static void craftArrow(RuneCatalog.ArrowDef arrow, int qty) {
        if (arrow != null && qty >= 1) {
            if (phase != WorkshopHopperCraft.Phase.IDLE) {
                tipKey("rune_codex.auto.failed");
            } else {
                Minecraft client = Minecraft.getInstance();
                LocalPlayer player = client.player;
                if (player != null && client.level != null && client.gameMode != null) {
                    BlockPos hopper = findHopper(client);
                    boolean inRange = hopper != null && inReach(player, hopper);
                    boolean hopperEmpty = hopper != null && isHopperEmpty(client.level, hopper);
                    int needBase = arrow.baseArrowCount() * qty;
                    boolean arrowsInInv = RuneItems.countCraftBaseArrows(player, arrow) >= needBase;
                    boolean runeInvSatisfied = invSatisfied(player, arrow.runes(), qty);
                    boolean runePouchCanCover = pouchCanCover(player, arrow.runes(), qty);
                    HopperCraftGuards.Outcome outcome = HopperCraftGuards.evaluateArrowCraft(
                        inRange, hopperEmpty, arrowsInInv, runeInvSatisfied, runePouchCanCover
                    );
                    if (outcome != HopperCraftGuards.Outcome.READY) {
                        tip(outcome);
                    } else {
                        hopperPos = hopper;
                        pendingIngredients = scaleIngredients(arrow.runes(), qty);
                        pendingArrow = arrow;
                        pendingBaseArrowCount = needBase;
                        waitTicks = 0;
                        phase = WorkshopHopperCraft.Phase.CLOSE_SCREEN;
                    }
                }
            }
        }
    }

    private static List<RuneCatalog.Ingredient> scaleIngredients(List<RuneCatalog.Ingredient> src, int qty) {
        ArrayList<RuneCatalog.Ingredient> out = new ArrayList<>(src.size());

        for (RuneCatalog.Ingredient ing : src) {
            out.add(new RuneCatalog.Ingredient(ing.runeId(), ing.count() * qty));
        }

        return List.copyOf(out);
    }

    public static void tip(HopperCraftGuards.Outcome outcome) {
        String key = switch (outcome) {
            case POUCH_ONLY -> "rune_codex.auto.pouch";
            case MISSING_INV -> "rune_codex.auto.missing";
            case OUT_OF_RANGE -> "rune_codex.auto.range";
            case HOPPER_OCCUPIED -> "rune_codex.auto.hopper_full";
            case READY -> null;
        };
        if (key != null) {
            tipKey(key);
        }
    }

    public static void tick(Minecraft client) {
        if (phase != WorkshopHopperCraft.Phase.IDLE) {
            LocalPlayer player = client.player;
            if (player != null && client.level != null && client.gameMode != null) {
                switch (phase) {
                    case CLOSE_SCREEN:
                        client.setScreen(null);
                        phase = WorkshopHopperCraft.Phase.OPEN_HOPPER;
                        break;
                    case OPEN_HOPPER:
                        if (hopperPos == null || !client.level.getBlockState(hopperPos).is(Blocks.HOPPER)) {
                            tipKey("rune_codex.auto.failed");
                            abort();
                            return;
                        }

                        Vec3 hit = Vec3.atCenterOf(hopperPos);
                        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, hopperPos, false);
                        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
                        waitTicks = 0;
                        phase = WorkshopHopperCraft.Phase.WAIT_MENU;
                        break;
                    case WAIT_MENU:
                        waitTicks++;
                        if (player.containerMenu instanceof HopperMenu) {
                            phase = WorkshopHopperCraft.Phase.DEPOSIT;
                            return;
                        }

                        if (waitTicks >= 40) {
                            tipKey("rune_codex.auto.failed");
                            abort();
                        }
                        break;
                    case DEPOSIT:
                        if (!(player.containerMenu instanceof HopperMenu menu)) {
                            tipKey("rune_codex.auto.failed");
                            abort();
                            return;
                        }

                        if (!hopperSlotsEmpty(menu)) {
                            tip(HopperCraftGuards.Outcome.HOPPER_OCCUPIED);
                            player.closeContainer();
                            abort();
                            return;
                        }

                        boolean deposited = depositCraftMaterials(client, player, menu, pendingIngredients, pendingArrow, pendingBaseArrowCount);
                        if (!deposited) {
                            tip(HopperCraftGuards.Outcome.MISSING_INV);
                            player.closeContainer();
                            abort();
                            return;
                        }

                        phase = WorkshopHopperCraft.Phase.CLOSE_MENU;
                        break;
                    case CLOSE_MENU:
                        if (player.containerMenu instanceof HopperMenu) {
                            player.closeContainer();
                        }

                        tipOk("rune_codex.auto.deposited");
                        abort();
                        break;
                    default:
                        abort();
                }
            } else {
                abort();
            }
        }
    }

    private static void tipKey(String key) {
        ModToast.showError(ModLanguageManager.get(key));
    }

    private static void tipOk(String key) {
        ModToast.showOk(ModLanguageManager.get(key));
    }

    private static void abort() {
        phase = WorkshopHopperCraft.Phase.IDLE;
        hopperPos = null;
        pendingIngredients = List.of();
        pendingArrow = null;
        pendingBaseArrowCount = 0;
        waitTicks = 0;
    }

    private static boolean invSatisfied(LocalPlayer player, List<RuneCatalog.Ingredient> ingredients, int qty) {
        for (RuneCatalog.Ingredient ingredient : ingredients) {
            RuneCatalog.RuneDef rune = RuneCatalog.byId(ingredient.runeId()).orElse(null);
            if (rune == null) {
                return false;
            }

            if (RuneStock.counts(player, rune).inventory() < ingredient.count() * qty) {
                return false;
            }
        }

        return true;
    }

    private static boolean pouchCanCover(LocalPlayer player, List<RuneCatalog.Ingredient> ingredients, int qty) {
        for (RuneCatalog.Ingredient ingredient : ingredients) {
            RuneCatalog.RuneDef rune = RuneCatalog.byId(ingredient.runeId()).orElse(null);
            if (rune == null) {
                return false;
            }

            int need = ingredient.count() * qty;
            RuneStock.Counts counts = RuneStock.counts(player, rune);
            int inv = counts.inventory();
            if (inv < need) {
                OptionalInt pouch = counts.pouch();
                if (pouch.isEmpty() || inv + pouch.getAsInt() < need) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean inReach(LocalPlayer player, BlockPos pos) {
        double reach = 5.0;
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach;
    }

    private static BlockPos findHopper(Minecraft client) {
        Level level = client.level;
        LocalPlayer player = client.player;
        if (level != null && player != null) {
            BlockPos exact = new BlockPos(-5029, 90, -4958);
            if (isHopperBlock(level, exact) && matchesHopperName(level, exact)) {
                return exact;
            }

            if (isHopperBlock(level, exact)) {
                return exact;
            }

            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            int r = (int)Math.ceil(5.0);

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = exact.offset(dx, dy, dz);
                        if (isHopperBlock(level, pos) && matchesHopperName(level, pos)) {
                            double dist = player.distanceToSqr(Vec3.atCenterOf(pos));
                            if (dist < bestDist) {
                                bestDist = dist;
                                best = pos;
                            }
                        }
                    }
                }
            }

            return best;
        } else {
            return null;
        }
    }

    private static boolean isHopperBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.HOPPER) && level.getBlockEntity(pos) instanceof HopperBlockEntity;
    }

    private static boolean matchesHopperName(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof HopperBlockEntity hopper)) {
            return false;
        } else {
            Component name = hopper.getCustomName();
            return name != null && containsTranslateKey(name, "att2.runes.crafting");
        }
    }

    private static boolean isHopperEmpty(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HopperBlockEntity hopper ? hopper.isEmpty() : false;
    }

    private static boolean hopperSlotsEmpty(HopperMenu menu) {
        for (int i = 0; i < 5; i++) {
            if (!menu.getSlot(i).getItem().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static boolean depositCraftMaterials(
        Minecraft client, LocalPlayer player, HopperMenu menu, List<RuneCatalog.Ingredient> ingredients, RuneCatalog.ArrowDef arrow, int baseArrowNeed
    ) {
        int arrowsLeft = Math.max(0, baseArrowNeed);
        Map<String, Integer> runesLeft = new HashMap<>();

        for (RuneCatalog.Ingredient ingredient : ingredients) {
            runesLeft.merge(ingredient.runeId(), ingredient.count(), Integer::sum);
        }

        int runeTotal = runesLeft.values().stream().mapToInt(Integer::intValue).sum();
        int guard = Math.max(64, arrowsLeft + runeTotal + 5);

        while (guard-- > 0 && (arrowsLeft > 0 || runesLeft.values().stream().anyMatch(n -> n > 0))) {
            int playerSlot = findNeededPlayerSlot(menu, arrow, arrowsLeft, runesLeft);
            if (playerSlot < 0) {
                break;
            }

            ItemStack stack = menu.getSlot(playerSlot).getItem();
            int hopperSlot = firstEmptyHopperSlot(menu);
            if (hopperSlot < 0) {
                break;
            }

            if (arrowsLeft > 0 && arrow != null && RuneItems.matchesCraftBaseArrow(stack, arrow)) {
                arrowsLeft = moveNeeded(client, player, menu, playerSlot, hopperSlot, stack, arrowsLeft);
            } else {
                String runeId = matchingRuneId(stack, runesLeft);
                if (runeId == null) {
                    break;
                }

                int need = runesLeft.get(runeId);
                int left = moveNeeded(client, player, menu, playerSlot, hopperSlot, stack, need);
                runesLeft.put(runeId, left);
            }
        }

        return arrowsLeft <= 0 && runesLeft.values().stream().allMatch(n -> n <= 0);
    }

    private static int moveNeeded(Minecraft client, LocalPlayer player, HopperMenu menu, int playerSlot, int hopperSlot, ItemStack stack, int need) {
        if (stack.getCount() <= need) {
            int moved = stack.getCount();
            client.gameMode.handleInventoryMouseClick(menu.containerId, playerSlot, 0, ClickType.QUICK_MOVE, player);
            return need - moved;
        }

        client.gameMode.handleInventoryMouseClick(menu.containerId, playerSlot, 0, ClickType.PICKUP, player);

        for (int i = 0; i < need; i++) {
            client.gameMode.handleInventoryMouseClick(menu.containerId, hopperSlot, 1, ClickType.PICKUP, player);
        }

        client.gameMode.handleInventoryMouseClick(menu.containerId, playerSlot, 0, ClickType.PICKUP, player);
        return 0;
    }

    private static int findNeededPlayerSlot(HopperMenu menu, RuneCatalog.ArrowDef arrow, int arrowsLeft, Map<String, Integer> runesLeft) {
        for (int slot = 5; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                if (arrowsLeft > 0 && arrow != null && RuneItems.matchesCraftBaseArrow(stack, arrow)) {
                    return slot;
                }

                if (matchingRuneId(stack, runesLeft) != null) {
                    return slot;
                }
            }
        }

        return -1;
    }

    private static int firstEmptyHopperSlot(HopperMenu menu) {
        for (int i = 0; i < 5; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    private static String matchingRuneId(ItemStack stack, Map<String, Integer> remaining) {
        for (Entry<String, Integer> entry : remaining.entrySet()) {
            if (entry.getValue() > 0) {
                RuneCatalog.RuneDef rune = RuneCatalog.byId(entry.getKey()).orElse(null);
                if (rune != null && RuneStock.countStack(stack, rune) > 0) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    private static boolean containsTranslateKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
            return true;
        } else {
            for (Component sibling : component.getSiblings()) {
                if (containsTranslateKey(sibling, key)) {
                    return true;
                }
            }

            return false;
        }
    }

    private enum Phase {
        IDLE,
        CLOSE_SCREEN,
        OPEN_HOPPER,
        WAIT_MENU,
        DEPOSIT,
        CLOSE_MENU;
    }
}
