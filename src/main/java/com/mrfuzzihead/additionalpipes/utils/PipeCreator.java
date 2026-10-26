package com.mrfuzzihead.additionalpipes.utils;

import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.item.ItemPipeAP;
import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;

import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import cpw.mods.fml.common.registry.GameRegistry;

public class PipeCreator {

    /**
     * Creates and registers a buildcraft pipe from the provided class.
     * Also sets a recipe for it from the provided Object[].
     *
     * @param output    how many of the pipe should be output from the recipe
     * @param clas
     * @param recipe
     * @param shapeless whether or not the recipe is shapeless
     * @return
     */
    public static Item createPipeAndRecipe(int output, Class<? extends Pipe<?>> clas, boolean shapeless,
        Object... recipe) {

        Item pipe = createPipe(clas);
        for (Object obj : recipe) {
            if (obj == null) return pipe;
        }
        if (shapeless) {
            GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(pipe, output), recipe));
        } else {
            GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(pipe, output), recipe));
        }
        return pipe;
    }

    /**
     * Creates and registers a buildcraft pipe from the provided class.
     *
     * @param clas
     * @return
     */
    public static Item createPipe(Class<? extends Pipe<?>> clas) {
        ItemPipeAP item = new ItemPipeAP(clas);

        // Preserve the registry names used before tooltip support was added. Teleport pipes were always registered
        // under their class name; the other pipes were registered by BuildCraft as buildcraftPipe.<lowercase>.
        String registryName = PipeTeleport.class.isAssignableFrom(clas) ? clas.getSimpleName()
            : "buildcraftPipe." + clas.getSimpleName()
                .toLowerCase(Locale.ENGLISH);
        GameRegistry.registerItem(item, registryName);

        // Display name/lang key; set after registration as the old BlockGenericPipe.registerPipe path did.
        item.setUnlocalizedName(clas.getSimpleName());
        AdditionalPipes.proxy.registerPipeRendering(item);
        BlockGenericPipe.pipes.put(item, clas);
        AdditionalPipes.proxy.createPipeSpecial(item, clas);
        return item;
    }
}
