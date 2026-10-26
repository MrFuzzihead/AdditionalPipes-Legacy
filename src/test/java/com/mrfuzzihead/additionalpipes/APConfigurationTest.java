package com.mrfuzzihead.additionalpipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class APConfigurationTest {

    @Test
    public void waterPumpRateIsClamped() {
        assertEquals(APConfiguration.MIN_WATER_PUMP_RATE, APConfiguration.clampWaterPumpRate(Integer.MIN_VALUE));
        assertEquals(APConfiguration.MIN_WATER_PUMP_RATE, APConfiguration.clampWaterPumpRate(0));
        assertEquals(90, APConfiguration.clampWaterPumpRate(90));
        assertEquals(APConfiguration.MAX_WATER_PUMP_RATE, APConfiguration.clampWaterPumpRate(Integer.MAX_VALUE));
    }

    @Test
    public void gravityFeedTicksAreClamped() {
        assertEquals(APConfiguration.MIN_GRAVITY_FEED_TICKS, APConfiguration.clampGravityFeedTicks(Integer.MIN_VALUE));
        assertEquals(APConfiguration.MIN_GRAVITY_FEED_TICKS, APConfiguration.clampGravityFeedTicks(0));
        assertEquals(48, APConfiguration.clampGravityFeedTicks(48));
        assertEquals(APConfiguration.MAX_GRAVITY_FEED_TICKS, APConfiguration.clampGravityFeedTicks(Integer.MAX_VALUE));
    }
}
