/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package com.mrfuzzihead.additionalpipes.pipes;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.utils.Log;
import com.mrfuzzihead.additionalpipes.utils.TeleportTag;

import buildcraft.api.core.Position;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.pipes.events.PipeEventItem;
import buildcraft.transport.utils.TransportUtils;

public class PipeItemsTeleport extends PipeTeleport<PipeTransportItems> {

    private static final int ICON = 0;

    public PipeItemsTeleport(Item items) {
        super(new PipeTransportItems(), items, PipeType.ITEMS);
    }

    public void eventHandler(PipeEventItem.Entered event) {
        if (getWorld().isRemote) {
            return;
        }

        if (event.item.hasExtraData() && TeleportTag.consume(event.item.getExtraData())) {
            // this was sent to us by another teleport pipe
            return;
        }

        List<PipeItemsTeleport> connectedTeleportPipes = TeleportManager.get()
            .getConnectedPipes(this, false, true);

        // no teleport pipes connected, use default
        if (connectedTeleportPipes.size() <= 0 || (state & 0x1) == 0) {
            return;
        }

        // output to random pipe
        LinkedList<ForgeDirection> outputOrientations = new LinkedList<ForgeDirection>();
        PipeTeleport<?> otherPipe;

        int originalPipeNumber = rand.nextInt(connectedTeleportPipes.size());
        int currentPipeNumber = originalPipeNumber;

        boolean found = false;
        int numberOfTries = 0;

        // find a pipe with something connected to it
        // The logic for this is... pretty complicated, actually.
        do {
            ++numberOfTries;
            otherPipe = connectedTeleportPipes.get(currentPipeNumber);

            for (ForgeDirection o : ForgeDirection.VALID_DIRECTIONS) {
                if (otherPipe.outputOpen(o)) {
                    outputOrientations.add(o);
                }
            }

            // no outputs found, try again
            if (outputOrientations.size() <= 0) {
                ++currentPipeNumber;

                // loop back to the start
                if (currentPipeNumber >= connectedTeleportPipes.size()) {
                    currentPipeNumber = 0;
                }
            } else {
                found = true;
            }
        } while (numberOfTries < connectedTeleportPipes.size() && !found);

        // couldn't find any, so give up
        if (!found) {
            return;
        }

        Position insertPoint = otherPipe.getPosition();
        insertPoint.x += 0.5;
        insertPoint.y += TransportUtils.getPipeFloorOf(event.item.getItemStack());
        insertPoint.z += 0.5;
        insertPoint.moveForwards(0.5);
        event.item.setPosition(insertPoint.x, insertPoint.y, insertPoint.z);

        // add the marker to the item to let the receiving pipe know not to send the item back
        TeleportTag.mark(event.item.getExtraData(), getFrequency());

        ForgeDirection newOrientation = otherPipe.getOpenOrientation()
            .getOpposite();
        ((PipeTransportItems) otherPipe.transport).injectItem(event.item, newOrientation);

        Log.debug("%s from %s to %s %s", event.item, getPosition(), otherPipe.getPosition(), newOrientation);
        event.cancelled = true;
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return ICON;
    }

}
