package com.mrfuzzihead.additionalpipes.network.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageSerializationTest {

    @Test
    public void telePipeUpdateRoundTrip() {
        MessageTelePipeUpdate in = new MessageTelePipeUpdate(1, 2, 3, 7, true, (byte) 3);
        ByteBuf buf = Unpooled.buffer();
        try {
            in.toBytes(buf);
            MessageTelePipeUpdate out = new MessageTelePipeUpdate();
            out.fromBytes(buf);
            assertEquals(1, out.x);
            assertEquals(2, out.y);
            assertEquals(3, out.z);
            assertEquals(7, out._freq);
            assertTrue(out._isPublic);
            assertEquals(3, out._state);
        } finally {
            buf.release();
        }
    }

    @Test
    public void distPipeRoundTrip() {
        MessageDistPipe in = new MessageDistPipe(4, 5, 6, (byte) 2, 17);
        ByteBuf buf = Unpooled.buffer();
        try {
            in.toBytes(buf);
            MessageDistPipe out = new MessageDistPipe();
            out.fromBytes(buf);
            assertEquals(4, out.x);
            assertEquals(5, out.y);
            assertEquals(6, out.z);
            assertEquals(2, out._index);
            assertEquals(17, out._newData);
        } finally {
            buf.release();
        }
    }

    @Test
    public void telePipeDataRoundTrip() {
        int[] locations = { 1, 2, 3, 4, 5, 6 };
        MessageTelePipeData in = new MessageTelePipeData(10, 20, 30, locations, UUID.randomUUID(), "owner");
        ByteBuf buf = Unpooled.buffer();
        try {
            in.toBytes(buf);
            MessageTelePipeData out = new MessageTelePipeData();
            out.fromBytes(buf);
            assertEquals(10, out.x);
            assertEquals(20, out.y);
            assertEquals(30, out.z);
            assertArrayEquals(locations, out.locations);
            assertEquals("owner", out.ownerName);
        } finally {
            buf.release();
        }
    }
}
