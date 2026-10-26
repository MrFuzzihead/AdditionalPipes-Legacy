package com.mrfuzzihead.additionalpipes.pipes;

import net.minecraft.item.ItemStack;

import buildcraft.transport.TravelingItem;

/**
 * Test-only TravelingItem that avoids {@code TravelingItem.make()}, which initializes FML's class loader and fails in
 * plain JUnit tests.
 */
final class TestTravelingItem extends TravelingItem {

    TestTravelingItem(ItemStack stack) {
        super(1);
        this.itemStack = stack.copy();
    }
}
