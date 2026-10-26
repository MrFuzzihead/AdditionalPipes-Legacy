package com.mrfuzzihead.additionalpipes.pipes;

import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.transport.PipeTransport;
import buildcraft.transport.TileGenericPipe;

public class PipeSwitch<pipeType extends PipeTransport> extends APPipe<pipeType> {

    private final int textureIndex;
    private long lastPowerCheckTick = Long.MIN_VALUE;
    private boolean powered;

    public PipeSwitch(pipeType transport, Item item, int textureIndex) {
        super(transport, item);
        this.textureIndex = textureIndex;
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {

        if (direction == ForgeDirection.UNKNOWN) {
            return textureIndex;
        }

        return textureIndex + (canPipeConnect(container.getNeighborTile(direction), direction) ? 0 : 1);
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public void onNeighborBlockChange(int blockId) {
        super.onNeighborBlockChange(blockId);
        lastPowerCheckTick = Long.MIN_VALUE;
        container.scheduleNeighborChange();
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity tile = container.getTile(direction);
            if (tile instanceof TileGenericPipe) {
                ((TileGenericPipe) tile).scheduleNeighborChange();
            }
        }
    }

    @Override
    public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
        if (container == null) return false;
        return super.canPipeConnect(tile, side) && !isPowered();
    }

    /**
     * Redstone state is queried once per tick; block/neighbour changes invalidate the cache.
     */
    private boolean isPowered() {
        World world = getWorld();
        if (world == null) {
            return false;
        }

        long time = world.getTotalWorldTime();
        if (time != lastPowerCheckTick) {
            powered = world.isBlockIndirectlyGettingPowered(container.xCoord, container.yCoord, container.zCoord);
            lastPowerCheckTick = time;
        }
        return powered;
    }

}
