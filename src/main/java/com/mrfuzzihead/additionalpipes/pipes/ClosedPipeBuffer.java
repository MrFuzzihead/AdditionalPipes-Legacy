package com.mrfuzzihead.additionalpipes.pipes;

import net.minecraft.item.ItemStack;

/**
 * Fixed-size FIFO overflow buffer used by {@link PipeItemsClosed}.
 *
 * <p>
 * Items are merged into existing stacks and then placed in empty slots. Only when the buffer is genuinely full is the
 * oldest slot evicted; the incoming item is always retained. This avoids the old behaviour where a full buffer could
 * destroy both the oldest item and the incoming item.
 */
final class ClosedPipeBuffer {

    static final int DEFAULT_SIZE = 9;

    private final ItemStack[] slots;

    ClosedPipeBuffer() {
        this(DEFAULT_SIZE);
    }

    ClosedPipeBuffer(int size) {
        slots = new ItemStack[Math.max(0, size)];
    }

    int size() {
        return slots.length;
    }

    ItemStack get(int index) {
        return slots[index];
    }

    void set(int index, ItemStack stack) {
        slots[index] = stack;
    }

    ItemStack remove(int index) {
        ItemStack stack = slots[index];
        slots[index] = null;
        return stack;
    }

    void clear() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = null;
        }
    }

    boolean isEmpty() {
        for (ItemStack slot : slots) {
            if (slot != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inserts up to one stack, merging into existing stacks and using empty slots first. If the buffer is full, the
     * oldest slot is evicted to make room for the incoming item.
     */
    void insert(ItemStack stack) {
        if (slots.length == 0 || stack == null || stack.stackSize <= 0) {
            return;
        }

        ItemStack remaining = stack.copy();
        int limit = 64;

        // Merge into partially filled matching stacks first.
        for (int i = 0; i < slots.length && remaining.stackSize > 0; i++) {
            ItemStack slot = slots[i];
            if (slot != null && slot.isItemEqual(remaining) && ItemStack.areItemStackTagsEqual(slot, remaining)) {
                int space = Math.min(slot.getMaxStackSize(), limit) - slot.stackSize;
                if (space > 0) {
                    int moved = Math.min(space, remaining.stackSize);
                    slot.stackSize += moved;
                    remaining.stackSize -= moved;
                }
            }
        }

        // Then use empty slots.
        for (int i = 0; i < slots.length && remaining.stackSize > 0; i++) {
            if (slots[i] == null) {
                int moved = Math.min(Math.min(remaining.getMaxStackSize(), limit), remaining.stackSize);
                ItemStack copy = remaining.copy();
                copy.stackSize = moved;
                slots[i] = copy;
                remaining.stackSize -= moved;
            }
        }

        // Still full: evict the oldest stack, shift the rest left, and put the remainder in the freed slot.
        while (remaining.stackSize > 0) {
            for (int i = 1; i < slots.length; i++) {
                slots[i - 1] = slots[i];
            }
            int moved = Math.min(Math.min(remaining.getMaxStackSize(), limit), remaining.stackSize);
            ItemStack copy = remaining.copy();
            copy.stackSize = moved;
            slots[slots.length - 1] = copy;
            remaining.stackSize -= moved;
        }
    }
}
