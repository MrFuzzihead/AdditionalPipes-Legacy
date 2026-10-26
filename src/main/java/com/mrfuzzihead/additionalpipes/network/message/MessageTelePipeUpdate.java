package com.mrfuzzihead.additionalpipes.network.message;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;
import com.mrfuzzihead.additionalpipes.utils.PlayerUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Message that sets the properties of a Teleport Pipe from the GUI
 *
 */
public class MessageTelePipeUpdate extends PipePositionMessage
    implements IMessageHandler<MessageTelePipeUpdate, IMessage> {

    int _freq;
    boolean _isPublic;
    byte _state;

    public MessageTelePipeUpdate() {}

    public MessageTelePipeUpdate(int x, int y, int z, int freq, boolean isPublic, byte index) {
        this.x = x;
        this.y = y;
        this.z = z;
        _freq = freq;
        _isPublic = isPublic;
        _state = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        readPosition(buf);
        _freq = buf.readInt();
        _isPublic = buf.readBoolean();
        _state = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writePosition(buf);
        buf.writeInt(_freq);
        buf.writeBoolean(_isPublic);
        buf.writeByte(_state);
    }

    @Override
    public IMessage onMessage(MessageTelePipeUpdate message, MessageContext ctx) {
        EntityPlayerMP entityPlayer = ctx.getServerHandler().playerEntity;
        if (!PlayerUtils.isPlayerNearBlock(entityPlayer, message.x, message.y, message.z)) {
            return null;
        }

        PipeTeleport<?> pipe = message.getPipe(entityPlayer.worldObj, PipeTeleport.class);
        if (pipe == null) {
            return null;
        }
        // only allow the owner (or a creative player / public pipe) to change pipe state
        if (!PipeTeleport.canPlayerModifyPipe(entityPlayer, pipe)) {
            entityPlayer.addChatComponentMessage(new ChatComponentText("Sorry, You may not change pipe state."));
            return null;
        }

        pipe.setFrequency(Math.max(0, message._freq));
        pipe.state = (byte) (message._state & 0x3);
        pipe.isPublic = message._isPublic;
        pipe.container.markDirty();

        return null;
    }

    @Override
    public String toString() {
        return "MessageTelePipeUpdate";
    }
}
