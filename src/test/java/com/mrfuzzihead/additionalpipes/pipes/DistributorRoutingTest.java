package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DistributorRoutingTest {

    private static boolean[] allValid() {
        return new boolean[] { true, true, true, true, true, true };
    }

    @Test
    public void advancesToNextValidSide() {
        assertEquals(1, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, 0, allValid()));
    }

    @Test
    public void skipsZeroWeightSides() {
        assertEquals(2, DistributorRouting.nextValidSide(new int[] { 1, 0, 1, 1, 1, 1 }, 0, allValid()));
    }

    @Test
    public void skipsDisconnectedSides() {
        boolean[] valid = new boolean[6];
        valid[3] = true;
        assertEquals(3, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, 0, valid));
    }

    @Test
    public void wrapsAround() {
        assertEquals(0, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, 5, allValid()));
        assertEquals(0, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, -1, allValid()));
    }

    @Test
    public void returnsMinusOneWhenNoSideIsUsable() {
        boolean[] valid = new boolean[6];
        assertEquals(-1, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, 0, valid));
        assertEquals(-1, DistributorRouting.nextValidSide(new int[] { 0, 0, 0, 0, 0, 0 }, 0, allValid()));
    }

    @Test
    public void rejectsMalformedInput() {
        assertEquals(-1, DistributorRouting.nextValidSide(null, 0, allValid()));
        assertEquals(-1, DistributorRouting.nextValidSide(new int[] { 1, 1 }, 0, allValid()));
        assertEquals(-1, DistributorRouting.nextValidSide(new int[] { 1, 1, 1, 1, 1, 1 }, 0, null));
    }
}
