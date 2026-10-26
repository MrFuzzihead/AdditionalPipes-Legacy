package com.mrfuzzihead.additionalpipes;

import java.io.File;
import java.io.IOException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;

import com.mrfuzzihead.additionalpipes.api.TeleportManagerBase;
import com.mrfuzzihead.additionalpipes.gates.GateProvider;
import com.mrfuzzihead.additionalpipes.gates.TriggerPipeClosed;
import com.mrfuzzihead.additionalpipes.gui.GuiHandler;
import com.mrfuzzihead.additionalpipes.network.PacketHandler;
import com.mrfuzzihead.additionalpipes.pipes.TeleportManager;
import com.mrfuzzihead.additionalpipes.selftest.SelfTest;
import com.mrfuzzihead.additionalpipes.textures.Textures;
import com.mrfuzzihead.additionalpipes.utils.Log;

import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementManager;
import buildcraft.core.BCCreativeTab;
import buildcraft.transport.BlockGenericPipe;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(
    modid = AdditionalPipes.MODID,
    name = AdditionalPipes.NAME,
    dependencies = "required-after:BuildCraft|Transport;required-after:BuildCraft|Silicon;after:BuildCraft|Factory;after:LogisticsPipes",
    version = Tags.VERSION)
public class AdditionalPipes {

    public static final String MODID = "additionalpipes";
    public static final String NAME = "Additional Pipes";

    @Instance(MODID)
    public static AdditionalPipes instance;

    @SidedProxy(
        clientSide = "com.mrfuzzihead.additionalpipes.MultiPlayerProxyClient",
        serverSide = "com.mrfuzzihead.additionalpipes.MultiPlayerProxy")
    public static MultiPlayerProxy proxy;

    public File configFile;

    public boolean logisticsPipesInstalled;

    public BCCreativeTab creativeTab;

    // teleport scanner TODO
    // public Item teleportScanner;

    // Redstone Liquid
    public Item pipeLiquidsRedstone;
    // Redstone
    public Item pipeItemsRedStone;
    // Addition
    public Item pipeItemsAddition;
    // Gravity Feed
    public Item pipeItemsGravityFeed;
    // Distributor
    public Item pipeItemsDistributor;

    // Item Teleport
    public Item pipeItemsTeleport;
    // Liquid Teleport
    public Item pipeLiquidsTeleport;
    // Power Teleport
    public Item pipePowerTeleport;
    // Logistics Teleport
    public Item pipeLogisticsTeleport;
    // Items Closed
    public Item pipeItemsClosed;
    // Switch pipes
    public Item pipePowerSwitch;
    public Item pipeItemsSwitch;
    public Item pipeLiquidsSwitch;
    // water pump pipe
    public Item pipeLiquidsWaterPump;

    public ITriggerInternal triggerPipeClosed;

    public TeleportManager teleportManager;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        PacketHandler.init();

        configFile = event.getSuggestedConfigurationFile();
        APConfiguration.loadConfigs(false, configFile);
        MinecraftForge.EVENT_BUS.register(this);

        // create BuildCraft creative tab
        creativeTab = new BCCreativeTab("apcreativetab");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

        logisticsPipesInstalled = Loader.isModLoaded("LogisticsPipes");

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        proxy.registerRendering();

        APConfiguration.loadConfigs(true, configFile);

        teleportManager = new TeleportManager();
        TeleportManagerBase.INSTANCE = teleportManager;

        Log.info("Registering pipes");
        loadPipes();

        // set creative tab icon
        creativeTab.setIcon(new ItemStack(pipeItemsTeleport));

        triggerPipeClosed = new TriggerPipeClosed();
        StatementManager.registerTriggerProvider(new GateProvider());

        RecipeRegistry.registerRemovalRecipes(this);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LPCompat.register(logisticsPipesInstalled);
    }

    @EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAdditionalPipes());
        if (teleportManager == null) {
            teleportManager = new TeleportManager();
        }
        TeleportManagerBase.INSTANCE = teleportManager;
        teleportManager.reset();
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (teleportManager != null) {
            teleportManager.reset();
        }
        TeleportManagerBase.INSTANCE = null;
        teleportManager = null;
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (Boolean.getBoolean("additionalpipes.selftest")) {
            SelfTest.run();
        }
    }

    private void loadPipes() {
        PipeRegistry.loadPipes(this);
    }

    // legacy method
    public static boolean isPipe(Item item) {
        if (item != null && BlockGenericPipe.pipes.containsKey(item)) {
            return true;
        }

        return false;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void textureHook(TextureStitchEvent.Pre event) throws IOException {
        Textures.registerIcons(event.map, event.map.getTextureType());
    }
}
