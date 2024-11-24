package sekelsta.horse_colors.client.renderer;

import net.minecraft.client.renderer.texture.*;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CustomLayeredTexture extends AbstractTexture {
    public final TextureLayerGroup layerGroup;

    public CustomLayeredTexture(TextureLayerGroup layers) {
        this.layerGroup = layers;
    }

    public void loadTexture(IResourceManager manager) {
        deleteGlTexture();
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), layerGroup.getLayer(manager), false, false);
    }

}
