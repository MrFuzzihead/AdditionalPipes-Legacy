package com.mrfuzzihead.additionalpipes.network.message;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.mrfuzzihead.additionalpipes.pipes.PipeItemsDistributor;
import com.mrfuzzihead.additionalpipes.utils.PlayerUtils;

import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Message that sets the properties of a Distribution Pipe from the GUI
 *
 */
public class MessageDistPipe extends PipePositionMessage implements IMessageHandler<MessageDistPipe, IMessage> {

    byte _index;
    int _newData;

    public MessageDistPipe() {}

    public MessageDistPipe(int x, int y, int z, byte index, int newData) {
        this.x = x;
        this.y = y;
        this.z = z;
        _index = index;
        _newData = newData;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        readPosition(buf);
        _index = buf.readByte();
        _newData = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writePosition(buf);
        buf.writeByte(_index);
        buf.writeInt(_newData);
    }

    @Override
    public IMessage onMessage(MessageDistPipe message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null || !PlayerUtils.isPlayerNearBlock(player, message.x, message.y, message.z)) {
            return null;
        }

        // Reject negative values; values above the supported maximum are clamped by setDistData.
        if (message._index < 0 || message._newData < 0) {
            return null;
        }

        World world = player.worldObj;
        PipeItemsDistributor pipe = message.getPipe(world, PipeItemsDistributor.class);
        if (pipe == null) {
            return null;
        }

        if (pipe.setDistData(message._index, message._newData)) {
            TileGenericPipe tile = message.getPipeTile(world);
            if (tile != null) {
                tile.markDirty();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MessageDistPipe";
    }
}
