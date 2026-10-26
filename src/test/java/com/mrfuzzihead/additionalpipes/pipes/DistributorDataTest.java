package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DistributorDataTest {

    @Test
    public void clampsWeightsToSupportedRange() {
        assertEquals(0, DistributorRouting.clampWeight(-1));
        assertEquals(0, DistributorRouting.clampWeight(0));
        assertEquals(42, DistributorRouting.clampWeight(42));
        assertEquals(DistributorRouting.MAX_WEIGHT, DistributorRouting.clampWeight(5000));
    }

    @Test
    public void appliesWeightToRequestedSide() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        assertTrue(pipe.setDistData(2, 9));
        assertEquals(9, pipe.distData[2]);
    }

    @Test
    public void clampsValuesFromPackets() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        assertTrue(pipe.setDistData(0, 5000));
        assertEquals(DistributorRouting.MAX_WEIGHT, pipe.distData[0]);

        assertTrue(pipe.setDistData(1, -4));
        assertEquals(0, pipe.distData[1]);
    }

    @Test
    public void allZeroConfigurationResetsToOnes() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        for (int i = 0; i < pipe.distData.length; i++) {
            assertTrue(pipe.setDistData(i, 0));
        }
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1 }, pipe.distData);
    }

    @Test
    public void invalidIndexIsRejectedWithoutChangingData() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        assertFalse(pipe.setDistData(-1, 5));
        assertFalse(pipe.setDistData(pipe.distData.length, 5));
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1 }, pipe.distData);
    }
}
