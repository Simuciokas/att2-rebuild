package fr.poubone.att2.client.data;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class CurrencyDialogParser {
    public static final String TITLE_KEY = "consciousness.currency.title";
    public static final String CHRONOTONS_KEY = "consciousness.currency.chronotons";
    public static final String ESC_KEY = "consciousness.currency.esc";
    public static final String POWDER_KEY = "consciousness.currency.rune_material";
    public static final String BANK_KEY = "consciousness.currency.bank";

    private CurrencyDialogParser() {
    }

    public static Optional<CurrencyDialogParser.Snapshot> parse(Component body) {
        if (body == null) {
            return Optional.empty();
        } else {
            return !DialogComponents.hasKey(body, "consciousness.currency.chronotons")
                ? Optional.empty()
                : Optional.of(
                    new CurrencyDialogParser.Snapshot(
                        firstArg(body, "consciousness.currency.chronotons"),
                        firstArg(body, "consciousness.currency.esc"),
                        firstArg(body, "consciousness.currency.rune_material"),
                        firstArg(body, "consciousness.currency.bank")
                    )
                );
        }
    }

    private static int firstArg(Component body, String key) {
        TranslatableContents contents = DialogComponents.findTranslatable(body, key);
        return DialogComponents.argInt(contents, 0).orElse(0);
    }

    public record Snapshot(int chronotons, int esc, int runePowder, int bank) {
    }
}
