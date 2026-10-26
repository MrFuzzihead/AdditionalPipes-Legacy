/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package com.mrfuzzihead.additionalpipes.pipes;

import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.utils.InventoryUtils;

import buildcraft.core.lib.inventory.ITransactor;
import buildcraft.core.lib.inventory.Transactor;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.pipes.events.PipeEventItem;

public class PipeItemsAddition extends APPipe<PipeTransportItems> {

    // re-use old Redstone Pipe texture
    private static final int ICON = 4;

    public PipeItemsAddition(Item item) {
        super(new PipeTransportItems(), item);
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return ICON;
    }

    public void eventHandler(PipeEventItem.FindDest event) {
        ItemStack stack = event.item.getItemStack();
        if (stack == null) {
            return;
        }

        LinkedList<ForgeDirection> newOris = new LinkedList<ForgeDirection>();
        Iterator<ForgeDirection> iterator = event.destinations.iterator();

        while (iterator.hasNext()) {
            ForgeDirection orientation = iterator.next();
            TileEntity entity = container.getTile(orientation);
            if (entity instanceof IInventory) {
                IInventory inventory = (IInventory) entity;

                if (InventoryUtils.containsItem(true, false, stack, inventory)) {
                    ITransactor transactor = Transactor.getTransactorFor(entity);
                    if (transactor.add(stack, orientation.getOpposite(), false).stackSize > 0) {
                        newOris.add(orientation);
                    }
                } else {
                    iterator.remove();
                }
            }
        }

        if (!newOris.isEmpty()) {
            event.destinations.clear();
            event.destinations.addAll(newOris);
        }
    }

}
