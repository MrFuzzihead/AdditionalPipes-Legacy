package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PowerAllocationTest {

    @Test
    public void allocatesEverythingWhenDemandFitsBudget() {
        assertArrayEquals(new int[] { 10, 20, 30 }, PowerAllocation.allocate(100, new int[] { 10, 20, 30 }));
    }

    @Test
    public void ignoresNegativeDemands() {
        assertArrayEquals(new int[] { 0, 5, 0 }, PowerAllocation.allocate(5, new int[] { -1, 5, 0 }));
    }

    @Test
    public void proportionalAllocationUsesFullBudget() {
        int[] result = PowerAllocation.allocate(300, new int[] { 100, 200, 300 });
        assertArrayEquals(new int[] { 50, 100, 150 }, result);
        assertEquals(300, PowerAllocation.sum(result));
    }

    @Test
    public void remainderGoesToLargestFraction() {
        int[] result = PowerAllocation.allocate(2, new int[] { 1, 1, 1 });
        assertEquals(2, PowerAllocation.sum(result));
        assertTrue(result[0] <= 1 && result[1] <= 1 && result[2] <= 1);
    }

    @Test
    public void neverCreatesEnergyOrExceedsDemand() {
        int[][] demandCases = { { 1, 2, 3 }, { 0, 0, 1 }, { 7, 7, 7, 7, 7, 7 },
            { Integer.MAX_VALUE, Integer.MAX_VALUE }, { -5, 3, -2 } };
        for (int[] demands : demandCases) {
            for (int budget = 0; budget <= 100; budget++) {
                int[] result = PowerAllocation.allocate(budget, demands);
                assertTrue("sum exceeded budget", PowerAllocation.sum(result) <= budget);
                for (int i = 0; i < demands.length; i++) {
                    assertTrue("negative allocation", result[i] >= 0);
                    assertTrue("allocation exceeded demand", result[i] <= Math.max(0, demands[i]));
                }
            }
        }
    }

    @Test
    public void handlesHugeValuesWithoutOverflow() {
        int[] result = PowerAllocation.allocate(Integer.MAX_VALUE, new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE });
        assertTrue(PowerAllocation.sum(result) <= Integer.MAX_VALUE);
        assertTrue(result[0] >= 0 && result[1] >= 0);
        assertTrue(result[0] <= Integer.MAX_VALUE && result[1] <= Integer.MAX_VALUE);
    }

    @Test
    public void emptyAndZeroBudgetAreSafe() {
        assertArrayEquals(new int[0], PowerAllocation.allocate(10, null));
        assertArrayEquals(new int[0], PowerAllocation.allocate(10, new int[0]));
        assertArrayEquals(new int[] { 0, 0 }, PowerAllocation.allocate(0, new int[] { 1, 2 }));
        assertArrayEquals(new int[] { 0, 0 }, PowerAllocation.allocate(-5, new int[] { 1, 2 }));
    }

    @Test
    public void sumClampsAtIntegerMaxValue() {
        assertEquals(Integer.MAX_VALUE, PowerAllocation.sum(new int[] { Integer.MAX_VALUE, 1 }));
    }
}
