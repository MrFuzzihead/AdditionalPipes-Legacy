package com.mrfuzzihead.additionalpipes.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class TeleportTagTest {

    @Test
    public void markAndConsume() {
        NBTTagCompound tag = new NBTTagCompound();
        assertFalse(TeleportTag.isMarked(tag));

        TeleportTag.mark(tag, 42);
        assertTrue(TeleportTag.isMarked(tag));
        assertEquals(42, tag.getInteger(TeleportTag.KEY));

        assertTrue(TeleportTag.consume(tag));
        assertFalse(TeleportTag.isMarked(tag));
        assertFalse(TeleportTag.consume(tag));
    }

    @Test
    public void nullTagIsNotMarked() {
        assertFalse(TeleportTag.isMarked(null));
        assertFalse(TeleportTag.consume(null));
    }
}
