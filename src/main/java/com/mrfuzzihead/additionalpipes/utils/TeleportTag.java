package com.mrfuzzihead.additionalpipes.utils;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Marks a travelling item so a receiving teleport pipe does not immediately teleport it again.
 *
 * <p>
 * The marker lives on the {@code TravelingItem}'s extra data, not on the item stack, so it cannot collide with item
 * NBT and it disappears when the item is finally delivered.
 */
public final class TeleportTag {

    public static final String KEY = "justTeleported";

    private TeleportTag() {}

    public static boolean isMarked(NBTTagCompound tag) {
        return tag != null && tag.hasKey(KEY);
    }

    /**
     * @return true if the tag was present; the marker is removed as a side effect.
     */
    public static boolean consume(NBTTagCompound tag) {
        if (!isMarked(tag)) {
            return false;
        }
        tag.removeTag(KEY);
        return true;
    }

    public static void mark(NBTTagCompound tag, int frequency) {
        tag.setInteger(KEY, frequency);
    }
}
