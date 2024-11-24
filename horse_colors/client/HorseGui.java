package sekelsta.horse_colors.client;
import net.minecraft.client.gui.inventory.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.inventory.ContainerHorseInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.AbstractHorseGenetic;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.util.Util;

@SideOnly(Side.CLIENT)
public class HorseGui extends GuiContainer {
    private static final ResourceLocation HORSE_GUI_TEXTURES = new ResourceLocation(HorseColors.MODID, "textures/gui/horse.png");
    /** The player inventory bound to this GUI. */
    private final IInventory playerInventory;
    /** The horse inventory bound to this GUI. */
    private final IInventory horseInventory;
    /** The EntityHorse whose inventory is currently being accessed. */
    private final AbstractHorseGenetic horseEntity;
    /** The mouse x-position recorded during the last rendered frame. */
    private float mousePosx;
    /** The mouse y-position recorded during the last renderered frame. */
    private float mousePosY;

    ITextComponent title;

    public HorseGui(AbstractHorseGenetic horse) {
        super(new ContainerHorseInventory(Minecraft.getMinecraft().player.inventory, horse.getInventory(), horse, Minecraft.getMinecraft().player));
        this.playerInventory = Minecraft.getMinecraft().player.inventory;
        this.horseInventory = horse.getInventory();
        this.horseEntity = horse;
        this.title = horse.getDisplayName();
        this.allowUserInput = false;
    }

   /**
    * Draw the foreground layer for the GuiContainer (everything in front of the items)
    */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(this.title.getFormattedText(), 8, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
        if (!HorseConfig.COMMON.enableSizes || horseEntity.isChild()) return;
        float height = horseEntity.getGenome().getGeneticHeightCm();
        int cm = Math.round(height);
        int inches = Math.round(height / 2.54f);
        int hands = inches / 4;
        int point = inches % 4;
        int yy = 20;
        this.fontRenderer.drawString(Util.translate("gui.height"), 82, yy, 0x404040);
        this.fontRenderer.drawString(" " + hands + "." +  point + " " + Util.translate("gui.hands"), 82, yy + 9, 0x404040);
        this.fontRenderer.drawString(" " + cm + " " + Util.translate("gui.cm"), 82, yy + 18, 0x404040);

        if (horseEntity.getGenome().isMiniature()) {
            this.fontRenderer.drawString(Util.translate("gui.miniature"), 82, yy+27, 0x404040);
        }
        else if (horseEntity.getGenome().isLarge()) {
            this.fontRenderer.drawString(Util.translate("gui.large"), 82, yy+36, 0x404040);
        }

    }

   /**
    * Draws the background layer of this container (behind the items).
    */
    @Override
   protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(HORSE_GUI_TEXTURES);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        if (this.horseEntity instanceof AbstractChestHorse) {
            AbstractChestHorse abstractchestedhorseentity = (AbstractChestHorse)this.horseEntity;
            if (abstractchestedhorseentity.hasChest()) {
                this.drawTexturedModalRect(i + 79, j + 17, 0, this.ySize, abstractchestedhorseentity.getInventoryColumns() * 18, 54);
            }
        }

        if (this.horseEntity.canBeSaddled()) {
            this.drawTexturedModalRect(i + 7, j + 35 - 18, 18, this.ySize + 54, 18, 18);
        }

        if (this.horseEntity.wearsArmor()) {/*
            if (this.horseEntity instanceof EntityLlama) {
                this.drawTexturedModalRect(i + 7, j + 35, 36, this.ySize + 54, 18, 18);
            } else {*/
                this.drawTexturedModalRect(i + 7, j + 35, 0, this.ySize + 54, 18, 18);
            //}
        }

      //  if (HorseConfig.isGenderEnabled()) {
            int iconWidth = 10;
            int iconHeight = 11;
            int textureX = 176;
            int renderX = i + 157;
            int renderY = j + 4;
            if (this.horseEntity.isMale()) {
                textureX += iconWidth;
            }
            int textureY = 0;
            boolean grayIcons = HorseConfig.COMMON.useGeneticAnimalsIcons;
            if (grayIcons) {
                textureX += 2 * iconWidth;
            }
            if (this.horseEntity.isPregnant()) {
                renderX -= 2;
                int pregRenderX = renderX + iconWidth + 1;
                // Blit pregnancy background
                this.drawTexturedModalRect(pregRenderX, renderY + 1, 181, 23, 2, 10);
                // Blit pregnancy foreground based on progress
                int pregnantAmount = (int)(11 * horseEntity.getPregnancyProgress());
                this.drawTexturedModalRect(pregRenderX, renderY + 11 - pregnantAmount, 177, 33 - pregnantAmount, 2, pregnantAmount);
            }
            // Blit gender icon
            // X, y to render to, x, y to render from, width, height
            this.drawTexturedModalRect(renderX, renderY, textureX, textureY, iconWidth, iconHeight);

            if (this.horseEntity.isPregnant() && grayIcons) {
                // Blit pregnancy foreground based on progress
                int pregnantAmount = (int)(10 * horseEntity.getPregnancyProgress()) + 1;
                this.drawTexturedModalRect( renderX, renderY + 11 - pregnantAmount, textureX, textureY + iconHeight + 11 - pregnantAmount, iconWidth, pregnantAmount);
            }
    //    }

        GuiInventory.drawEntityOnScreen(i + 51, j + 60, 17, (float)(i + 51) - this.mousePosx, (float)(j + 75 - 50) - this.mousePosY, this.horseEntity);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.mousePosx = (float)mouseX;
        this.mousePosY = (float)mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @SubscribeEvent
    public static void replaceGui(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiScreenHorseInventory) {
            GuiScreenHorseInventory screen = (GuiScreenHorseInventory)event.getGui();
            // field_147034_x = horseEntity
            AbstractHorse horse = ObfuscationReflectionHelper.getPrivateValue(GuiScreenHorseInventory.class, screen, "field_147034_x");
            if (horse instanceof AbstractHorseGenetic) {
                AbstractHorseGenetic horseGenetic = (AbstractHorseGenetic)horse;
                event.setGui(new HorseGui(horseGenetic));
            }
        }
    }
}
