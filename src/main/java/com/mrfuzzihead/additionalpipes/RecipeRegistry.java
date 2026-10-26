package com.mrfuzzihead.additionalpipes;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the optional pipe-to-pipe removal recipes. BuildCraft already registers its own uncrafting recipes, so this
 * only covers Additional Pipes' items.
 */
final class RecipeRegistry {

    private RecipeRegistry() {}

    static void registerRemovalRecipes(AdditionalPipes mod) {
        if (!APConfiguration.allowWRRemove) {
            return;
        }

        GameRegistry.addShapelessRecipe(new ItemStack(mod.pipeItemsTeleport), new Object[] { mod.pipePowerTeleport });
        GameRegistry.addShapelessRecipe(new ItemStack(mod.pipeItemsTeleport), new Object[] { mod.pipeLiquidsTeleport });

        if (mod.logisticsPipesInstalled) {
            GameRegistry
                .addShapelessRecipe(new ItemStack(mod.pipeItemsTeleport), new Object[] { mod.pipeLogisticsTeleport });
        }

        GameRegistry.addShapelessRecipe(new ItemStack(mod.pipeItemsSwitch), new Object[] { mod.pipeLiquidsSwitch });
        GameRegistry.addShapelessRecipe(new ItemStack(mod.pipeItemsSwitch), new Object[] { mod.pipePowerSwitch });
    }
}
