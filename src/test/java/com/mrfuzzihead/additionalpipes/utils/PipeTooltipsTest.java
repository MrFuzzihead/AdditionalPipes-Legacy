package com.mrfuzzihead.additionalpipes.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;

import org.junit.Test;

import com.mrfuzzihead.additionalpipes.pipes.PipeItemsTeleport;

import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.PipeTransportPower;

public class PipeTooltipsTest {

    @Test
    public void addsColoredFluidAndPowerRates() {
        Class<? extends Pipe<?>> pipeClass = PipeItemsTeleport.class;
        Integer oldFluid = PipeTransportFluids.fluidCapacities.put(pipeClass, 123);
        Integer oldPower = PipeTransportPower.powerCapacities.put(pipeClass, 456);

        try {
            List<String> lines = PipeTooltips.get(pipeClass, false);
            assertTrue(lines.contains(EnumChatFormatting.AQUA + "123 mB/t"));
            assertTrue(lines.contains(EnumChatFormatting.RED + "456 RF/t"));
        } finally {
            restore(PipeTransportFluids.fluidCapacities, pipeClass, oldFluid);
            restore(PipeTransportPower.powerCapacities, pipeClass, oldPower);
        }
    }

    @Test
    public void teleportDescriptionsAndShiftDetailsAreLoaded() throws Exception {
        try (InputStream in = PipeTooltipsTest.class.getResourceAsStream("/assets/additionalpipes/lang/en_US.lang")) {
            assertNotNull(in);
            StringTranslate.inject(in);
        }

        List<String> base = PipeTooltips.get(PipeItemsTeleport.class, false);
        assertTrue(base.contains("Teleports items to other item teleport pipes on the same frequency."));

        List<String> shift = PipeTooltips.get(PipeItemsTeleport.class, true);
        assertTrue(shift.contains("Right-click to set frequency and Send/Receive mode."));
        assertTrue(shift.contains("Private by default; the owner can make it public."));
    }

    private static <T> void restore(java.util.Map<T, Integer> map, T key, Integer oldValue) {
        if (oldValue == null) {
            map.remove(key);
        } else {
            map.put(key, oldValue);
        }
    }
}
