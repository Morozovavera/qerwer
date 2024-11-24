package sekelsta.horse_colors.client.renderer;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import sekelsta.horse_colors.entity.AbstractHorseGenetic;
import sekelsta.horse_colors.genetics.IGeneticEntity;

// Can't inherit from AbstractHorseRenderer because that uses HorseModel
@SideOnly(Side.CLIENT)
public class HorseGeneticRenderer extends RenderLiving<AbstractHorseGenetic>
{

    private static final Map<TextureLayer, ResourceLocation> LAYERED_LOCATION_CACHE = Maps.newHashMap();

    protected void preRenderCallback(AbstractHorseGenetic horse, float partialTickTime) {
        float scale = horse.getProportionalAgeScale();
        GlStateManager.scale(scale, scale, scale);
        super.preRenderCallback(horse, partialTickTime);
    }


    public HorseGeneticRenderer(RenderManager renderManager)
    {
        super(renderManager, new HorseGeneticModel(), 0.75F);
        this.addLayer(new HorseArmorLayer(this));
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call EntityRenderer.bindEntityTexture.
     */
//    @Override
//    public ResourceLocation getEntityTexture(AbstractHorseGenetic entity)
//    {
//        if (entity instanceof IGeneticEntity) {
//            String s = ((IGeneticEntity)entity).getGenes().getTexture();
//            ResourceLocation resourcelocation = LAYERED_LOCATION_CACHE.get(s);
//
//            if (resourcelocation == null)
//            {
//                resourcelocation = new ResourceLocation(s);
//                Minecraft.getMinecraft().getTextureManager().loadTexture(
//                    resourcelocation,
//                    new CustomLayeredTexture(((IGeneticEntity)entity).getGenes().getVariantTexturePaths())
//                );
//                LAYERED_LOCATION_CACHE.put(s, resourcelocation);
//            }
//
//            return resourcelocation;
//        }
//        System.out.println("Trying to render an ineligible entity");
//        return null;
//    }

    @Override
    public ResourceLocation getEntityTexture(AbstractHorseGenetic entity)
    {

        if (entity instanceof IGeneticEntity) {
            TextureLayer l = entity.getGenome().getTexturePaths();
            ResourceLocation resourcelocation = LAYERED_LOCATION_CACHE.get(l);

            if (resourcelocation == null)
            {
                resourcelocation = new ResourceLocation(l.getUniqueName());
                Minecraft.getMinecraft().getTextureManager().loadTexture(
                        resourcelocation,
                        new CustomLayeredTexture(entity.getGenome().getTexturePaths())
                );
                LAYERED_LOCATION_CACHE.put(l, resourcelocation);
            }

            return resourcelocation;
        }
        System.out.println("Trying to render an ineligible entity");

        return null;
    }
}
