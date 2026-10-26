package com.mrfuzzihead.additionalpipes.pipes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;

import buildcraft.api.core.IIconProvider;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;
import logisticspipes.api.ILPPipe;
import logisticspipes.api.ILPPipeTile;

public class LogisticsPipelineTest {

    private static final class TestPipe extends Pipe<PipeTransportItems> {

        TestPipe() {
            super(new PipeTransportItems(), null);
        }

        @Override
        public IIconProvider getIconProvider() {
            return null;
        }

        @Override
        public int getIconIndex(ForgeDirection direction) {
            return 0;
        }
    }

    private static final class TestPipeTile extends TileGenericPipe {

        private final Map<ForgeDirection, TileEntity> neighbors = new EnumMap<ForgeDirection, TileEntity>(
            ForgeDirection.class);
        private final Set<ForgeDirection> connections = EnumSet.noneOf(ForgeDirection.class);

        TestPipeTile() {
            pipe = new TestPipe();
            pipe.container = this;
        }

        void connect(ForgeDirection side, TileEntity neighbor) {
            connections.add(side);
            neighbors.put(side, neighbor);
        }

        @Override
        public boolean isPipeConnected(ForgeDirection with) {
            return connections.contains(with);
        }

        @Override
        public TileEntity getNeighborTile(ForgeDirection dir) {
            return neighbors.get(dir);
        }
    }

    private static final class LogisticsTile extends TileEntity implements ILPPipeTile {

        @Override
        public ILPPipe getLPPipe() {
            return null;
        }

        @Override
        public IBlockPos getPos() {
            return null;
        }
    }

    private static void link(TestPipeTile a, TestPipeTile b, ForgeDirection direction) {
        a.connect(direction, b);
        b.connect(direction.getOpposite(), a);
    }

    @Test
    public void followsStraightPipelineToLogisticsPipe() {
        TestPipeTile a = new TestPipeTile();
        TestPipeTile b = new TestPipeTile();
        LogisticsTile logistics = new LogisticsTile();
        link(a, b, ForgeDirection.EAST);
        b.connect(ForgeDirection.EAST, logistics);

        PipeTransportItemsLogistics transport = new PipeTransportItemsLogistics();
        assertTrue(transport.pipelineEndsinLogisticsPipe(a, ForgeDirection.UNKNOWN));
    }

    @Test
    public void rejectsBranchingPipeline() {
        TestPipeTile a = new TestPipeTile();
        TestPipeTile b = new TestPipeTile();
        TestPipeTile branch = new TestPipeTile();
        LogisticsTile logistics = new LogisticsTile();
        link(a, b, ForgeDirection.EAST);
        b.connect(ForgeDirection.EAST, logistics);
        b.connect(ForgeDirection.NORTH, branch);

        PipeTransportItemsLogistics transport = new PipeTransportItemsLogistics();
        assertFalse(transport.pipelineEndsinLogisticsPipe(a, ForgeDirection.UNKNOWN));
    }

    @Test
    public void rejectsPipelineWithNoLogisticsEnd() {
        TestPipeTile a = new TestPipeTile();
        TestPipeTile b = new TestPipeTile();
        link(a, b, ForgeDirection.EAST);

        PipeTransportItemsLogistics transport = new PipeTransportItemsLogistics();
        assertFalse(transport.pipelineEndsinLogisticsPipe(a, ForgeDirection.UNKNOWN));
    }

    @Test
    public void terminatesOnPipeLoopInsteadOfOverflowing() {
        TestPipeTile a = new TestPipeTile();
        TestPipeTile b = new TestPipeTile();
        TestPipeTile c = new TestPipeTile();
        TestPipeTile d = new TestPipeTile();
        link(a, b, ForgeDirection.EAST);
        link(b, c, ForgeDirection.EAST);
        link(c, d, ForgeDirection.EAST);
        link(d, a, ForgeDirection.EAST);

        PipeTransportItemsLogistics transport = new PipeTransportItemsLogistics();
        assertFalse(transport.pipelineEndsinLogisticsPipe(a, ForgeDirection.WEST));
    }
}
