package com.mrfuzzihead.additionalpipes.pipes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.api.ITeleportPipe;
import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.gui.GuiHandler;
import com.mrfuzzihead.additionalpipes.utils.Log;
import com.mrfuzzihead.additionalpipes.utils.PipePermissions;
import com.mrfuzzihead.additionalpipes.utils.PlayerUtils;

import buildcraft.api.core.Position;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.PipeWire;
import buildcraft.transport.Gate;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransport;
import buildcraft.transport.TileGenericPipe;

public abstract class PipeTeleport<pipeType extends PipeTransport> extends APPipe<pipeType> implements ITeleportPipe {

    protected static final Random rand = new Random();

    private int frequency = 0;
    boolean registered;
    // 0b0 = none, 0b1 = send, 0b10 = receive, 0b11 = both
    public byte state = 1;

    public UUID ownerUUID;
    public String ownerName = "";

    public int[] network = new int[0];
    public boolean isPublic = false;

    public final PipeType type;

    // Pipe.updateSignalState(PipeWire) is private; cache the method from the declaring class instead of walking an
    // assumed superclass chain from every concrete teleport pipe.
    static Method updateSignalState;

    static {
        try {
            updateSignalState = Pipe.class.getDeclaredMethod("updateSignalState", PipeWire.class);
            updateSignalState.setAccessible(true);
        } catch (Exception e) {
            updateSignalState = null;
            Log.error("Unable to access Pipe.updateSignalState; teleport gate signals will not propagate", e);
        }
    }

    public PipeTeleport(pipeType transport, Item item, PipeType type) {
        super(transport, item);
        this.type = type;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public PipeType getType() {
        return type;
    }

    @Override
    public void initialize() {
        super.initialize();
        tryRegister();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        unregister();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        unregister();
    }

    private void tryRegister() {
        if (registered || container == null || container.getWorldObj() == null || container.getWorldObj().isRemote) {
            return;
        }
        TeleportManager manager = TeleportManager.getOrNull();
        if (manager == null) {
            return;
        }
        manager.addInternal(this, frequency);
        registered = true;
    }

    private void unregister() {
        if (!registered) {
            return;
        }
        TeleportManager manager = TeleportManager.getOrNull();
        if (manager != null) {
            manager.removeInternal(this, frequency);
        }
        registered = false;
    }

    @Override
    public boolean blockActivated(EntityPlayer player, ForgeDirection direction) {
        if (player.worldObj.isRemote) return true;

        if (ownerUUID == null) {
            ownerUUID = PlayerUtils.getUUID(player);
            ownerName = player.getCommandSenderName();
        }

        if (!isPublic) {
            // test for player name change
            if (PlayerUtils.getUUID(player)
                .equals(ownerUUID)) {
                if (!player.getCommandSenderName()
                    .equals(ownerName)) {
                    ownerName = player.getCommandSenderName();
                }
            } else {
                // access denied
                player.addChatMessage(new ChatComponentText("Access denied!  This pipe belongs to " + ownerName));

                // if we return false, this method can get called again with a different side, and it will show the
                // message again
                return true;
            }
        }

        if (APConfiguration.filterRightclicks) {
            ItemStack equippedItem = player.getCurrentEquippedItem();

            if (equippedItem != null && AdditionalPipes.isPipe(equippedItem.getItem())) {
                return false;
            }
        }

        player.openGui(
            AdditionalPipes.instance,
            GuiHandler.PIPE_TP,
            getWorld(),
            container.xCoord,
            container.yCoord,
            container.zCoord);
        return true;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        // A pipe can be removed while its chunk is dormant without being invalidated; re-register on the next tick.
        if (!registered) {
            tryRegister();
        }
    }

    public void setFrequency(int freq) {
        if (freq == frequency) {
            return;
        }
        TeleportManager manager = TeleportManager.getOrNull();
        if (registered && manager != null) {
            manager.removeInternal(this, frequency);
        }
        frequency = freq;
        if (registered && manager != null) {
            manager.addInternal(this, frequency);
        }
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    @Override
    public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
        Pipe<?> pipe = null;
        if (tile instanceof TileGenericPipe) {
            pipe = ((TileGenericPipe) tile).pipe;
        }
        if (pipe != null && pipe instanceof PipeTeleport) return false;

        return super.canPipeConnect(tile, side);
    }

    @Override
    public boolean outputOpen(ForgeDirection to) {
        return container.isPipeConnected(to);
    }

    // Teleport Gates stuff
    // ---------------------------------------------

    @Override
    public void updateSignalState() {
        TeleportManager manager = TeleportManager.getOrNull();
        if (manager == null) {
            return;
        }

        ArrayList<PipeTeleport<?>> otherTeleportPipes = manager.<PipeTeleport<?>>getConnectedPipes(this, true, true);

        for (PipeWire wire : PipeWire.values()) {
            if (wireSet[wire.ordinal()]) {
                myUpdateSignalStateForColor(wire, otherTeleportPipes);
            }
        }
    }

    // we cannot override this because it is private
    // so we override the function that calls it and invoke our version.
    private void myUpdateSignalStateForColor(PipeWire wire, ArrayList<PipeTeleport<?>> otherTeleportPipes) {
        int prevStrength = wireSignalStrength[wire.ordinal()];
        boolean isBroadcast = false;

        for (Gate g : gates) {
            if (g != null && (g.broadcastSignal & (1 << wire.ordinal())) != 0) {
                isBroadcast = true;
                break;
            }
        }

        // find connected pipes
        ArrayList<Pipe<?>> connectedPipes = new ArrayList<Pipe<?>>();
        int maxStrength = 0;

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity tile = container.getTile(dir);
            if (tile instanceof IPipeTile) {
                Pipe<?> pipe = (Pipe<?>) ((IPipeTile) tile).getPipe();
                if (isWireConnectedTo(tile, pipe, wire, dir)) {
                    connectedPipes.add(pipe);

                    // may as well do this now instead of looping back through again later
                    int pipeStrength = pipe.wireSignalStrength[wire.ordinal()];
                    if (pipeStrength > maxStrength) {
                        maxStrength = pipeStrength;
                    }
                }
            }
        }

        connectedPipes.addAll(otherTeleportPipes);

        if (isBroadcast) {
            if (prevStrength < 255) {
                myPropagateSignalState(wire, 255, connectedPipes);
            }
        } else {
            // look for a signal
            for (Pipe<?> pipe : otherTeleportPipes) {
                int pipeStrength = pipe.wireSignalStrength[wire.ordinal()];
                if (pipeStrength > maxStrength) {
                    maxStrength = pipeStrength;
                }
            }

            if (maxStrength > prevStrength && maxStrength > 1) {
                wireSignalStrength[wire.ordinal()] = maxStrength - 1;
            } else {
                wireSignalStrength[wire.ordinal()] = 0;
            }

            if (prevStrength != wireSignalStrength[wire.ordinal()]) {
                container.scheduleRenderUpdate();
            }

            if (wireSignalStrength[wire.ordinal()] == 0) {
                for (Pipe<?> p : connectedPipes) {
                    if (p.wireSignalStrength[wire.ordinal()] > 0) {
                        invokeUpdateSignalState(p, wire);
                    }
                }
            } else {
                for (Pipe<?> p : connectedPipes) {
                    if (p.wireSignalStrength[wire.ordinal()] < (wireSignalStrength[wire.ordinal()] - 1)) {
                        invokeUpdateSignalState(p, wire);
                    }
                }
            }
        }
    }

