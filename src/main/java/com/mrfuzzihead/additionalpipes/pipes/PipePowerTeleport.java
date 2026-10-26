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
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.api.transport.IPipeTile;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.IPipeTransportPowerHook;
import buildcraft.transport.PipeTransportPower;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipePowerDiamond;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;

public class PipePowerTeleport extends PipeTeleport<PipeTransportPower> implements IPipeTransportPowerHook {

    private static final int ICON = 3;

    private static class PowerRequest {

        public final TileEntity tile;
        public final ForgeDirection orientation;
        public final boolean isPipe;
        public final int demand;

        public PowerRequest(TileEntity te, ForgeDirection o, int demand) {
            tile = te;
            orientation = o;
            isPipe = te instanceof IPipeTile;
            this.demand = Math.max(0, demand);
        }
    }

    public PipePowerTeleport(Item item) {
        super(new PipeTransportPower(), item, PipeType.POWER);
        ((PipeTransportPower) transport).initFromPipe(PipePowerDiamond.class);
    }

    @Override
    public int requestEnergy(ForgeDirection from, int value) {
        int requested = 0;

        if ((state & 0x2) == 0 || value <= 0) { // No need to waste CPU
            return requested;
        }

        List<PipePowerTeleport> pipeList = TeleportManager.get()
            .getConnectedPipes(this, true, false);

        if (pipeList.size() <= 0) {
            return requested;
        }

        for (PipeTeleport<?> pipe : pipeList) {
            LinkedList<ForgeDirection> possibleMovements = getConnectedPipes(pipe);
            for (ForgeDirection orientation : possibleMovements) {
                TileEntity tile = pipe.container.getTile(orientation);
                if (!(tile instanceof TileGenericPipe)) {
                    continue;
                }

                TileGenericPipe adjacentTile = (TileGenericPipe) tile;
                if (adjacentTile.pipe == null || !(adjacentTile.pipe.transport instanceof PipeTransportPower)) {
                    continue;
                }

                PipeTransportPower nearbyTransport = (PipeTransportPower) adjacentTile.pipe.transport;
                ForgeDirection requestSide = orientation.getOpposite();
                nearbyTransport.requestEnergy(requestSide, value);
                long total = (long) requested + Math.max(0, nearbyTransport.nextPowerQuery[requestSide.ordinal()]);
                requested = (int) Math.min(Integer.MAX_VALUE, total);
            }
        }
        return requested;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int energy) {
        if (energy <= 0 || (state & 0x1) == 0) {
            return 0;
        }

        List<PipePowerTeleport> connectedPipes = TeleportManager.get()
            .getConnectedPipes(this, false, true);
        if (connectedPipes.isEmpty()) {
            return 0;
        }

        List<PowerRequest> requests = new LinkedList<PowerRequest>();
        for (PipePowerTeleport receiver : connectedPipes) {
            requests.addAll(getPowerRequestsByNeighbors(receiver));
        }

        if (requests.isEmpty()) {
            Log.debug("No pipes want power on channel %d", getFrequency());
            return 0;
        }

        int budget = (int) Math.floor(APConfiguration.powerTransmittanceCfg * (double) energy);
        if (budget <= 0) {
            return 0;
        }

        int[] demands = new int[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            demands[i] = requests.get(i).demand;
        }

        int[] allocations = PowerAllocation.allocate(budget, demands);
        int acceptedTotal = 0;

        for (int i = 0; i < requests.size(); i++) {
            int allocation = allocations[i];
            if (allocation <= 0) {
                continue;
            }

            PowerRequest powerEntry = requests.get(i);
            if (powerEntry.isPipe) {
                PipeTransportPower nearbyTransport = (PipeTransportPower) (((TileGenericPipe) powerEntry.tile).pipe.transport);
                double accepted = nearbyTransport.receiveEnergy(powerEntry.orientation, allocation);
                acceptedTotal += Math.min(allocation, Math.max(0, (int) accepted));
            } else if (powerEntry.tile instanceof IEnergyHandler) {
                IEnergyHandler handler = (IEnergyHandler) powerEntry.tile;
                if (handler.canConnectEnergy(powerEntry.orientation.getOpposite())) {
                    acceptedTotal += Math.min(
                        allocation,
                        Math.max(0, handler.receiveEnergy(powerEntry.orientation.getOpposite(), allocation, false)));
                }
            } else if (powerEntry.tile instanceof IEnergyReceiver) {
                IEnergyReceiver handler = (IEnergyReceiver) powerEntry.tile;
                if (handler.canConnectEnergy(powerEntry.orientation.getOpposite())) {
                    acceptedTotal += Math.min(
                        allocation,
                        Math.max(0, handler.receiveEnergy(powerEntry.orientation.getOpposite(), allocation, false)));
                }
            } else {
                Log.error("Don't know how to transmit power to tile " + powerEntry.tile);
            }
        }

        return Math.min(energy, acceptedTotal);
    }

    private List<PowerRequest> getPowerRequestsByNeighbors(PipePowerTeleport pipe) {
        List<PowerRequest> needsPower = new LinkedList<PowerRequest>();

        for (ForgeDirection orientation : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity tile = pipe.container.getTile(orientation);
            if (tile instanceof TileGenericPipe) {
                TileGenericPipe adjacentPipe = (TileGenericPipe) tile;
                int demand = getPowerDemand(adjacentPipe);
                if (demand > 0) {
                    needsPower.add(new PowerRequest(adjacentPipe, orientation.getOpposite(), demand));
                }
            } else if (tile != null) {
                int demand = getPowerRequestedByTileEntity(tile, orientation.getOpposite());
                if (demand > 0) {
                    needsPower.add(new PowerRequest(tile, orientation.getOpposite(), demand));
                }
            }
        }

        return needsPower;
    }

    private static int getPowerDemand(TileGenericPipe tile) {
        if (tile.pipe == null || !(tile.pipe.transport instanceof PipeTransportPower)) {
            return 0;
        }

        PipeTransportPower powerTransport = (PipeTransportPower) tile.pipe.transport;
        long demand = 0;
        for (int query : powerTransport.nextPowerQuery) {
            if (query > 0) {
                demand += query;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, demand);
    }

    private int getPowerRequestedByTileEntity(TileEntity tile, ForgeDirection sideOnTile) {
        int power = 0;

        if (tile instanceof IEnergyHandler) {
            IEnergyHandler handler = (IEnergyHandler) tile;
            if (handler.canConnectEnergy(sideOnTile)) {
                power = handler.receiveEnergy(sideOnTile, transport.maxPower, true);
            }
        } else if (tile instanceof IEnergyReceiver) {
            IEnergyReceiver handler = (IEnergyReceiver) tile;
            if (handler.canConnectEnergy(sideOnTile)) {
                power = handler.receiveEnergy(sideOnTile, transport.maxPower, true);
            }
        }

        return power;
    }

    // returns all adjacent pipes and tile entities
    private LinkedList<ForgeDirection> getConnectedPipes(PipeTeleport<?> pipe) {
        LinkedList<ForgeDirection> result = new LinkedList<ForgeDirection>();

        for (ForgeDirection orientation : ForgeDirection.VALID_DIRECTIONS) {
            if (pipe.outputOpen(orientation)) {
                TileEntity te = pipe.container.getTile(orientation);
                if ((te instanceof IPipeTile) && Utils.checkPipesConnections(pipe.container, te)) {
                    result.add(orientation);
                }
            }
        }

        return result;
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return ICON;
    }

}
