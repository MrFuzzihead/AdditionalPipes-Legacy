package com.mrfuzzihead.additionalpipes.pipes;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.api.transport.IPipeTile;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import logisticspipes.api.ILPPipeTile;

/**
 * Pipe transport for Logistics Teleport Pipes
 *
 * @author Jamie
 *
 */
public class PipeTransportItemsLogistics extends PipeTransportItems {

    /**
     * Get the number of pipes that are connected to a pipe tile.
     *
     * @param tile
     * @return
     */
    public static int getNumConnectedPipes(IPipeTile tile) {
        int count = 0;

        for (ForgeDirection o : ForgeDirection.VALID_DIRECTIONS) {
            if (tile.isPipeConnected(o)) {
                ++count;
            }
        }

        return count;
    }

    /**
     * Get the number of pipes that are connected to a pipe tile.
     *
     * @param tile
     * @return
     */
    public static int getNumConnectedPipesExceptSide(IPipeTile tile, ForgeDirection sideToExclude) {
        int count = 0;

        for (ForgeDirection o : ForgeDirection.VALID_DIRECTIONS) {
            if (sideToExclude != o && tile.isPipeConnected(o)) {
                ++count;
            }
        }

        return count;
    }

    @Override
    public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {

        ForgeDirection currentlyConnectedSide = container.pipe.getOpenOrientation()
            .getOpposite();
        if (!(currentlyConnectedSide.equals(side) || getNumConnectedPipes(container) == 0)) {
            return false;
        }

        return pipelineEndsinLogisticsPipe(tile, side.getOpposite());
    }

    public void switchSource() {
        int connectedSide = container.pipe.getOpenOrientation()
            .getOpposite()
            .ordinal();
        int newSide = 6;

        for (int i = connectedSide + 1; i <= connectedSide + 6; ++i) {
            ForgeDirection o = ForgeDirection.VALID_DIRECTIONS[i % 6];
            TileEntity tile = container.getTile(o);

            if (pipelineEndsinLogisticsPipe(tile, o.getOpposite())) {
                newSide = o.ordinal();
                break;
            }
        }

        if (newSide != ForgeDirection.UNKNOWN.ordinal() && connectedSide != ForgeDirection.UNKNOWN.ordinal()
            && connectedSide != newSide) {
            container.pipeConnectionsBuffer[connectedSide] = false;
            container.pipeConnectionsBuffer[newSide] = true;

            container.scheduleRenderUpdate();
            // worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
        }
    }

    /**
     * Iterative check for a straight item pipeline ending in a Logistics Pipe. A visited set guards against loops and
     * avoids the StackOverflowError the old recursive version could hit on large or cyclic networks.
     *
     * @param start                   first tile to inspect
     * @param sideConnectedToPrevPipe side of {@code start} that connects back to the previous pipe
     */
    public boolean pipelineEndsinLogisticsPipe(TileEntity start, ForgeDirection sideConnectedToPrevPipe) {
        Set<TileEntity> visited = Collections.newSetFromMap(new IdentityHashMap<TileEntity, Boolean>());
        TileEntity current = start;
        ForgeDirection previousSide = sideConnectedToPrevPipe;

        while (current != null) {
            if (!visited.add(current)) {
                // A loop of pipes never reaches a logistics pipe.
                return false;
            }

            // found one!
            if (current instanceof ILPPipeTile) {
                return true;
            }

            if (!(current instanceof IPipeTile)) {
                return false;
            }

            Pipe<?> pipe = (Pipe<?>) ((IPipeTile) current).getPipe();
            if (!BlockGenericPipe.isValid(pipe) || !(pipe.transport instanceof PipeTransportItems)) {
                return false;
            }

            // if it branches off, then this pipeline isn't valid
            if (getNumConnectedPipesExceptSide(pipe.container, previousSide) != 1) {
                return false;
            }

            TileEntity next = null;
            ForgeDirection nextPreviousSide = ForgeDirection.UNKNOWN;
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (side != previousSide && pipe.container.isPipeConnected(side)) {
                    next = ((IPipeTile) current).getNeighborTile(side);
                    nextPreviousSide = side.getOpposite();
                    break;
                }
            }

            if (next == null) {
                return false;
            }

            current = next;
            previousSide = nextPreviousSide;
        }

        return false;
    }

}
