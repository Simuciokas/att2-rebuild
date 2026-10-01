package fr.poubone.att2.client.quest;

import java.net.URI;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.Set;

public final class GuideLinks {
    public static final String ORIGIN = "https://guide-att2.com";
    private static final Set<String> LOCALES = Set.of("en", "zh", "ja", "ko", "ar", "ru", "es", "de", "hi", "pt");

    private GuideLinks() {
    }

    public static URI page(QuestType type, int mainOrNumber, String city, String language) {
        String hash = hash(type, mainOrNumber, city);
        if (hash == null) {
            return null;
        }

        String prefix = LOCALES.contains(language) ? "/" + language : "";
        return URI.create("https://guide-att2.com" + prefix + "/" + hash);
    }

    static String hash(QuestType type, int mainOrNumber, String city) {
        return switch (type) {
            case MAIN -> mainHash(mainOrNumber);
            case SIDE -> sideHash(mainOrNumber);
            case DAILY -> dailyHash(mainOrNumber, city);
        };
    }

    private static String mainHash(int step) {
        if (step < 0) {
            return "#rayon=main";
        }

        String book;
        if (step == 0) {
            book = "acte-1";
        } else if (step <= 50) {
            book = "acte-2";
        } else if (step <= 90) {
            book = "acte-3";
        } else if (step <= 280) {
            book = "acte-4";
        } else {
            book = "acte-5";
        }

        return "#rayon=main&livre=" + book;
    }

    private static String sideHash(int number) {
        return number >= 1 && number <= 60 ? "#rayon=side&livre=sq" + String.format(Locale.ROOT, "%02d", number) : null;
    }

    private static String dailyHash(int number, String city) {
        String slug = slug(city);
        return slug != null && number >= 1 && number <= 30 ? "#rayon=daily&livre=dq-" + slug + "-" + number : null;
    }

    private static String slug(String city) {
        if (city != null && !city.isBlank()) {
            String stripped = Normalizer.normalize(city, Form.NFD).replaceAll("\\p{M}", "");
            String slug = stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
            return slug.isEmpty() ? null : slug;
        } else {
            return null;
        }
    }
}
