package com.mrfuzzihead.additionalpipes;

import net.minecraft.item.Item;
import net.minecraft.world.World;

import buildcraft.transport.ItemPipe;
import buildcraft.transport.Pipe;

public class MultiPlayerProxy {

    public void registerRendering() {}

    /**
     * Client-only world accessor used by client packets without linking {@code FMLClientHandler} on the server.
     */
    public World getClientWorld() {
        return null;
    }

    public void registerPipeRendering(Item res) {}

    public void createPipeSpecial(ItemPipe item, Class<? extends Pipe<?>> clas) {}
}
