package sekelsta.horse_colors;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.math.RayTraceResult;

import sekelsta.horse_colors.client.renderer.TextureLayerGroup;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.AbstractHorseGenetic;
import sekelsta.horse_colors.genetics.EquineGenome;
import sekelsta.horse_colors.genetics.Genome;
import sekelsta.horse_colors.genetics.IGeneticEntity;
import sekelsta.horse_colors.client.renderer.TextureLayer;

// Class for putting horse info on the debug screen
public class HorseDebug {
    // Determines when to print horse debug info on the screen
    public static boolean showDebug(EntityPlayer player)
    {
        if (!HorseConfig.enableDebugInfo())
        {
            return false;
        }
        return showBasicDebug(player) || showGeneDebug(player);
    }

    public static boolean showBasicDebug(EntityPlayer player) {
        ItemStack itemStack = player.getHeldItemOffhand();
        if (itemStack != null && itemStack.getItem() == Items.STICK) {
            return true;
        }
        ItemStack inHand = player.getHeldItemMainhand();
        return inHand != null && inHand.getItem() == Items.STICK;
    }

    public static boolean showGeneDebug(EntityPlayer player) {
        ItemStack itemStack = player.getHeldItemOffhand();
        if (itemStack != null && itemStack.getItem() == Items.TOTEM_OF_UNDYING) {
            return true;
        }
        ItemStack inHand = player.getHeldItemMainhand();
        return inHand != null && inHand.getItem() == Items.TOTEM_OF_UNDYING;
    }

    public static ArrayList<String> debugNamedGenes(Genome genome) {
        ArrayList<String> list = new ArrayList<String>();
        for (Enum gene : genome.listGenes()) {
            String s = gene + ": ";
            s += genome.getAllele(gene, 0) + ", ";
            s += genome.getAllele(gene, 1);
            list.add(s);
        }
        return list;
    }




    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void renderOverlayEvent(RenderGameOverlayEvent.Text event)
    {
        // If the player is looking at a horse and all conditions are met, add 
        // genetic information about that horse to the debug screen

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (!showDebug(player))
        {
            return;
        }
        // Check if we're looking at a horse
        RayTraceResult mouseOver = Minecraft.getMinecraft().objectMouseOver;
        if (mouseOver != null
            && mouseOver.entityHit != null
            && mouseOver.entityHit instanceof IGeneticEntity)
        {
            // If so, print information about it to the debug screen
            IGeneticEntity entity = (IGeneticEntity)mouseOver.entityHit;

            if (showBasicDebug(player) && entity instanceof EntityAgeable) {
                event.getLeft().add("Growing age: " + ((EntityAgeable)entity).getGrowingAge());
            }
            if (showBasicDebug(player) && entity instanceof AbstractHorseGenetic) {
                event.getLeft().add("Display age: " + ((AbstractHorseGenetic)entity).getDisplayAge());
                event.getLeft().add("Pregnant since: " + ((AbstractHorseGenetic)entity).getPregnancyStart());
            }

            if (showBasicDebug(player)) {
                event.getLeft().add(entity.getGenome().getTexturePaths().name);
                event.getLeft().add("Layers:");
                for (String s : entity.getGenome().getTexturePaths().getDebugStrings()) {
                    event.getLeft().add(s);
                }
            }
            if (showGeneDebug(player)) {
                List<String> strings = debugNamedGenes(entity.getGenome());
                for (int i = 0; i < strings.size() / 2; ++i) {
                    event.getRight().add(strings.get(i));
                }
                for (int i = strings.size() / 2; i < strings.size(); ++i) {
                    event.getLeft().add(strings.get(i));
                }
            }
        }
    }
}
