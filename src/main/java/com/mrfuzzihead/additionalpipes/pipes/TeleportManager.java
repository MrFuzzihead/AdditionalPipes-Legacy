package com.mrfuzzihead.additionalpipes.pipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.api.ITeleportPipe;
import com.mrfuzzihead.additionalpipes.api.PipeType;
import com.mrfuzzihead.additionalpipes.api.TeleportManagerBase;
import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.PipeTransportPower;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class TeleportManager extends TeleportManagerBase {

    private final Map<PipeType, Multimap<Integer, ITeleportPipe>> channels;

    public final Map<Integer, String> frequencyNames;

    public TeleportManager() {
        channels = new EnumMap<PipeType, Multimap<Integer, ITeleportPipe>>(PipeType.class);
        for (PipeType type : PipeType.values()) {
            channels.put(type, LinkedListMultimap.<Integer, ITeleportPipe>create());
        }
        frequencyNames = new HashMap<Integer, String>();
    }

    /**
     * Returns the manager for the current server. The instance is owned by
     * {@link com.mrfuzzihead.additionalpipes.AdditionalPipes}
     * and published through {@link TeleportManagerBase#INSTANCE}; it is replaced between server runs.
     */
    public static TeleportManager get() {
        TeleportManager manager = getOrNull();
        if (manager == null) {
            throw new IllegalStateException("Teleport manager is not initialized");
        }
        return manager;
    }

    /**
     * Returns the current manager, or {@code null} between server runs. Lifecycle code (pipe invalidation during a
     * server stop) uses this to avoid throwing while the manager has already been cleared.
     */
    public static TeleportManager getOrNull() {
        TeleportManagerBase manager = TeleportManagerBase.INSTANCE;
        return manager instanceof TeleportManager ? (TeleportManager) manager : null;
    }

    /**
     * Get the pipes in a channel (a channel consists of a frequency and a type).
     */
    public Collection<ITeleportPipe> getPipesInChannel(int frequency, PipeType type) {
        Multimap<Integer, ITeleportPipe> channel = channels.get(type);
        if (channel == null) {
            return java.util.Collections.emptyList();
        }
        return channel.get(frequency);
    }

    @Override
    public void add(ITeleportPipe pipe, int frequency) {
        if (isClientSide()) {
            return;
        }
        addInternal(pipe, frequency);
        logPipeChange("added", pipe, frequency);
    }

    void addInternal(ITeleportPipe pipe, int frequency) {
        Multimap<Integer, ITeleportPipe> channel = channels.get(pipe.getType());
        if (channel != null && !channel.containsEntry(frequency, pipe)) {
            channel.put(frequency, pipe);
        }
    }

    @Override
    public void remove(ITeleportPipe pipe, int frequency) {
        if (isClientSide()) {
            return;
        }
        removeInternal(pipe, frequency);
        logPipeChange("removed", pipe, frequency);
    }

    void removeInternal(ITeleportPipe pipe, int frequency) {
        Multimap<Integer, ITeleportPipe> channel = channels.get(pipe.getType());
        if (channel != null) {
            channel.remove(frequency, pipe);
        }
    }

    private static boolean isClientSide() {
        return FMLCommonHandler.instance()
            .getEffectiveSide() == Side.CLIENT;
    }

    private void logPipeChange(String action, ITeleportPipe pipe, int frequency) {
        // if unit tests are being run, pipe.container will be null.
        if (!APConfiguration.enableDebugLog || pipe.getContainer() == null) {
            return;
        }
        Log.debug(
            "[TeleportManager] Pipe %s: %s @ (%.1f, %.1f, %.1f), %d pipes in channel",
            action,
            pipe.getType()
                .toString()
                .toLowerCase(),
            pipe.getPosition().x,
            pipe.getPosition().y,
            pipe.getPosition().z,
            getPipesInChannel(frequency, pipe.getType()).size());
    }

    @Override
    public void reset() {
        clearInternal();
        Log.info("Reset teleport manager.");
    }

    void clearInternal() {
        for (Multimap<Integer, ITeleportPipe> channel : channels.values()) {
            channel.clear();
        }
        frequencyNames.clear();
    }

    /**
     * Get pipes connected to the provided one. (template function version)
     *
     * @param pipe
     * @param includeSend    whether or not to return connected pipes that send stuff.
     * @param includeReceive whether or not to return connected pipes that receive stuff.
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends ITeleportPipe> ArrayList<T> getConnectedPipes(T pipe, boolean includeSend,
        boolean includeReceive) {
        Collection<T> channel = (Collection<T>) (Collection<?>) getPipesInChannel(pipe.getFrequency(), pipe.getType());

        ArrayList<T> connected = new ArrayList<T>();

        for (T other : channel) {
            if (other.getContainer() != null && other.getContainer()
                .isInvalid()) {
                continue;
            }

            // pipe is open or includeReceive &&
            // both public or same owner
            if (pipe != other) {
                if ((other.canReceive() && includeReceive) || (other.canSend() && includeSend)) {
                    if (pipe.isPublic() ? other.isPublic()
                        : (other.getOwnerUUID() != null && other.getOwnerUUID()
                            .equals(pipe.getOwnerUUID()))) {
                        connected.add(other);
                    }
                }
            }

        }
        return connected;
    }

    public Collection<PipeTeleport<PipeTransportItems>> getAllItemPipesInNetwork() {
        return getAll(PipeType.ITEMS);
    }

    public Collection<PipeTeleport<PipeTransportFluids>> getAllFluidPipesInNetwork() {
        return getAll(PipeType.FLUIDS);
    }

    public Collection<PipeTeleport<PipeTransportPower>> getAllPowerPipesInNetwork() {
        return getAll(PipeType.POWER);
    }

    public Collection<PipeTeleport<PipeTransportItemsLogistics>> getAllLogisticsPipesInNetwork() {
        return getAll(PipeType.LOGISTICS);
    }

    @SuppressWarnings("unchecked")
    private <T extends ITeleportPipe> Collection<T> getAll(PipeType type) {
        return (Collection<T>) (Collection<?>) channels.get(type)
            .values();
    }

    /**
     * Get the name of the provided frequency.
     *
     * @param frequency
     * @return the name of the frequency, or an empty string if it has no name.
     */
    @Override
    public String getFrequencyName(int frequency) {
        String name = frequencyNames.get(frequency);
        return name == null ? "" : name;
    }

    @Override
    public void setFrequencyName(int frequency, String name) {
        frequencyNames.put(frequency, name);
    }

}
