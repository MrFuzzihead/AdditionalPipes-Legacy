package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class ClosedPipeBufferTest {

    private final Item itemA = new Item();
    private final Item itemB = new Item();
    private final Item itemC = new Item();

    private static ItemStack stack(Item item, int size) {
        return new ItemStack(item, size);
    }

    @Test
    public void mergesIntoExistingStack() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);
        buffer.insert(stack(itemA, 32));
        buffer.insert(stack(itemA, 16));
        assertEquals(48, buffer.get(0).stackSize);
        assertNull(buffer.get(1));
    }

    @Test
    public void fillsEmptySlots() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);
        buffer.insert(stack(itemA, 64));
        buffer.insert(stack(itemB, 64));
        assertSame(
            itemA,
            buffer.get(0)
                .getItem());
        assertSame(
            itemB,
            buffer.get(1)
                .getItem());
    }

    @Test
    public void fullBufferEvictsOldestAndKeepsIncomingItem() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);
        buffer.insert(stack(itemA, 64));
        buffer.insert(stack(itemB, 64));
        buffer.insert(stack(itemC, 64));

        assertSame(
            itemB,
            buffer.get(0)
                .getItem());
        assertSame(
            itemC,
            buffer.get(1)
                .getItem());
        assertEquals(2, buffer.size());
    }

    @Test
    public void differentNbtDoesNotMerge() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);

        ItemStack first = stack(itemA, 1);
        first.setTagCompound(new NBTTagCompound());
        first.getTagCompound()
            .setInteger("v", 1);

        ItemStack second = stack(itemA, 1);
        second.setTagCompound(new NBTTagCompound());
        second.getTagCompound()
            .setInteger("v", 2);

        buffer.insert(first);
        buffer.insert(second);

        assertEquals(1, buffer.get(0).stackSize);
        assertEquals(1, buffer.get(1).stackSize);
    }

    @Test
    public void ignoresEmptyStacks() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);
        buffer.insert(null);
        buffer.insert(new ItemStack(itemA, 0));
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void removeAndClear() {
        ClosedPipeBuffer buffer = new ClosedPipeBuffer(2);
        buffer.insert(stack(itemA, 1));
        assertSame(
            itemA,
            buffer.remove(0)
                .getItem());
        assertNull(buffer.get(0));

        buffer.insert(stack(itemB, 1));
        buffer.clear();
        assertTrue(buffer.isEmpty());
    }
}
