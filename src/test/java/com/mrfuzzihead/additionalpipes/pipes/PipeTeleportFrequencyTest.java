package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.api.TeleportManagerBase;

public class PipeTeleportFrequencyTest {

    private TeleportManager manager;

    @Before
    public void reset() {
        manager = new TeleportManager();
        TeleportManagerBase.INSTANCE = manager;
    }

    @After
    public void cleanup() {
        manager.clearInternal();
        TeleportManagerBase.INSTANCE = null;
    }

    @Test
    public void setFrequencyWithoutRegistrationOnlyChangesField() {
        PipeItemsTeleport pipe = new PipeItemsTeleport(null);
        pipe.setFrequency(3);

        assertEquals(3, pipe.getFrequency());
        assertTrue(
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .isEmpty());
    }

    @Test
    public void setFrequencyReRegistersRegisteredPipe() {
        PipeItemsTeleport pipe = new PipeItemsTeleport(null);
        pipe.setFrequency(3);
        pipe.registered = true;
        manager.addInternal(pipe, 3);

        pipe.setFrequency(4);

        assertTrue(
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .isEmpty());
        assertEquals(
            1,
            manager.getPipesInChannel(4, PipeType.ITEMS)
                .size());
        assertSame(
            pipe,
            manager.getPipesInChannel(4, PipeType.ITEMS)
                .iterator()
                .next());
    }
}
