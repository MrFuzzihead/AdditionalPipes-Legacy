package com.mrfuzzihead.additionalpipes.item;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.additionalpipes.AdditionalPipes;
import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;
import com.mrfuzzihead.additionalpipes.utils.PipeTooltips;

import buildcraft.transport.ItemPipe;
import buildcraft.transport.Pipe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Additional Pipes pipe item with BuildCraft-style tooltips.
 *
 * <p>
 * The item provides a class-specific description from {@code tip.<PipeClass>} (and {@code tip.shift.<PipeClass>} while
 * shift is held) plus the coloured fluid/power throughput line when the pipe has one.
 */
public class ItemPipeAP extends ItemPipe {

    private final Class<? extends Pipe<?>> pipeClass;

    public ItemPipeAP(Class<? extends Pipe<?>> pipeClass) {
        super(AdditionalPipes.instance.creativeTab);
        this.pipeClass = pipeClass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack stack) {
        return PipeTeleport.class.isAssignableFrom(pipeClass) ? EnumRarity.rare : EnumRarity.common;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        // BuildCraft's own ItemPipe.addInformation would add its tooltip manager's (uncoloured) rate line and colour
        // the last line by pipe class. We provide the complete tooltip here so the rate line can be colour-coded.
        list.addAll(PipeTooltips.get(pipeClass, GuiScreen.isShiftKeyDown()));
    }
}
