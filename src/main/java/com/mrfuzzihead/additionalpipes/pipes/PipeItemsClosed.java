package com.mrfuzzihead.additionalpipes.pipes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.gui.GuiHandler;

import buildcraft.core.lib.inventory.InvUtils;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.pipes.events.PipeEventItem;

public class PipeItemsClosed extends APPipe<PipeTransportItems> implements IInventory {

    private final ClosedPipeBuffer buffer = new ClosedPipeBuffer();

    public PipeItemsClosed(Item item) {
        super(new PipeTransportItems(), item);
    }

    @Override
    public boolean blockActivated(EntityPlayer player, ForgeDirection direction) {
        ItemStack equippedItem = player.getCurrentEquippedItem();
        if (equippedItem != null && AdditionalPipes.isPipe(equippedItem.getItem())) {
            return false;
        }
        if (player.worldObj.isRemote) return true;
        player.openGui(
            AdditionalPipes.instance,
            GuiHandler.PIPE_CLOSED,
            getWorld(),
            container.xCoord,
            container.yCoord,
            container.zCoord);
        return true;
    }

    @Override
    public void dropContents() {
        super.dropContents();
        InvUtils.dropItems(getWorld(), this, container.xCoord, container.yCoord, container.zCoord);
        InvUtils.wipeInventory(this);
    }

    public void eventHandler(PipeEventItem.DropItem event) {
        ItemStack stack = event.item.getItemStack();
        if (stack != null && stack.stackSize > 0) {
            buffer.insert(stack);
            stack.stackSize = 0;
            markDirty();
            if (container != null) {
                container.scheduleRenderUpdate();
            }
        }

        // Suppress the world drop in either case.
        event.entity = null;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        NBTTagList list = new NBTTagList();

        for (int i = 0; i < buffer.size(); i++) {
            ItemStack stack = buffer.get(i);
            if (stack != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                stack.writeToNBT(stackTag);
                list.appendTag(stackTag);
            }
        }

        nbttagcompound.setTag("closedInventory", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        buffer.clear();

        NBTTagList list = nbttagcompound.getTagList("closedInventory", 10);
        for (int i = 0; i < list.tagCount() && i < buffer.size(); i++) {
            NBTTagCompound stackTag = (NBTTagCompound) list.getCompoundTagAt(i);
            buffer.set(i, ItemStack.loadItemStackFromNBT(stackTag));
        }
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return 18 + (getStackInSlot(0) == null ? 0 : 1);
    }

    @Override
    public int getSizeInventory() {
        return buffer.size();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return buffer.get(i);
    }

    @Override
    public ItemStack decrStackSize(int i, int amt) {
        ItemStack stack = buffer.get(i);
        if (stack == null) {
            return null;
        }

        ItemStack split = stack.splitStack(amt);
        if (stack.stackSize <= 0) {
            buffer.set(i, null);
        }
        markDirty();
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        ItemStack stack = buffer.remove(i);
        if (stack != null) {
            markDirty();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack stack) {
        buffer.set(i, stack);
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "pipeItemsClosed";
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer var1) {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public void markDirty() {
        if (container != null && container.getWorldObj() != null && !container.getWorldObj().isRemote) {
            container.markDirty();
        }
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}
}
