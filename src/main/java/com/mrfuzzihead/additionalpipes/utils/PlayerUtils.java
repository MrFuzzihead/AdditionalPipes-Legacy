package com.mrfuzzihead.additionalpipes.utils;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerUtils {

    /**
     * Get a player's UUID.
     * Calling this is easier than the more complicated default syntax.
     *
     * @param player
     */
    public static UUID getUUID(EntityPlayer player) {
        // getUUIDFromProfile()
        return EntityPlayer.func_146094_a(player.getGameProfile());
    }

    /**
     * Matches the 8-block reach check used by the pipe containers.
     */
    public static boolean isPlayerNearBlock(EntityPlayer player, int x, int y, int z) {
        return player != null && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
    }
}
