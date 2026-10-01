package fr.poubone.att2.client.data;

public final class MapReady {
    private MapReady() {
    }

    public static boolean isReady(boolean chronotonPresent, boolean dahalOrStatsBar, int numeroJoueur) {
        return chronotonPresent || dahalOrStatsBar || numeroJoueur >= 1;
    }

    public static boolean isReady() {
        boolean chronoton = ScoreCache.has("CHRONOTON");
        boolean bar = MapStatBar.hasDahalProgress() || MapStatBar.sawPointTotals() || MapStatBar.sawDetailedValues();
        int numero = ScoreCache.get("NUMEROJOUEUR").orElse(0);
        return isReady(chronoton, bar, numero) || ScoreCache.has("GAMELEVEL");
    }
}
