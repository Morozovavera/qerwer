package sekelsta.horse_colors.entity;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.World;

import sekelsta.horse_colors.genetics.EquineGenome;
import sekelsta.horse_colors.genetics.breed.Breed;
import sekelsta.horse_colors.genetics.breed.BreedManager;
import sekelsta.horse_colors.genetics.Species;

import java.util.Collection;

public class MuleGeneticEntity extends AbstractHorseGenetic {

    protected static final DataParameter<Integer> SPECIES = EntityDataManager.<Integer>createKey(MuleGeneticEntity.class, DataSerializers.VARINT);
    public MuleGeneticEntity(World world) {
        super(world);
    }

    @Override
    public boolean fluffyTail() {
        return true;
    }

    @Override
    public boolean longEars() {
        return true;
    }

    @Override
    public boolean thinMane() {
        return false;
    }

    @Override
    public Species getSpecies() {
        return Species.MULE;
    }

    @Override
    protected boolean canMate() {
        return false;
    }

    @Override
    // Helper function for createChild that creates and spawns an entity of the
    // correct species
    public AbstractHorse getChild(EntityAgeable ageable) {
        MuleGeneticEntity child = new MuleGeneticEntity(this.world);
        return child;
    }

    @Nullable
    protected ResourceLocation getLootTable()
    {
        return LootTableList.ENTITIES_MULE;
    }

    protected SoundEvent getAmbientSound() {
        super.getAmbientSound();
        return SoundEvents.ENTITY_MULE_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        super.getDeathSound();
        return SoundEvents.ENTITY_MULE_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        super.getHurtSound(damageSourceIn);
        return SoundEvents.ENTITY_MULE_HURT;
    }

    protected void playChestEquipSound() {
        this.playSound(SoundEvents.ENTITY_MULE_CHEST, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
    }
    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData spawnDataIn)
    {
        spawnDataIn = super.onInitialSpawn(difficulty, spawnDataIn);
        EquineGenome horse = new EquineGenome(Species.HORSE);
        horse.randomize(BreedManager.getBreed("default_horse"));
        EquineGenome donkey = new EquineGenome(Species.DONKEY);
        donkey.randomize(BreedManager.getBreed("default_donkey"));
        this.genes.inheritGenes(horse, donkey);
        this.useGeneticAttributes();
        return spawnDataIn;
    }

    public void setSpecies(Species species) {
        this.dataManager.set(SPECIES, species.ordinal());
    }


    @Override
    public boolean isFertile() {
        return false;
    }

    @Override
    public void setFertile(boolean fertile) {
        // Pass
    }


    @Override
    public Collection<Breed<EquineGenome.Gene>> getBreeds() {
        return ImmutableList.of(getDefaultBreed());
    }

}
