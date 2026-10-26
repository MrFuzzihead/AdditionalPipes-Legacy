package com.mrfuzzihead.additionalpipes.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;

import com.mrfuzzihead.additionalpipes.network.PacketHandler;
import com.mrfuzzihead.additionalpipes.network.message.MessageTelePipeData;
import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;
import com.mrfuzzihead.additionalpipes.pipes.TeleportManager;
import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.core.lib.gui.BuildCraftContainer;
import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class ContainerTeleportPipe extends BuildCraftContainer {

    public int connectedPipes = 0;

    private int ticks = 0;
    public PipeTeleport<?> pipe;
    private int freq;
    private byte state;
    private boolean isPublic;

    // true if the provided pipe is sending items to other pipes
    // and output locations should be shown on the ledger
    private boolean isSendingPipe;

    public ContainerTeleportPipe(EntityPlayer player, PipeTeleport<?> pipe) {
        super(0);
        this.pipe = pipe;

        // set these variables to invalid values so that they will be updated
        state = -1;
        isPublic = !pipe.isPublic;
        freq = -1;

        isSendingPipe = pipe.canSend();

        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            TeleportManager manager = TeleportManager.getOrNull();
            if (manager == null) {
                return;
            }
            List<PipeTeleport<?>> connectedPipes = manager
                .<PipeTeleport<?>>getConnectedPipes(pipe, !isSendingPipe, isSendingPipe);
            int[] locations = new int[connectedPipes.size() * 3];
            for (int i = 0; i < connectedPipes.size() && i < 9; i++) {
                PipeTeleport<?> connectedPipe = connectedPipes.get(i);
                locations[3 * i] = connectedPipe.container.xCoord;
                locations[3 * i + 1] = connectedPipe.container.yCoord;
                locations[3 * i + 2] = connectedPipe.container.zCoord;
            }

            MessageTelePipeData message = new MessageTelePipeData(
                pipe.container.xCoord,
                pipe.container.yCoord,
                pipe.container.zCoord,
                locations,
                pipe.ownerUUID,
                pipe.ownerName);
            PacketHandler.INSTANCE.sendTo(message, (EntityPlayerMP) player);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer entityplayer) {
        TileGenericPipe tile = pipe.container;
        if (tile.getWorldObj()
            .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) return false;
        if (entityplayer.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) > 64) return false;
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int connectedPipesNew = connectedPipes;
        if (ticks % 20 == 0) { // reduce lag
            ticks = 0;
            Log.debug("Old connected: %d", connectedPipesNew);
            connectedPipesNew = TeleportManager.get()
                .getConnectedPipes(pipe, !isSendingPipe, isSendingPipe)
                .size();
            Log.debug("New connected: %d", connectedPipesNew);
        }
        ticks++;
        for (Object crafter : crafters) {
            if (freq != pipe.getFrequency()) {
                ((ICrafting) crafter).sendProgressBarUpdate(this, 0, pipe.getFrequency());
            }
            if (state != pipe.state) {
                ((ICrafting) crafter).sendProgressBarUpdate(this, 1, pipe.state);
            }
            if (connectedPipesNew != connectedPipes) {
                ((ICrafting) crafter).sendProgressBarUpdate(this, 2, connectedPipesNew);
            }
            if (isPublic != pipe.isPublic) {
                ((ICrafting) crafter).sendProgressBarUpdate(this, 3, pipe.isPublic ? 1 : 0);
            }
        }
        state = pipe.state;
        freq = pipe.getFrequency();
        isPublic = pipe.isPublic;
        connectedPipes = connectedPipesNew;
    }

    @Override
    public void updateProgressBar(int i, int j) {
        switch (i) {
            case 0:
                pipe.setFrequency(j);
                break;
            case 1:
                pipe.state = (byte) j;
                break;
            case 2:
                connectedPipes = j;
                break;
            case 3:
                pipe.isPublic = (j == 1);
                break;
        }
    }

}
