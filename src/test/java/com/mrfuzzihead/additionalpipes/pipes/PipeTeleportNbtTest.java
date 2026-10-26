package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class PipeTeleportNbtTest {

    @Test
    public void invalidOwnerAndFrequencyDoNotCrash() {
        PipeItemsTeleport pipe = new PipeItemsTeleport(null);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("freq", -5);
        tag.setByte("state", (byte) 7);
        tag.setString("ownerUUID", "not-a-uuid");
        tag.setString("ownerName", "broken");
        tag.setBoolean("isPublic", true);

        pipe.readFromNBT(tag);

        assertEquals(0, pipe.getFrequency());
        assertEquals(3, pipe.getState());
        assertNull(pipe.getOwnerUUID());
        assertTrue(pipe.isPublic());
    }

    @Test
    public void stateBitsAreClamped() {
        PipeItemsTeleport pipe = new PipeItemsTeleport(null);
        pipe.state = 0x7;
        assertTrue(pipe.canSend());
        assertTrue(pipe.canReceive());

        pipe.state = 0x4;
        assertFalse(pipe.canSend());
        assertFalse(pipe.canReceive());
    }
}
