package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.Test;

import buildcraft.transport.Pipe;

public class PipeTeleportReflectionTest {

    @Test
    public void cachesThePrivatePipeSignalMethod() {
        assertNotNull(
            "PipeTeleport failed to resolve Pipe.updateSignalState(PipeWire)",
            PipeTeleport.updateSignalState);
        assertEquals(Pipe.class, PipeTeleport.updateSignalState.getDeclaringClass());
        assertTrue(Modifier.isPrivate(PipeTeleport.updateSignalState.getModifiers()));
    }
}
