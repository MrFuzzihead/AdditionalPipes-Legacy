package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import buildcraft.transport.TravelingItem;
import buildcraft.transport.pipes.events.PipeEventItem;

public class DistributorEventTest {

    private static TravelingItem item() {
        return new TestTravelingItem(new ItemStack(new Item(), 1));
    }

    private static PipeEventItem.FindDest event(PipeItemsDistributor pipe, ForgeDirection... directions) {
        return new PipeEventItem.FindDest(pipe, item(), new LinkedList<ForgeDirection>(Arrays.asList(directions)));
    }

    @Test
    public void skipsDisconnectedCurrentSide() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        pipe.distSide = ForgeDirection.DOWN.ordinal();
        pipe.curTick = Integer.MAX_VALUE;

        PipeEventItem.FindDest event = event(pipe, ForgeDirection.EAST);
        pipe.eventHandler(event);

        assertEquals(1, event.destinations.size());
        assertEquals(ForgeDirection.EAST, event.destinations.get(0));
    }

    @Test
    public void skipsZeroWeightSide() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        pipe.distData[ForgeDirection.DOWN.ordinal()] = 0;
        pipe.distSide = ForgeDirection.DOWN.ordinal();
        pipe.curTick = 0;

        PipeEventItem.FindDest event = event(pipe, ForgeDirection.DOWN, ForgeDirection.EAST);
        pipe.eventHandler(event);

        assertEquals(1, event.destinations.size());
        assertEquals(ForgeDirection.EAST, event.destinations.get(0));
    }

    @Test
    public void leavesNormalRoutingWhenNoSideIsConfigured() {
        PipeItemsDistributor pipe = new PipeItemsDistributor(null);
        Arrays.fill(pipe.distData, 0);
        pipe.distSide = ForgeDirection.DOWN.ordinal();
        pipe.curTick = 0;

        PipeEventItem.FindDest event = event(pipe, ForgeDirection.EAST);
        pipe.eventHandler(event);

        assertEquals(1, event.destinations.size());
        assertEquals(ForgeDirection.EAST, event.destinations.get(0));
    }
}
