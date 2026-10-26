package com.mrfuzzihead.additionalpipes.pipes;

/**
 * Pure integer allocation for power teleport pipes.
 *
 * <p>
 * Distributes a fixed energy budget across receivers in proportion to their demand. The result always sums to at most
 * the budget and never exceeds an individual demand, so a teleport pipe cannot create energy or push more than a
 * receiver asked for. Remainders are assigned by largest fractional part, with lower indexes winning ties.
 */
final class PowerAllocation {

    private PowerAllocation() {}

    static int[] allocate(int budget, int[] demands) {
        if (demands == null || demands.length == 0) {
            return new int[0];
        }

        int[] result = new int[demands.length];
        if (budget <= 0) {
            return result;
        }

        long totalDemand = 0;
        for (int demand : demands) {
            if (demand > 0) {
                totalDemand += demand;
            }
        }
        if (totalDemand <= 0) {
            return result;
        }

        if (totalDemand <= budget) {
            for (int i = 0; i < demands.length; i++) {
                result[i] = Math.max(0, demands[i]);
            }
            return result;
        }

        long[] remainders = new long[demands.length];
        long allocated = 0;
        for (int i = 0; i < demands.length; i++) {
            int demand = Math.max(0, demands[i]);
            long scaled = (long) demand * budget / totalDemand;
            result[i] = (int) scaled;
            allocated += result[i];
            remainders[i] = ((long) demand * budget) % totalDemand;
        }

        int remaining = budget - (int) allocated;
        boolean[] receivedExtra = new boolean[demands.length];
        while (remaining > 0) {
            int best = -1;
            long bestRemainder = -1;
            for (int i = 0; i < demands.length; i++) {
                if (receivedExtra[i] || result[i] >= Math.max(0, demands[i])) {
                    continue;
                }
                if (remainders[i] > bestRemainder) {
                    best = i;
                    bestRemainder = remainders[i];
                }
            }
            if (best < 0) {
                break;
            }
            result[best]++;
            receivedExtra[best] = true;
            remaining--;
        }

        return result;
    }

    static int sum(int[] values) {
        if (values == null) {
            return 0;
        }
        long total = 0;
        for (int value : values) {
            total += Math.max(0, value);
            if (total > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }
}
