package com.mrfuzzihead.additionalpipes.pipes;

/**
 * Pure side-selection logic for {@link PipeItemsDistributor}.
 */
final class DistributorRouting {

    static final int MAX_WEIGHT = 1000;

    private DistributorRouting() {}

    /**
     * Clamps a client-supplied distribution weight to the supported range.
     */
    static int clampWeight(int value) {
        if (value <= 0) {
            return 0;
        }
        return Math.min(MAX_WEIGHT, value);
    }

    /**
     * Returns the next side (0-5) after {@code currentSide} that is both a valid destination and has a positive
     * weight, or -1 if no such side exists.
     */
    static int nextValidSide(int[] weights, int currentSide, boolean[] valid) {
        if (weights == null || valid == null || weights.length != 6 || valid.length != 6) {
            return -1;
        }

        int side = ((currentSide % 6) + 6) % 6;
        for (int i = 0; i < 6; i++) {
            side = (side + 1) % 6;
            if (weights[side] > 0 && valid[side]) {
                return side;
            }
        }
        return -1;
    }
}
