package com.mrfuzzihead.additionalpipes.selftest;

import java.util.Arrays;
import java.util.LinkedList;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsAddition;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsClosed;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsTeleport;
import com.mrfuzzihead.additionalpipes.pipes.PipeLiquidsWaterPump;
import com.mrfuzzihead.additionalpipes.pipes.PipePowerTeleport;
import com.mrfuzzihead.additionalpipes.pipes.TeleportManager;
import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.BuildCraftTransport;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.TravelingItem;
import buildcraft.transport.pipes.events.PipeEventItem;
import cofh.api.energy.IEnergyReceiver;

/**
 * Headless dedicated-server integration checks for behaviour that cannot be exercised by plain JUnit tests.
 *
 * <p>
 * Enabled with {@code -Dadditionalpipes.selftest=true} and run from {@code FMLServerStartedEvent}. Each check logs
 * {@code [SELFTEST] PASS/FAIL}; the server shuts down after the suite so a Gradle task can verify the log.
 */
public final class SelfTest {

    private static final int Y = 100;
    private static int failures;

    private interface Check {

        void run() throws Exception;
    }

    private static final class TestEnergyChest extends TileEntityChest implements IEnergyReceiver {

        private final int capacity = 1000;
        private int received;

        @Override
        public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
            int accepted = Math.min(Math.max(0, maxReceive), capacity - received);
            if (!simulate) {
                received += accepted;
            }
            return accepted;
        }

        @Override
        public int getEnergyStored(ForgeDirection from) {
            return received;
        }

        @Override
        public int getMaxEnergyStored(ForgeDirection from) {
            return capacity;
        }

