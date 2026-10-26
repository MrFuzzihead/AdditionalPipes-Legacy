package com.mrfuzzihead.additionalpipes.network.message;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Common coordinate and pipe-lookup helpers for client/server pipe messages.
 */
public abstract class PipePositionMessage implements IMessage {

    public int x, y, z;

    protected final void writePosition(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    protected final void readPosition(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    /**
     * Resolves the pipe at this message's position, returning {@code null} if the tile or pipe type does not match.
     */
    protected final <T> T getPipe(World world, Class<T> pipeType) {
        if (world == null) {
            return null;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileGenericPipe)) {
            return null;
        }

        Object pipe = ((TileGenericPipe) tile).pipe;
        return pipeType.isInstance(pipe) ? pipeType.cast(pipe) : null;
    }

    protected final TileGenericPipe getPipeTile(World world) {
        if (world == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileGenericPipe ? (TileGenericPipe) tile : null;
    }
}
