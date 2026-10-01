package fr.poubone.att2.client.data;

import java.util.Map;

public final class SpellSelectTriggers {
    private static final Map<Integer, SpellSelectTriggers.Entry> TABLE = Map.ofEntries(
        Map.entry(1, new SpellSelectTriggers.Entry(1181, 10)),
        Map.entry(2, new SpellSelectTriggers.Entry(1208, 10)),
        Map.entry(3, new SpellSelectTriggers.Entry(1301, 10)),
        Map.entry(4, new SpellSelectTriggers.Entry(1361, 10)),
        Map.entry(5, new SpellSelectTriggers.Entry(1435, 10)),
        Map.entry(6, new SpellSelectTriggers.Entry(1447, 10)),
        Map.entry(7, new SpellSelectTriggers.Entry(1459, 10)),
        Map.entry(8, new SpellSelectTriggers.Entry(1471, 10)),
        Map.entry(9, new SpellSelectTriggers.Entry(1483, 10)),
        Map.entry(10, new SpellSelectTriggers.Entry(1193, 10)),
        Map.entry(21, new SpellSelectTriggers.Entry(1220, 3)),
        Map.entry(22, new SpellSelectTriggers.Entry(1226, 3)),
        Map.entry(23, new SpellSelectTriggers.Entry(1232, 3)),
        Map.entry(24, new SpellSelectTriggers.Entry(1238, 10)),
        Map.entry(25, new SpellSelectTriggers.Entry(1251, 10)),
        Map.entry(26, new SpellSelectTriggers.Entry(1264, 10)),
        Map.entry(27, new SpellSelectTriggers.Entry(1277, 10)),
        Map.entry(28, new SpellSelectTriggers.Entry(1289, 10)),
        Map.entry(29, new SpellSelectTriggers.Entry(3415, 10)),
        Map.entry(30, new SpellSelectTriggers.Entry(1313, 10)),
        Map.entry(33, new SpellSelectTriggers.Entry(3428, 10)),
        Map.entry(34, new SpellSelectTriggers.Entry(1349, 10)),
        Map.entry(35, new SpellSelectTriggers.Entry(3444, 10)),
        Map.entry(41, new SpellSelectTriggers.Entry(1375, 10)),
        Map.entry(42, new SpellSelectTriggers.Entry(1387, 10)),
        Map.entry(43, new SpellSelectTriggers.Entry(1399, 10)),
        Map.entry(44, new SpellSelectTriggers.Entry(1411, 10)),
        Map.entry(45, new SpellSelectTriggers.Entry(1423, 10)),
        Map.entry(46, new SpellSelectTriggers.Entry(3462, 10))
    );

    private SpellSelectTriggers() {
    }

    public static boolean isSupported(int spellId) {
        return TABLE.containsKey(spellId);
    }

    public static int levelCount(int spellId) {
        SpellSelectTriggers.Entry entry = TABLE.get(spellId);
        return entry == null ? 0 : entry.levels();
    }

    public static int levelSelectTrigger(int spellId, int level) {
        SpellSelectTriggers.Entry entry = TABLE.get(spellId);
        return entry != null && level >= 1 && level <= entry.levels() ? entry.base() + level - 1 : -1;
    }

    public static int obtainTrigger(int spellId) {
        SpellSelectTriggers.Entry entry = TABLE.get(spellId);
        return entry == null ? -1 : entry.base() + entry.levels();
    }

    private record Entry(int base, int levels) {
    }
}
