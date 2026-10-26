package com.mrfuzzihead.additionalpipes.gui;

import java.util.Arrays;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.additionalpipes.network.PacketHandler;
import com.mrfuzzihead.additionalpipes.network.message.MessageDistPipe;
import com.mrfuzzihead.additionalpipes.pipes.PipeItemsDistributor;
import com.mrfuzzihead.additionalpipes.textures.Textures;

import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiDistributionPipe extends GuiContainer {

    protected int xSize;
    protected int ySize;
    private final GuiButton[] buttons = new GuiButton[18];
    private int guiX;
    private int guiY;
    private final PipeItemsDistributor pipe;

    public GuiDistributionPipe(TileGenericPipe container) {
        super(new ContainerDistributionPipe(container));
        pipe = (PipeItemsDistributor) container.pipe;
        xSize = 117;
        ySize = 130;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        guiX = (width - xSize) / 2 + 30;
        guiY = (height - ySize) / 2 - 10;

        for (int side = 0; side < 6; side++) {
            int y = guiY + 24 + side * 18;
            int id = side * 3;
            buttonList.add(buttons[id] = new GuiButton(id + 1, guiX + 1, y, 20, 17, "-"));
            buttonList.add(buttons[id + 1] = new GuiButton(id + 2, guiX + 23, y, 30, 17, "0"));
            buttonList.add(buttons[id + 2] = new GuiButton(id + 3, guiX + 55, y, 20, 17, "+"));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int p1, int p2) {
        for (int side = 0; side < 6; side++) {
            buttons[side * 3 + 1].displayString = Integer.toString(pipe.distData[side]);
        }

        fontRendererObj
            .drawString(StatCollector.translateToLocal("gui.pipeItemsDistributor"), guiX + 42, guiY + 22, 4210752);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || button.id < 1 || button.id > buttons.length) {
            return;
        }

        int zeroBased = button.id - 1;
        int index = zeroBased / 3;
        int action = zeroBased % 3;
        if (index < 0 || index >= pipe.distData.length || action == 1) {
            return;
        }

        int delta = action == 0 ? -1 : 1;
        int newData = PipeItemsDistributor.clampWeight(pipe.distData[index] + delta);
        if (newData == pipe.distData[index]) {
            return;
        }

        // Optimistically update the client copy so rapid clicks are not all calculated from the same stale value.
        pipe.distData[index] = newData;
        boolean anyPositive = false;
        for (int value : pipe.distData) {
            if (value > 0) {
                anyPositive = true;
                break;
            }
        }
        if (!anyPositive) {
            Arrays.fill(pipe.distData, 1);
        }

        MessageDistPipe message = new MessageDistPipe(
            pipe.container.xCoord,
            pipe.container.yCoord,
            pipe.container.zCoord,
            (byte) index,
            pipe.distData[index]);
        PacketHandler.INSTANCE.sendToServer(message);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.GUI_DISTRIBUTION);
        int j = (width - xSize) / 2;
        int k = (height - ySize) / 2;
        drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
    }
}
