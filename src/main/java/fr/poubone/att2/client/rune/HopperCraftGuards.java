package fr.poubone.att2.client.rune;

public final class HopperCraftGuards {
    private HopperCraftGuards() {
    }

    public static HopperCraftGuards.Outcome evaluate(boolean inRange, boolean hopperEmpty, boolean invSatisfied, boolean pouchCanCover) {
        if (!inRange) {
            return HopperCraftGuards.Outcome.OUT_OF_RANGE;
        } else if (!hopperEmpty) {
            return HopperCraftGuards.Outcome.HOPPER_OCCUPIED;
        } else if (invSatisfied) {
            return HopperCraftGuards.Outcome.READY;
        } else {
            return pouchCanCover ? HopperCraftGuards.Outcome.POUCH_ONLY : HopperCraftGuards.Outcome.MISSING_INV;
        }
    }

    public static HopperCraftGuards.Outcome evaluateArrowCraft(
        boolean inRange, boolean hopperEmpty, boolean arrowsInInv, boolean runeInvSatisfied, boolean runePouchCanCover
    ) {
        if (!inRange) {
            return HopperCraftGuards.Outcome.OUT_OF_RANGE;
        } else if (!hopperEmpty) {
            return HopperCraftGuards.Outcome.HOPPER_OCCUPIED;
        } else if (!arrowsInInv) {
            return HopperCraftGuards.Outcome.MISSING_INV;
        } else if (runeInvSatisfied) {
            return HopperCraftGuards.Outcome.READY;
        } else {
            return runePouchCanCover ? HopperCraftGuards.Outcome.POUCH_ONLY : HopperCraftGuards.Outcome.MISSING_INV;
        }
    }

    public enum Outcome {
        READY,
        MISSING_INV,
        POUCH_ONLY,
        HOPPER_OCCUPIED,
        OUT_OF_RANGE;
    }
}
