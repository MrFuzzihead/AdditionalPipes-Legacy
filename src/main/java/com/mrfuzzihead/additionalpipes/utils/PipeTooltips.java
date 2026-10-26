package com.mrfuzzihead.additionalpipes.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.google.common.base.Splitter;

import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.PipeTransportPower;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Builds BuildCraft-style item tooltips for Additional Pipes pipes.
 *
 * <p>
 * Descriptions come from {@code tip.<PipeClass>} and {@code tip.shift.<PipeClass>} in the language file. Fluid and
 * power throughput is read from BuildCraft's capacity maps, so teleport/switch pipes show their real configured rate.
 */
@SideOnly(Side.CLIENT)
public final class PipeTooltips {

    private static final Splitter NEW_LINE = Splitter.on("\\n");

    private PipeTooltips() {}

    public static List<String> get(Class<? extends Pipe<?>> pipeClass, boolean shift) {
        List<String> lines = new ArrayList<String>();

        addLocalized(lines, "tip." + pipeClass.getSimpleName());

        Integer fluidRate = PipeTransportFluids.fluidCapacities.get(pipeClass);
        if (fluidRate != null && fluidRate > 0) {
            lines.add(EnumChatFormatting.AQUA + Integer.toString(fluidRate) + " mB/t");
        }

        Integer powerRate = PipeTransportPower.powerCapacities.get(pipeClass);
        if (powerRate != null && powerRate > 0) {
            lines.add(EnumChatFormatting.RED + Integer.toString(powerRate) + " RF/t");
        }

        if (shift) {
            addLocalized(lines, "tip.shift." + pipeClass.getSimpleName());
        }

        return lines;
    }

    private static void addLocalized(List<String> lines, String key) {
        if (!StatCollector.canTranslate(key)) {
            return;
        }

        for (String line : NEW_LINE.split(StatCollector.translateToLocal(key))) {
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
    }
}
