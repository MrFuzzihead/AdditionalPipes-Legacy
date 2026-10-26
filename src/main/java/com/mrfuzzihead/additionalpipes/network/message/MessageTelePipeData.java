package com.mrfuzzihead.additionalpipes.network.message;

import java.util.UUID;

import net.minecraft.world.World;

import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageTelePipeData extends PipePositionMessage implements IMessageHandler<MessageTelePipeData, IMessage> {

    public int[] locations;
    public String ownerUUID;
    public String ownerName;

    public MessageTelePipeData() {}

    public MessageTelePipeData(int x, int y, int z, int[] locations, UUID ownerUUID, String ownerName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.locations = locations;
        this.ownerUUID = ownerUUID == null ? "" : ownerUUID.toString();
        this.ownerName = ownerName == null ? "" : ownerName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        readPosition(buf);
        int rawLength = buf.readInt();
        if (rawLength < 0) {
            rawLength = 0;
        }

        // Only keep the first 9 locations used by the GUI, but consume the whole list so the strings stay aligned.
        int length = Math.min(rawLength, 9 * 3);
        this.locations = new int[length - (length % 3)];

        for (int counter = 0; counter < rawLength; ++counter) {
            int value = buf.readInt();
            if (counter < locations.length) {
                locations[counter] = value;
            }
        }

        ownerUUID = ByteBufUtils.readUTF8String(buf);
        ownerName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writePosition(buf);
        buf.writeInt(locations.length);
        for (int location : locations) {
            buf.writeInt(location);
        }
        ByteBufUtils.writeUTF8String(buf, ownerUUID);
        ByteBufUtils.writeUTF8String(buf, ownerName);
    }

    @Override
    public IMessage onMessage(MessageTelePipeData message, MessageContext ctx) {
        if (message.locations == null) {
            message.locations = new int[0];
        }

        World world = AdditionalPipes.proxy.getClientWorld();
        PipeTeleport<?> pipe = message.getPipe(world, PipeTeleport.class);
        if (pipe == null) {
            return null;
        }
        if (message.ownerUUID != null && !message.ownerUUID.isEmpty()) {
            try {
                pipe.ownerUUID = UUID.fromString(message.ownerUUID);
            } catch (IllegalArgumentException ignored) {
                // Ignore a malformed UUID from an untrusted server; keep the existing owner.
            }
        }
        pipe.ownerName = message.ownerName == null ? "" : message.ownerName;
        pipe.network = message.locations;

        return null;
    }

    @Override
    public String toString() {
        return "MessageTelePipeData";
    }
}
