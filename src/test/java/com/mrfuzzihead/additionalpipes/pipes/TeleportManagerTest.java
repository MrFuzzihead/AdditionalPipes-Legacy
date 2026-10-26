package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.api.TeleportManagerBase;

public class TeleportManagerTest {

    private TeleportManager manager;

    private static PipeItemsTeleport pipe(int frequency, int state, boolean isPublic, UUID owner) {
        PipeItemsTeleport pipe = new PipeItemsTeleport(null);
        pipe.setFrequency(frequency);
        pipe.state = (byte) state;
        pipe.isPublic = isPublic;
        pipe.ownerUUID = owner;
        return pipe;
    }

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
    public void addIsIdempotent() {
        PipeItemsTeleport pipe = pipe(3, 3, true, null);
        manager.addInternal(pipe, 3);
        manager.addInternal(pipe, 3);
        assertEquals(
            1,
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .size());
    }

    @Test
    public void getReturnsThePublishedManager() {
        assertSame(manager, TeleportManager.get());
    }

    @Test
    public void lookupReturnsNullBetweenServerRuns() {
        TeleportManagerBase.INSTANCE = null;
        assertNull(TeleportManager.getOrNull());
    }

    @Test
    public void channelViewsAreTypeSpecific() {
        PipeItemsTeleport pipe = pipe(3, 3, true, null);
        manager.addInternal(pipe, 3);

        assertEquals(
            1,
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .size());
        assertTrue(
            manager.getPipesInChannel(3, PipeType.FLUIDS)
                .isEmpty());
        assertTrue(
            manager.getPipesInChannel(3, PipeType.POWER)
                .isEmpty());
        assertTrue(
            manager.getPipesInChannel(3, PipeType.LOGISTICS)
                .isEmpty());
        assertTrue(
            manager.getAllItemPipesInNetwork()
                .contains(pipe));
        assertTrue(
            manager.getAllFluidPipesInNetwork()
                .isEmpty());
    }

    @Test
    public void removeRemovesPipe() {
        PipeItemsTeleport pipe = pipe(3, 3, true, null);
        manager.addInternal(pipe, 3);
        manager.removeInternal(pipe, 3);
        assertTrue(
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .isEmpty());
    }

    @Test
    public void sendReceiveFiltering() {
        PipeItemsTeleport sender = pipe(3, 3, true, null);
        PipeItemsTeleport sendOnly = pipe(3, 1, true, null);
        PipeItemsTeleport receiveOnly = pipe(3, 2, true, null);
        manager.addInternal(sender, 3);
        manager.addInternal(sendOnly, 3);
        manager.addInternal(receiveOnly, 3);

        ArrayList<PipeItemsTeleport> senders = manager.getConnectedPipes(sender, true, false);
        ArrayList<PipeItemsTeleport> receivers = manager.getConnectedPipes(sender, false, true);
        ArrayList<PipeItemsTeleport> both = manager.getConnectedPipes(sender, true, true);

        assertEquals(1, senders.size());
        assertSame(sendOnly, senders.get(0));
        assertEquals(1, receivers.size());
        assertSame(receiveOnly, receivers.get(0));
        assertEquals(2, both.size());
    }

    @Test
    public void privateChannelRequiresMatchingOwner() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();

        PipeItemsTeleport a = pipe(4, 3, false, ownerA);
        PipeItemsTeleport b = pipe(4, 3, false, ownerA);
        PipeItemsTeleport c = pipe(4, 3, false, ownerB);

        manager.addInternal(a, 4);
        manager.addInternal(b, 4);
        manager.addInternal(c, 4);

        ArrayList<PipeItemsTeleport> connected = manager.getConnectedPipes(a, true, true);
        assertEquals(1, connected.size());
        assertSame(b, connected.get(0));
    }

    @Test
    public void publicChannelOnlyConnectsToPublicPipes() {
        UUID owner = UUID.randomUUID();

        PipeItemsTeleport pub = pipe(5, 3, true, null);
        PipeItemsTeleport pub2 = pipe(5, 3, true, null);
        PipeItemsTeleport priv = pipe(5, 3, false, owner);

        manager.addInternal(pub, 5);
        manager.addInternal(pub2, 5);
        manager.addInternal(priv, 5);

        ArrayList<PipeItemsTeleport> connected = manager.getConnectedPipes(pub, true, true);
        assertEquals(1, connected.size());
        assertSame(pub2, connected.get(0));
    }

    @Test
    public void clearRemovesEverything() {
        manager.addInternal(pipe(3, 3, true, null), 3);
        manager.setFrequencyName(3, "test");
        manager.clearInternal();
        assertTrue(
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .isEmpty());
        assertEquals("", manager.getFrequencyName(3));
    }

    @Test
    public void resetClearsChannelsAndNames() {
        manager.addInternal(pipe(9, 3, true, null), 9);
        manager.setFrequencyName(9, "all");

        manager.reset();

        assertTrue(
            manager.getPipesInChannel(3, PipeType.ITEMS)
                .isEmpty());
        assertTrue(
            manager.getAllFluidPipesInNetwork()
                .isEmpty());
        assertTrue(
            manager.getAllPowerPipesInNetwork()
                .isEmpty());
        assertTrue(
            manager.getAllLogisticsPipesInNetwork()
                .isEmpty());
        assertEquals("", manager.getFrequencyName(9));
    }
}