        @Override
        public boolean canConnectEnergy(ForgeDirection from) {
            return true;
        }
    }

    private SelfTest() {}

    public static void run() {
        Log.info("[SELFTEST] START");
        failures = 0;

        check("registration", SelfTest::registration);
        check("teleport lifecycle", SelfTest::teleportLifecycle);
        check("closed pipe", SelfTest::closedPipe);
        check("water pump", SelfTest::waterPump);
        check("power teleport", SelfTest::powerTeleport);
        check("addition pipe", SelfTest::additionPipe);
        check("commands", SelfTest::commands);
        check("manager reset", SelfTest::managerReset);

        Log.info("[SELFTEST] DONE failures=" + failures);
        MinecraftServer.getServer()
            .initiateShutdown();
    }

    private static void check(String name, Check check) {
        try {
            check.run();
            Log.info("[SELFTEST] PASS " + name);
        } catch (Throwable t) {
            failures++;
            Log.error("[SELFTEST] FAIL " + name + ": " + t, t);
        }
    }

    private static void assertTrue(String message, boolean value) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static WorldServer world() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        world.getChunkFromChunkCoords(0, 0);
        return world;
    }

    private static void clear(World world, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = Y - 1; y <= Y + 1; y++) {
                    world.setBlockToAir(x, y, z);
                }
            }
        }
    }

    private static Pipe<?> place(Item item, int x, int y, int z) {
        World world = world();
        Pipe<?> pipe = BlockGenericPipe.createPipe(item);
        assertTrue("createPipe returned null for " + item, pipe != null);

        boolean placed = BlockGenericPipe
            .placePipe(pipe, world, x, y, z, BuildCraftTransport.genericPipeBlock, 0, null, ForgeDirection.UP);
        assertTrue("placePipe failed at " + x + "," + y + "," + z, placed);

        Pipe<?> placedPipe = BlockGenericPipe.getPipe(world, x, y, z);
        assertTrue("pipe missing after placement at " + x + "," + y + "," + z, placedPipe != null);
        return placedPipe;
    }

    private static void remove(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileGenericPipe && ((TileGenericPipe) tile).pipe != null) {
            ((TileGenericPipe) tile).pipe.invalidate();
        }
        world.setBlockToAir(x, y, z);
    }

    private static void registration() {
        Item[] items = { AdditionalPipes.instance.pipeItemsTeleport, AdditionalPipes.instance.pipeLiquidsTeleport,
            AdditionalPipes.instance.pipePowerTeleport, AdditionalPipes.instance.pipeItemsDistributor,
            AdditionalPipes.instance.pipeItemsAddition, AdditionalPipes.instance.pipeItemsGravityFeed,
            AdditionalPipes.instance.pipeItemsClosed, AdditionalPipes.instance.pipeItemsSwitch,
            AdditionalPipes.instance.pipePowerSwitch, AdditionalPipes.instance.pipeLiquidsSwitch,
            AdditionalPipes.instance.pipeLiquidsWaterPump };

        for (Item item : items) {
            assertTrue("pipe item is null", item != null);
            assertTrue("pipe class is not registered: " + item, BlockGenericPipe.pipes.containsKey(item));
            assertTrue("pipe has no registry name: " + item, Item.itemRegistry.getNameForObject(item) != null);
        }

        if (AdditionalPipes.instance.logisticsPipesInstalled) {
            Item logistics = AdditionalPipes.instance.pipeLogisticsTeleport;
            assertTrue("logistics pipe is null", logistics != null);
            assertTrue("logistics pipe is not registered", BlockGenericPipe.pipes.containsKey(logistics));
        }
    }

    private static void teleportLifecycle() {
        World world = world();
        clear(world, 0, 0, 2, 0);

        PipeItemsTeleport a = (PipeItemsTeleport) place(AdditionalPipes.instance.pipeItemsTeleport, 0, Y, 0);
        PipeItemsTeleport b = (PipeItemsTeleport) place(AdditionalPipes.instance.pipeItemsTeleport, 2, Y, 0);

        a.state = 3;
        b.state = 3;
        a.isPublic = true;
        b.isPublic = true;
        a.setFrequency(7);
        b.setFrequency(7);

        assertTrue(
            "pipes were not linked",
            TeleportManager.get()
                .getConnectedPipes(b, true, true)
                .contains(a));

        a.onChunkUnload();
        assertTrue(
            "pipe remained registered after unload",
            !TeleportManager.get()
                .getPipesInChannel(7, PipeType.ITEMS)
                .contains(a));

        a.updateEntity();
        assertTrue(
            "pipe was not re-registered after unload",
            TeleportManager.get()
                .getPipesInChannel(7, PipeType.ITEMS)
                .contains(a));

        a.setFrequency(8);
        assertTrue(
            "old frequency still contains pipe",
            !TeleportManager.get()
                .getPipesInChannel(7, PipeType.ITEMS)
                .contains(a));
        assertTrue(
            "new frequency is missing pipe",
            TeleportManager.get()
                .getPipesInChannel(8, PipeType.ITEMS)
                .contains(a));

        remove(world, 0, Y, 0);
        remove(world, 2, Y, 0);
    }

    private static void closedPipe() {
        World world = world();
        clear(world, 4, 0, 4, 0);

        PipeItemsClosed pipe = (PipeItemsClosed) place(AdditionalPipes.instance.pipeItemsClosed, 4, Y, 0);
        PipeEventItem.DropItem event = new PipeEventItem.DropItem(
            pipe,
            TravelingItem.make(4.5, Y + 0.5, 0.5, new ItemStack(Blocks.cobblestone, 5)),
            null);

        pipe.eventHandler(event);

        assertTrue("world drop was not suppressed", event.entity == null);
        assertTrue("item was not consumed", event.item.getItemStack().stackSize == 0);
        assertTrue("buffer was not populated", pipe.getStackInSlot(0) != null && pipe.getStackInSlot(0).stackSize == 5);

        pipe.dropContents();
        assertTrue("buffer was not cleared on drop", pipe.getStackInSlot(0) == null);

        remove(world, 4, Y, 0);
    }

    private static void waterPump() {
        World world = world();
        for (int y = 4; y <= 6; y++) {
            world.setBlockToAir(6, y, 0);
        }

        PipeLiquidsWaterPump pump = (PipeLiquidsWaterPump) place(
            AdditionalPipes.instance.pipeLiquidsWaterPump,
            6,
            5,
            0);

        // Place the water after the pipe with flag 2 (no neighbour notify) so it stays a static source for this tick.
        world.setBlock(6, 4, 0, Blocks.water, 0, 2);
        pump.updateEntity();

        FluidStack stack = pump.transport.getStack(ForgeDirection.DOWN);
        assertTrue(
            "water pump produced no water",
            stack != null && stack.getFluid() == FluidRegistry.WATER && stack.amount > 0);

        remove(world, 6, 5, 0);
        world.setBlockToAir(6, 4, 0);
    }

    private static void powerTeleport() {
        World world = world();
        clear(world, 6, 0, 8, 0);

        PipePowerTeleport sender = (PipePowerTeleport) place(AdditionalPipes.instance.pipePowerTeleport, 6, Y, 0);
        PipePowerTeleport receiver = (PipePowerTeleport) place(AdditionalPipes.instance.pipePowerTeleport, 7, Y, 0);
        // A chest gives TileBuffer a block with a tile entity; the subclass keeps BlockChest's casts safe.
        world.setBlock(8, Y, 0, Blocks.chest, 0, 3);
        TestEnergyChest energy = new TestEnergyChest();
        world.setTileEntity(8, Y, 0, energy);

        sender.state = 1;
        receiver.state = 2;
        sender.isPublic = true;
        receiver.isPublic = true;
        sender.setFrequency(11);
        receiver.setFrequency(11);
        receiver.container.getTileCache()[ForgeDirection.EAST.ordinal()].refresh();

        float previousTransmittance = APConfiguration.powerTransmittanceCfg;
        APConfiguration.powerTransmittanceCfg = 0.5f;
        try {
            int returned = sender.receiveEnergy(ForgeDirection.UP, 1000);
            assertTrue("power teleport returned more than it received", returned <= 1000);
            assertTrue("power teleport created energy: " + returned, returned == 500);
            assertTrue(
                "receiver got the wrong amount: " + energy.getEnergyStored(ForgeDirection.UP),
                energy.getEnergyStored(ForgeDirection.UP) == 500);

            // A full receiver has no demand; the teleport pipe must not claim the energy.
            energy.receiveEnergy(ForgeDirection.UP, 1000, false);
            assertTrue(
                "power teleport accepted energy with no demand",
                sender.receiveEnergy(ForgeDirection.UP, 1000) == 0);
        } finally {
            APConfiguration.powerTransmittanceCfg = previousTransmittance;
            remove(world, 6, Y, 0);
            remove(world, 7, Y, 0);
            world.setBlockToAir(8, Y, 0);
        }
    }

    private static void additionPipe() {
        World world = world();
        clear(world, 7, 0, 9, 0);

        world.setBlock(9, Y, 0, Blocks.chest);
        TileEntityChest matchingChest = (TileEntityChest) world.getTileEntity(9, Y, 0);
        assertTrue("matching chest was not created", matchingChest != null);
        matchingChest.setInventorySlotContents(0, new ItemStack(Blocks.cobblestone, 1));

        world.setBlock(7, Y, 0, Blocks.chest);
        TileEntityChest emptyChest = (TileEntityChest) world.getTileEntity(7, Y, 0);
        assertTrue("empty chest was not created", emptyChest != null);

        PipeItemsAddition pipe = (PipeItemsAddition) place(AdditionalPipes.instance.pipeItemsAddition, 8, Y, 0);
        LinkedList<ForgeDirection> destinations = new LinkedList<ForgeDirection>(
            Arrays.asList(ForgeDirection.EAST, ForgeDirection.WEST));
        PipeEventItem.FindDest event = new PipeEventItem.FindDest(
            pipe,
            TravelingItem.make(8.5, Y + 0.5, 0.5, new ItemStack(Blocks.cobblestone, 1)),
            destinations);

        pipe.eventHandler(event);

        assertTrue("did not choose the chest containing the item", event.destinations.contains(ForgeDirection.EAST));
        assertTrue("chose the empty chest", !event.destinations.contains(ForgeDirection.WEST));

        remove(world, 8, Y, 0);
        world.setBlockToAir(9, Y, 0);
        world.setBlockToAir(7, Y, 0);
    }

    private static void managerReset() {
        TeleportManager.get()
            .reset();
        assertTrue(
            "item channels were not cleared",
            TeleportManager.get()
                .getAllItemPipesInNetwork()
                .isEmpty());
        assertTrue(
            "fluid channels were not cleared",
            TeleportManager.get()
                .getAllFluidPipesInNetwork()
                .isEmpty());
        assertTrue(
            "power channels were not cleared",
            TeleportManager.get()
                .getAllPowerPipesInNetwork()
                .isEmpty());
        assertTrue(
            "logistics channels were not cleared",
            TeleportManager.get()
                .getAllLogisticsPipesInNetwork()
                .isEmpty());
    }

    private static void commands() {
        MinecraftServer server = MinecraftServer.getServer();
        server.getCommandManager()
            .executeCommand(server, "additionalpipes teleport");
        server.getCommandManager()
            .executeCommand(server, "additionalpipes teleport items");
        server.getCommandManager()
            .executeCommand(server, "additionalpipes teleport power");
        server.getCommandManager()
            .executeCommand(server, "additionalpipes teleport logistics");
    }
}
