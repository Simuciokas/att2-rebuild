package fr.poubone.att2.client.data;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PlayerInfoDialogParser {
    public static final String TITLE_KEY = "consciousness.player_infor.title";
    public static final String GAMELEVEL_KEY = "consciousness.player_infor.gamelevel";
    public static final String LEVELMASTER_KEY = "consciousness.player_infor.levelmaster";
    public static final String ETERNAN_KEY = "consciousness.player_infor.leveleternan";
    public static final String DAHALMAX_KEY = "consciousness.player_infor.dahalmax";
    public static final String LEVEL_PROGRESS_KEY = "consciousness.player_infor.level_progress";

    private PlayerInfoDialogParser() {
    }

    public static Optional<PlayerInfoDialogParser.Snapshot> parse(List<PlayerInfoDialogParser.Action> actions) {
        if (actions == null) {
            return Optional.empty();
        }

        Integer gameLevel = null;
        int levelMaster = 0;
        Integer dahalMax = null;
        Integer xpRequired = null;
        String pending = null;
        boolean relevant = false;

        for (PlayerInfoDialogParser.Action action : actions) {
            if (action != null && action.label() != null) {
                if (DialogComponents.hasKey(action.label(), "consciousness.player_infor.gamelevel")) {
                    pending = "consciousness.player_infor.gamelevel";
                    relevant = true;
                } else if (DialogComponents.hasKey(action.label(), "consciousness.player_infor.levelmaster")) {
                    pending = "consciousness.player_infor.levelmaster";
                    relevant = true;
                } else if (DialogComponents.hasKey(action.label(), "consciousness.player_infor.leveleternan")) {
                    pending = "consciousness.player_infor.leveleternan";
                    relevant = true;
                } else if (DialogComponents.hasKey(action.label(), "consciousness.player_infor.dahalmax")) {
                    pending = "consciousness.player_infor.dahalmax";
                    relevant = true;
                } else {
                    OptionalInt number = DialogComponents.parseInt(action.label());
                    if (pending != null && number.isPresent()) {
                        int value = number.getAsInt();
                        if ("consciousness.player_infor.gamelevel".equals(pending)) {
                            gameLevel = value;
                        } else if ("consciousness.player_infor.levelmaster".equals(pending)) {
                            levelMaster = value;
                        } else if ("consciousness.player_infor.dahalmax".equals(pending)) {
                            dahalMax = value;
                        }

                        pending = null;
                    }

                    OptionalInt req = requirement(action.tooltip());
                    if (req.isPresent()) {
                        xpRequired = req.getAsInt();
                    }
                }
            }
        }

        return relevant && (gameLevel != null || dahalMax != null)
            ? Optional.of(
                new PlayerInfoDialogParser.Snapshot(
                    gameLevel == null ? 0 : gameLevel, levelMaster, dahalMax == null ? 0 : dahalMax, xpRequired == null ? 0 : xpRequired
                )
            )
            : Optional.empty();
    }

    private static OptionalInt requirement(Component tooltip) {
        TranslatableContents progress = DialogComponents.findTranslatable(tooltip, "consciousness.player_infor.level_progress");
        if (progress == null) {
            return DialogComponents.slashDenominator(tooltip);
        }

        if (progress.getArgs().length > 0) {
            OptionalInt fromArg = DialogComponents.slashDenominator(
                progress.getArgs()[0] instanceof Component c ? c : Component.literal(String.valueOf(progress.getArgs()[0]))
            );
            if (fromArg.isPresent()) {
                return fromArg;
            }
        }

        return DialogComponents.slashDenominator(tooltip);
    }

    public record Action(Component label, Component tooltip) {
    }

    public record Snapshot(int gameLevel, int levelMaster, int dahalMax, int xpRequired) {
    }
}
