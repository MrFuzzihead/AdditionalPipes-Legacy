package buildcraft.additionalpipes.textures;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import buildcraft.api.core.IIconProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class APPipeIconProvider implements IIconProvider {

    private IIcon icons[];

    // 1 more than highest-numbered icon
    private final int iconCount = 41;

    public APPipeIconProvider() {
        icons = new IIcon[iconCount];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int iconIndex) {
        if (iconIndex >= iconCount) return null;
        return icons[iconIndex];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        for (int i = 0; i < iconCount; i++) {
            // 6/7/8 belonged to the removed Advanced Wooden and Advanced Insertion pipes;
            // 31 belonged to the removed Obsidian Fluid Pipe. Keep the numbering stable
            // for the remaining pipes and leave these slots empty.
            if (i == 6 || i == 7 || i == 8 || i == 31) {
                continue;
            }
            icons[i] = iconRegister.registerIcon("additionalpipes:" + i);
        }
    }

}
