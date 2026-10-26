package com.mrfuzzihead.additionalpipes;

import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.transport.ItemPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.TransportProxyClient;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MultiPlayerProxyClient extends MultiPlayerProxy {

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance()
            .getClient().theWorld;
    }

    @Override
    public void registerPipeRendering(Item res) {
        MinecraftForgeClient.registerItemRenderer(res, TransportProxyClient.pipeItemRenderer);
    }

    @Override
    public void createPipeSpecial(ItemPipe item, Class<? extends Pipe<?>> clas) {
        try {
            Pipe<?> dummyPipe = clas.getConstructor(Item.class)
                .newInstance(item);
            if (dummyPipe != null) {
                item.setPipesIcons(dummyPipe.getIconProvider());
                item.setPipeIconIndex(dummyPipe.getIconIndexForItem());
            }
        } catch (Exception e) {
            Log.error("MultiPlayerProxyClient.createPipeSpecial() failed", e);
        }
    }
}
