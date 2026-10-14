
package com.velocitybreaker.game;

public final class ChestRunTracker {

    private int collectedChests;
    private int chestMultiplier;
    private long totalDiamondRewards;

    public ChestRunTracker() {
        reset();
    }

    /**
     * Call this only when the player actually collects a chest.
     *
     * Example:
     * Tier 1 chest: total multiplier becomes x1.
     * Tier 2 chest: total multiplier becomes x3.
     * Tier 3 chest: total multiplier becomes x6.
     *
     * @param chestTier multiplier value assigned to this chest
     * @param baseDiamonds random base diamond reward for this chest
     * @return diamonds awarded for this chest
     */
    public synchronized long collectChest(
            int chestTier,
            int baseDiamonds) {

        if (chestTier < 1) {
            throw new IllegalArgumentException(
                    "Chest tier must be at least 1.");
        }

        if (baseDiamonds < 1 || baseDiamonds > 100) {
            throw new IllegalArgumentException(
                    "Base diamonds must be between 1 and 100.");
        }

        chestMultiplier = Math.addExact(
                chestMultiplier, chestTier);

        collectedChests++;

        long reward = Math.multiplyExact(
                (long) baseDiamonds, chestMultiplier);

        totalDiamondRewards = Math.addExact(
                totalDiamondRewards, reward);

        return reward;
    }

    /**
     * Call this when a chest is missed.
     * A missed chest must not change any totals.
     */
    public synchronized void missChest() {
        // Intentionally does nothing.
    }

    public synchronized int getCollectedChests() {
        return collectedChests;
    }

    public synchronized int getChestMultiplier() {
        return chestMultiplier;
    }

    public synchronized long getTotalDiamondRewards() {
        return totalDiamondRewards;
    }

    public synchronized void reset() {
        collectedChests = 0;
        chestMultiplier = 0;
        totalDiamondRewards = 0;
    }
}
