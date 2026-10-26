package com.mrfuzzihead.additionalpipes.network;

import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.network.message.MessageDistPipe;
import com.mrfuzzihead.additionalpipes.network.message.MessageTelePipeData;
import com.mrfuzzihead.additionalpipes.network.message.MessageTelePipeUpdate;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE
        .newSimpleChannel(AdditionalPipes.MODID.toLowerCase());

    public static void init() {
        INSTANCE.registerMessage(MessageDistPipe.class, MessageDistPipe.class, 0, Side.SERVER);
        INSTANCE.registerMessage(MessageTelePipeUpdate.class, MessageTelePipeUpdate.class, 1, Side.SERVER);
        INSTANCE.registerMessage(MessageTelePipeData.class, MessageTelePipeData.class, 3, Side.CLIENT);

    }
}
