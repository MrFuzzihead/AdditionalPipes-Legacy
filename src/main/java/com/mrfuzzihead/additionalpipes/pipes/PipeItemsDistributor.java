/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package com.mrfuzzihead.additionalpipes.pipes;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.gui.GuiHandler;

import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.pipes.events.PipeEventItem;

public class PipeItemsDistributor extends APPipe<PipeTransportItems> {

    public int distData[] = { 1, 1, 1, 1, 1, 1 };
    public int distSide = 0;
    public int curTick = Integer.MAX_VALUE;

    public PipeItemsDistributor(Item item) {
        super(new PipeTransportItems(), item);
    }

    @Override
    public int getIconIndex(net.minecraftforge.common.util.ForgeDirection connection) {
        switch (connection) {
            case DOWN: // -y
                return 10;
            case UP: // +y
                return 11;
            case NORTH: // -z
                return 12;
            case SOUTH: // +z
                return 13;
            case WEST: // -x
                return 14;
            case EAST: // +x
            default:
                return 9;
        }
    }

    public void eventHandler(PipeEventItem.FindDest event) {
        ItemStack stack = event.item.getItemStack();
        if (stack == null || stack.stackSize <= 0 || event.destinations.isEmpty()) {
            return;
        }

        boolean[] valid = new boolean[6];
        for (ForgeDirection direction : event.destinations) {
            valid[direction.ordinal()] = true;
        }

        if (curTick >= distData[distSide] || !valid[distSide]) {
            int next = DistributorRouting.nextValidSide(distData, distSide, valid);
            if (next < 0) {
                // No configured side is currently connected; let BuildCraft route/bounce normally.
                return;
            }
            distSide = next;
            curTick = 0;
        }

        curTick += stack.stackSize;
        event.destinations.clear();
        event.destinations.add(ForgeDirection.VALID_DIRECTIONS[distSide]);
    }

    @Override
    public boolean blockActivated(EntityPlayer player, ForgeDirection direction) {
        if (player.isSneaking()) {
            return false;
        }

        Item equipped = player.getCurrentEquippedItem() != null ? player.getCurrentEquippedItem()
            .getItem() : null;
        if (equipped != null) {
            if (APConfiguration.filterRightclicks && AdditionalPipes.isPipe(equipped)) {
                return false;
            }
        }

        if (player.worldObj.isRemote) return true;
        player.openGui(
            AdditionalPipes.instance,
            GuiHandler.PIPE_DIST,
            container.getWorldObj(),
            container.xCoord,
            container.yCoord,
            container.zCoord);

        return true;
    }

    public static int clampWeight(int value) {
        return DistributorRouting.clampWeight(value);
    }

    /**
     * Applies a weight to one side. Values are clamped to the supported range and an all-zero configuration is
     * normalized back to one item per side.
     *
     * @return false if the index is invalid.
     */
    public boolean setDistData(int index, int value) {
        if (index < 0 || index >= distData.length) {
            return false;
        }

        distData[index] = DistributorRouting.clampWeight(value);
        boolean found = false;
        for (int d : distData) {
            if (d > 0) {
                found = true;
                break;
            }
        }
        if (!found) {
            Arrays.fill(distData, 1);
        }
        return true;
    }

    private void sanityCheck() {
        for (int i = 0; i < distData.length; i++) {
            distData[i] = DistributorRouting.clampWeight(distData[i]);
        }
        for (int d : distData) {
            if (d > 0) {
                return;
            }
        }
        Arrays.fill(distData, 1);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("curTick", curTick);
        nbt.setInteger("distSide", distSide);
        for (int i = 0; i < distData.length; i++) {
            nbt.setInteger("distData" + i, distData[i]);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        curTick = Math.max(0, nbt.getInteger("curTick"));
        distSide = nbt.getInteger("distSide");
        if (distSide < 0 || distSide >= distData.length) {
            distSide = 0;
        }
        for (int i = 0; i < distData.length; i++) {
            distData[i] = nbt.getInteger("distData" + i);
        }
        sanityCheck();
    }

}
