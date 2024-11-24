package sekelsta.horse_colors.entity;
import net.minecraft.entity.passive.EntityDonkey;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.config.HorseConfig;

import sekelsta.horse_colors.client.renderer.HorseGeneticRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModEntities {
    private static int ID = 0;

    static IForgeRegistry<Biome> biomeRegistry;

    public static EntityEntry HORSE_GENETIC;
    public static EntityEntry DONKEY_GENETIC;
    public static EntityEntry MULE_GENETIC;

    public static final int horseEggPrimary = 0x7F4320;
    public static final int horseEggSecondary = 0x110E0D;
    static
    {
        int horsePlainsWeight = (int)Math.round(5 * HorseConfig.COMMON.horseSpawnMultiplier);
        int horseSavannaWeight = (int)Math.round(1 * HorseConfig.COMMON.horseSpawnMultiplier);

        // Default tracker is fine, or could use .tracker(64, 2, false)
        final ResourceLocation horseRegistryName = new ResourceLocation(HorseColors.MODID, "horse_felinoid");
        // Vanilla size in 1.12 was 1.3964844F, 1.6F
	    HORSE_GENETIC = EntityEntryBuilder.create()
            .entity(HorseGeneticEntity.class)
            // Last parameter is network ID, which needs to be unique per mod.
            .id(horseRegistryName, ID++)
            .name(horseRegistryName.toString())
            .egg(horseEggPrimary, horseEggSecondary)
            .tracker(64, 2, false)
            .spawn(EnumCreatureType.CREATURE, horsePlainsWeight, 
                4, 6, 
                BiomeDictionary.getBiomes(BiomeDictionary.Type.PLAINS))
            .spawn(EnumCreatureType.CREATURE, horseSavannaWeight, 
                4, 6, 
                BiomeDictionary.getBiomes(BiomeDictionary.Type.SAVANNA))
            .build();

        final ResourceLocation donkeyRegistryName = new ResourceLocation(HorseColors.MODID, "donkey");
	    DONKEY_GENETIC = EntityEntryBuilder.create()
            .entity(DonkeyGeneticEntity.class)
            // Last parameter is network ID, which needs to be unique per mod.
            .id(donkeyRegistryName, ID++)
            .name(donkeyRegistryName.toString())
            .egg(0x726457, 0xcdc0b5)
            .tracker(64, 2, false)
            .spawn(EnumCreatureType.CREATURE, 1, 
                1, 3, 
                BiomeDictionary.getBiomes(BiomeDictionary.Type.PLAINS))
            .spawn(EnumCreatureType.CREATURE, 1, 
                1, 1, 
                BiomeDictionary.getBiomes(BiomeDictionary.Type.SAVANNA))
            .build();

        final ResourceLocation muleRegistryName = new ResourceLocation(HorseColors.MODID, "mule");
	    MULE_GENETIC = EntityEntryBuilder.create()
            .entity(MuleGeneticEntity.class)
            // Last parameter is network ID, which needs to be unique per mod.
            .id(muleRegistryName, ID++)
            .name(muleRegistryName.toString())
            .egg(0x4b3a30, 0xcdb9a8)
            .tracker(64, 2, false)
            .build();
    }
/*
   private static <T extends Entity> EntityType<T> register(String key, EntityType.Builder<T> builder) {
      return Registry.register(Registry.ENTITY_TYPE, key, builder.build(key));
   }*/
    public static class RegistrationHandler {
        /**
         * Register this mod's {@link Entity} types.
         *
         * @param event The event
         */
        @SubscribeEvent
        public static void registerEntities(final RegistryEvent.Register<EntityEntry> event) {

            event.getRegistry().registerAll(
                    HORSE_GENETIC,
                    DONKEY_GENETIC,
                    MULE_GENETIC
            );
        }

        // I just need the biome registry for later
        @SubscribeEvent
        public static void catchBiomeRegistry(final RegistryEvent.Register<Biome> event) {
            biomeRegistry = event.getRegistry();
        }

    }

    /**
     * Get an array of {@link Biome}s with the specified {@link BiomeDictionary.Type}.
     *
     * @param type The Type
     * @return An array of Biomes
     */
    private static Biome[] getBiomes(final BiomeDictionary.Type type) {
        assert(type != null);
        assert(BiomeDictionary.getBiomes(type) != null);
        return BiomeDictionary.getBiomes(type).toArray(new Biome[0]);
    }

    //Removes initial vanilla horse spawns
    public static void editSpawnTable() {
        Collection<Biome> allBiomes = biomeRegistry.getValuesCollection();
        for (Biome biome : allBiomes) {
                List<Biome.SpawnListEntry> spawns = biome.getSpawnableList(EnumCreatureType.CREATURE);
                if (spawns.isEmpty()) {
                    continue;
                }
                ArrayList<Biome.SpawnListEntry> horseSpawns = new ArrayList<Biome.SpawnListEntry>();
                for (Biome.SpawnListEntry entry : spawns) {
                    if (entry.entityClass == EntityHorse.class && HorseConfig.blockVanillaHorseSpawns()) {
                        HorseColors.logger.debug("Removing vanilla horse spawn: " + entry + " from biome " + biome);
                        horseSpawns.add(entry);
                    }
                    else if (entry.entityClass == EntityDonkey.class && HorseConfig.blockVanillaDonkeySpawns()) {
                        HorseColors.logger.debug("Removing vanilla donkey spawn: " + entry + " from biome " + biome);
                        horseSpawns.add(entry);
                    }
                }
                for (Biome.SpawnListEntry horseSpawn : horseSpawns) {
                    spawns.remove(horseSpawn);
                }
        }

    }
    @SideOnly(Side.CLIENT)
    public static void registerRenderers()
    {
        HorseGeneticRenderer renderer = new HorseGeneticRenderer(Minecraft.getMinecraft().getRenderManager());
        RenderingRegistry.registerEntityRenderingHandler(HorseGeneticEntity.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(MuleGeneticEntity.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(DonkeyGeneticEntity.class, renderer);
    }

    public static void onLoadComplete() {
        // This needs to happen after the config file is read and vanilla horse spawns are added
        editSpawnTable();
    }
}
