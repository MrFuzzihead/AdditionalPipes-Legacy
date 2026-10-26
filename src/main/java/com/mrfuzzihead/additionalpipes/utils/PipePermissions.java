package com.mrfuzzihead.additionalpipes.utils;

import java.util.UUID;

/**
 * Pure permission rules for a teleport pipe, kept separate from {@code EntityPlayer} so they can be unit tested.
 */
public final class PipePermissions {

    private PipePermissions() {}

    public static boolean canModify(UUID owner, boolean isPublic, boolean creative, UUID player) {
        return isPublic || creative || (owner != null && owner.equals(player));
    }
}