    private static void invokeUpdateSignalState(Pipe<?> pipe, PipeWire wire) {
        if (updateSignalState == null) {
            return;
        }
        try {
            updateSignalState.invoke(pipe, wire);
        } catch (Exception e) {
            Log.error("Unable to propagate teleport gate signal", e);
        }
    }

    private void myPropagateSignalState(PipeWire wire, int strength, ArrayList<Pipe<?>> connectedPipes) {
        wireSignalStrength[wire.ordinal()] = strength;
        for (Pipe<?> pipe : connectedPipes) {
            if (strength == 0) {
                if (pipe.wireSignalStrength[wire.ordinal()] > 0) {
                    invokeUpdateSignalState(pipe, wire);
                }
            } else {
                if (pipe.wireSignalStrength[wire.ordinal()] < strength) {
                    invokeUpdateSignalState(pipe, wire);
                }
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setInteger("freq", frequency);
        nbttagcompound.setByte("state", state);
        if (ownerUUID != null) {
            nbttagcompound.setString("ownerUUID", ownerUUID.toString());
            nbttagcompound.setString("ownerName", ownerName);
        }
        nbttagcompound.setBoolean("isPublic", isPublic);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        frequency = Math.max(0, nbttagcompound.getInteger("freq"));
        state = (byte) (nbttagcompound.getByte("state") & 0x3);
        ownerUUID = null;
        ownerName = "";
        if (nbttagcompound.hasKey("ownerUUID")) {
            try {
                ownerUUID = UUID.fromString(nbttagcompound.getString("ownerUUID"));
            } catch (IllegalArgumentException ignored) {
                // Corrupt/legacy owner UUID: leave the pipe unowned rather than crashing the chunk load.
            }
            ownerName = nbttagcompound.getString("ownerName");
        }
        isPublic = nbttagcompound.getBoolean("isPublic");
    }

    public static boolean canPlayerModifyPipe(EntityPlayer player, PipeTeleport<?> pipe) {
        return PipePermissions
            .canModify(pipe.ownerUUID, pipe.isPublic, player.capabilities.isCreativeMode, PlayerUtils.getUUID(player));
    }

    public Position getPosition() {
        return new Position(container.xCoord, container.yCoord, container.zCoord);
    }

    @Override
    public boolean canReceive() {
        return (state & 0x2) > 0;
    }

    @Override
    public boolean canSend() {
        return (state & 0x1) > 0;
    }
}
