package com.mrfuzzihead.additionalpipes;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.additionalpipes.pipes.PipeItemsAddition;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsClosed;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsDistributor;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsGravityFeed;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsTeleport;
import com.mrfuzzihead.additionalpipes.pipes.PipeLiquidsTeleport;
import com.mrfuzzihead.additionalpipes.pipes.PipeLiquidsWaterPump;
import com.mrfuzzihead.additionalpipes.pipes.PipeLogisticsTeleport;
import com.mrfuzzihead.additionalpipes.pipes.PipePowerTeleport;
import com.mrfuzzihead.additionalpipes.pipes.PipeSwitchFluids;
import com.mrfuzzihead.additionalpipes.pipes.PipeSwitchItems;
import com.mrfuzzihead.additionalpipes.pipes.PipeSwitchPower;
import com.mrfuzzihead.additionalpipes.utils.PipeCreator;

import buildcraft.BuildCraftSilicon;
import buildcraft.BuildCraftTransport;
import buildcraft.core.recipes.AssemblyRecipeManager;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.PipeTransportPower;
import buildcraft.transport.pipes.PipeFluidsDiamond;
import buildcraft.transport.pipes.PipeFluidsGold;
import buildcraft.transport.pipes.PipePowerDiamond;
import buildcraft.transport.pipes.PipePowerGold;
import buildcraft.transport.pipes.PipePowerIron;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Creates and registers the mod's pipe items and their crafting/assembly recipes.
 */
final class PipeRegistry {

    private PipeRegistry() {}

    static void loadPipes(AdditionalPipes mod) {
        // Item Teleport Pipe
        mod.pipeItemsTeleport = PipeCreator.createPipe(PipeItemsTeleport.class);

        AssemblyRecipeManager.INSTANCE.addRecipe(
            "teleportPipe",
            10000,
            new ItemStack(mod.pipeItemsTeleport, 8),
            new Object[] { new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 4),
                new ItemStack(BuildCraftTransport.pipeItemsDiamond, 8),
                new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3) });

        // Liquid Teleport Pipe
        PipeTransportFluids.fluidCapacities
            .put(PipeLiquidsTeleport.class, PipeTransportFluids.fluidCapacities.get(PipeFluidsDiamond.class));
        mod.pipeLiquidsTeleport = PipeCreator.createPipe(PipeLiquidsTeleport.class);
        if (mod.pipeItemsTeleport != null) {
            GameRegistry.addShapelessRecipe(
                new ItemStack(mod.pipeLiquidsTeleport),
                new Object[] { BuildCraftTransport.pipeWaterproof, mod.pipeItemsTeleport });
        }

        // Power Teleport Pipe
        PipeTransportPower.powerCapacities
            .put(PipePowerTeleport.class, PipeTransportPower.powerCapacities.get(PipePowerDiamond.class));
        mod.pipePowerTeleport = PipeCreator.createPipe(PipePowerTeleport.class);
        if (mod.pipeItemsTeleport != null) {
            GameRegistry.addShapelessRecipe(
                new ItemStack(mod.pipePowerTeleport),
                new Object[] { Items.redstone, mod.pipeItemsTeleport });
        }

        if (mod.logisticsPipesInstalled) {
            // Logistics Teleport Pipe
            mod.pipeLogisticsTeleport = PipeCreator.createPipe(PipeLogisticsTeleport.class);
            if (mod.pipeItemsTeleport != null) {
                GameRegistry.addShapelessRecipe(
                    new ItemStack(mod.pipeLogisticsTeleport),
                    new Object[] { mod.pipeItemsTeleport, BuildCraftSilicon.redstoneChipset });
            }
        }

        // Distributor Pipe
        mod.pipeItemsDistributor = PipeCreator.createPipeAndRecipe(
            1,
            PipeItemsDistributor.class,
            false,
            " r ",
            "IgI",
            'r',
            "dustRedstone",
            'I',
            "ingotIron",
            'g',
            "blockGlass");

        // Addition Pipe
        mod.pipeItemsAddition = PipeCreator.createPipeAndRecipe(
            1,
            PipeItemsAddition.class,
            false,
            " R ",
            "RIR",
            " R ",
            'I',
            BuildCraftTransport.pipeItemsClay,
            'R',
            "dustRedstone");

        // Gravity Feed Pipe
        mod.pipeItemsGravityFeed = PipeCreator.createPipeAndRecipe(
            1,
            PipeItemsGravityFeed.class,
            false,
            "   ",
            "IgI",
            " I ",
            'I',
            "ingotIron",
            'g',
            "blockGlass");

        // Closed Items Pipe
        mod.pipeItemsClosed = PipeCreator
            .createPipeAndRecipe(1, PipeItemsClosed.class, true, BuildCraftTransport.pipeItemsVoid, "gearIron");

        // switch pipes
        mod.pipeItemsSwitch = PipeCreator.createPipeAndRecipe(
            8,
            PipeSwitchItems.class,
            false,
            "GgI",
            'g',
            "blockGlass",
            'G',
            "gearGold",
            'I',
            "gearIron");

        // set power capacity to the average between iron and gold
        int switchPowerCapacity = (PipeTransportPower.powerCapacities.get(PipePowerGold.class)
            + PipeTransportPower.powerCapacities.get(PipePowerIron.class)) / 2;

        PipeTransportPower.powerCapacities.put(PipeSwitchPower.class, switchPowerCapacity);
        mod.pipePowerSwitch = PipeCreator
            .createPipeAndRecipe(1, PipeSwitchPower.class, true, mod.pipeItemsSwitch, "dustRedstone");

        int switchFluidCapacity = PipeTransportFluids.fluidCapacities.get(PipeFluidsGold.class);

        PipeTransportFluids.fluidCapacities.put(PipeSwitchFluids.class, switchFluidCapacity);
        mod.pipeLiquidsSwitch = PipeCreator.createPipeAndRecipe(
            1,
            PipeSwitchFluids.class,
            true,
            mod.pipeItemsSwitch,
            BuildCraftTransport.pipeWaterproof);

        // water pump pipe
        // set fluid capacity
        PipeTransportFluids.fluidCapacities.put(PipeLiquidsWaterPump.class, APConfiguration.waterPumpWaterPerTick);
        mod.pipeLiquidsWaterPump = PipeCreator.createPipeAndRecipe(
            1,
            PipeLiquidsWaterPump.class,
            false,
            " L ",
            "rPr",
            " W ",
            'r',
            "dustRedstone",
            'P',
            "gearIron",
            'L',
            BuildCraftTransport.pipeFluidsGold,
            'W',
            BuildCraftTransport.pipeFluidsWood);
    }

}
