package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PipeLiquidsWaterPumpTest {

    @Test
    public void onlyPumpsOnTheServerSideWithWaterBelow() {
        assertTrue(PipeLiquidsWaterPump.shouldPump(false, true));
        assertFalse(PipeLiquidsWaterPump.shouldPump(true, true));
        assertFalse(PipeLiquidsWaterPump.shouldPump(false, false));
        assertFalse(PipeLiquidsWaterPump.shouldPump(true, false));
    }
}
