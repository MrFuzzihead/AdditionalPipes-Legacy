package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import buildcraft.transport.pipes.events.PipeEventItem;

public class PipeItemsClosedEventTest {

    private static PipeEventItem.DropItem event(PipeItemsClosed pipe, ItemStack stack) {
        return new PipeEventItem.DropItem(pipe, new TestTravelingItem(stack), null);
    }

    @Test
    public void storesDroppedItemAndSuppressesEntity() {
        PipeItemsClosed pipe = new PipeItemsClosed(null);
        PipeEventItem.DropItem event = event(pipe, new ItemStack(new Item(), 5));

        pipe.eventHandler(event);

        assertNull(event.entity);
        assertEquals(0, event.item.getItemStack().stackSize);
        assertNotNull(pipe.getStackInSlot(0));
        assertEquals(5, pipe.getStackInSlot(0).stackSize);
    }

    @Test
    public void evictsOldestWhenFull() {
        PipeItemsClosed pipe = new PipeItemsClosed(null);
        Item[] items = new Item[10];

        for (int i = 0; i < items.length; i++) {
            items[i] = new Item();
            pipe.eventHandler(event(pipe, new ItemStack(items[i], 64)));
        }

        assertEquals(9, pipe.getSizeInventory());
        assertSame(
            items[1],
            pipe.getStackInSlot(0)
                .getItem());
        assertSame(
            items[9],
            pipe.getStackInSlot(8)
                .getItem());
    }
}
