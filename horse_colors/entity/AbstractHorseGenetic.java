package sekelsta.horse_colors.entity;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.ai.*;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.genetics.EquineGenome.Gene;
import sekelsta.horse_colors.genetics.breed.horse.*;
import sekelsta.horse_colors.item.ModItems;
import sekelsta.horse_colors.item.GeneBookItem;
import sekelsta.horse_colors.genetics.*;
import sekelsta.horse_colors.genetics.breed.*;
import sekelsta.horse_colors.util.Util;

public abstract class AbstractHorseGenetic extends AbstractChestHorse implements IGeneticEntity<Gene> {

    protected EquineGenome genes = new EquineGenome(this.getSpecies(), this);
    protected UUID motherUUID = null;
    protected UUID fatherUUID = null;

    protected static final DataParameter<String> GENES = EntityDataManager.<String>createKey(AbstractHorseGenetic.class, DataSerializers.STRING);

    protected static final DataParameter<Integer> HORSE_RANDOM = EntityDataManager.<Integer>createKey(AbstractHorseGenetic.class, DataSerializers.VARINT);
    protected static final DataParameter<Integer> DISPLAY_AGE = EntityDataManager.<Integer>createKey(AbstractHorseGenetic.class, DataSerializers.VARINT);

    protected static final DataParameter<Boolean> FERTILE = EntityDataManager.<Boolean>createKey(AbstractHorseGenetic.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Boolean> GENDER = EntityDataManager.<Boolean>createKey(AbstractHorseGenetic.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Integer> PREGNANT_SINCE = EntityDataManager.<Integer>createKey(AbstractHorseGenetic.class, DataSerializers.VARINT);
    protected static final DataParameter<Float> MOTHER_SIZE = EntityDataManager.<Float>createKey(AbstractHorseGenetic.class, DataSerializers.FLOAT);
    protected static final DataParameter<Boolean> IS_CASTRATED = EntityDataManager.<Boolean>createKey(AbstractHorseGenetic.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Boolean> AUTOBREEDABLE = EntityDataManager.<Boolean>createKey(AbstractHorseGenetic.class, DataSerializers.BOOLEAN);
    protected int trueAge;

    protected FleeGoal fleeGoal;
    public OustGoal oustGoal;
    protected long lastOustTime;

    protected static final UUID CSNB_SPEED_UUID = UUID.fromString("84ca527a-5c70-4336-a737-ae3f6d40ef45");
    protected static final UUID CSNB_JUMP_UUID = UUID.fromString("72323326-888b-4e46-bf52-f669600642f7");
    // See net.minecraft.entity.ai.attributes.ModifiableAttributeInstance.computeValue()
    // for which integers go with which operations
    // 2 is for "MULTIPLY TOTAL"
    protected static final AttributeModifier CSNB_SPEED_MODIFIER = (new AttributeModifier(CSNB_SPEED_UUID, "CSNB speed penalty", -0.6, 2)).setSaved(false);
    protected static final AttributeModifier CSNB_JUMP_MODIFIER = (new AttributeModifier(CSNB_JUMP_UUID, "CSNB jump penalty", -0.6, 2)).setSaved(false);

    protected static final int HORSE_GENETICS_VERSION = 2;

    protected List<AbstractHorseGenetic> unbornChildren = new ArrayList<>();

    public AbstractHorseGenetic(World worldIn)
    {
        super(worldIn);
    }

    public void copyAbstractHorse(AbstractHorse horse)
    {
        this.randomizeGenes(getRandomBreed());
        // Copy location
        this.setLocationAndAngles(horse.posX, horse.posY, horse.posZ, horse.rotationYaw, horse.rotationPitch);
        // Set tamed
        this.setHorseTamed(horse.isTame());
        // We don't know the player, so don't call setTamedBy
        // Set temper, in case it isn't tamed
        this.setTemper(horse.getTemper());
        // Do not transfer isRearing, isBreeding, or isEatingHaystack.
        // Set age
        this.setGrowingAge(horse.getGrowingAge());
        this.trueAge = horse.getGrowingAge();
        // Transfer inventory
        ContainerHorseChest inv = 
            ReflectionHelper.<ContainerHorseChest, AbstractHorse>getPrivateValue(AbstractHorse.class, horse, "horseChest", "field_110296_bG");
        this.horseChest.setInventorySlotContents(0, inv.getStackInSlot(0));
        this.horseChest.setInventorySlotContents(1, inv.getStackInSlot(1));
        this.updateHorseSlots();
        // Copy over speed, health, and jump
        double health = horse.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(health);

        double jump = horse.getEntityAttribute(JUMP_STRENGTH).getBaseValue();
        this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(jump);

        double speed = horse.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(speed);

        this.useGeneticAttributes();
    }


    public EquineGenome getGenome() {
        return genes;
    }


    @Override
    public int getTrueAge() {
        if (isChild() && getDisplayAge() >= 0) {
            return getBirthAge();
        }
        return getDisplayAge();
    }

    public void setGeneData(String genes) {
        this.dataManager.set(GENES, genes);
    }

    public String getGeneData() {
        return (String)this.dataManager.get(GENES);
    }

    public void setMotherSize(float size) {
        this.dataManager.set(MOTHER_SIZE, size);
    }

    public float getMotherSize() {
        return ((Float)this.dataManager.get(MOTHER_SIZE)).floatValue();
    }

    public abstract boolean fluffyTail();
    public abstract boolean longEars();
    public abstract boolean thinMane();
    public abstract Species getSpecies();

    public boolean canEquipChest() {
        return true;
    }

    @Override
    public Random getRand() {
        return super.getRNG();
    }

    @Override
    public int getSeed() {
        return this.dataManager.get(HORSE_RANDOM);
    }

    @Override
    public void setSeed(int seed) {
        this.dataManager.set(HORSE_RANDOM, seed);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIPanic(this, 1.2D));
        this.tasks.addTask(1, new EntityAIRunAroundLikeCrazy(this, 1.2D));
        if (HorseConfig.COMMON.spookyHorses) this.tasks.addTask(1, new SpookGoal(this));

        this.tasks.addTask(4, fleeGoal = new FleeGoal(this));
        this.tasks.addTask(5, new GenderedBreedGoal(this, 1.0D, AbstractHorse.class));
        this.tasks.addTask(7, new StayWithHerd(this));
        this.tasks.addTask(8, oustGoal = new OustGoal(this));
        this.tasks.addTask(9, new RandomWalkGroundTie(this, 0.7D));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(11, new EntityAILookIdle(this));


//        this.tasks.addTask(2, new GenderedBreedGoal(this, 1.0D, AbstractHorse.class));
//      //  this.tasks.addTask(4, new EntityAIFollowParent(this, 1.0D));
//        this.tasks.addTask(6, new RandomWalkGroundTie(this, 0.7D));
//        this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
//        this.tasks.addTask(8, new EntityAILookIdle(this));
//        //this.tasks.addTask(9, oustGoal = new OustGoal(this));
//        //this.tasks.addTask(10,  fleeGoal = new FleeGoal(this));
//        this.tasks.addTask(11, new StayWithHerd(this));
    }

    public void fleeFrom(Entity entity) {
        fleeGoal.toAvoid = entity;
    }

    public void oust(AbstractHorseGenetic competitor, AbstractHorseGenetic mare) {
        oustGoal.target = competitor;
        oustGoal.stayNear = mare;
    }

    public boolean isDrivingAwayCompetitor() {
        if (oustGoal.target != null) {
            lastOustTime = ticksExisted;
        }
        return ticksExisted - lastOustTime < 400;
    }

    public boolean isDirectRelative(AbstractHorseGenetic other) {
        boolean isParent = this.entityUniqueID != null && (this.entityUniqueID == other.motherUUID || this.entityUniqueID == other.fatherUUID);
        boolean isChild = other.entityUniqueID != null && (this.motherUUID == other.entityUniqueID || this.fatherUUID == other.entityUniqueID);
        boolean sharesMother = this.motherUUID != null && (this.motherUUID == other.motherUUID || this.motherUUID == other.fatherUUID);
        boolean sharesFather = this.fatherUUID != null && (this.fatherUUID == other.fatherUUID || this.fatherUUID == other.motherUUID);
        return isParent || isChild || sharesMother || sharesFather;
    }

    public boolean isVehicle() {
        return !this.getPassengers().isEmpty();
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();

        this.dataManager.register(GENES, "");
        this.dataManager.register(HORSE_RANDOM, 0);
        this.dataManager.register(DISPLAY_AGE, 0);
        this.dataManager.register(FERTILE, true);
        this.dataManager.register(GENDER, false);
        this.dataManager.register(AUTOBREEDABLE, false);
        this.dataManager.register(PREGNANT_SINCE, -1);
        this.dataManager.register(MOTHER_SIZE, 1f);
        this.dataManager.register(IS_CASTRATED, false);
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        this.getEntityData().setInteger("HorseGeneticsVersion", HORSE_GENETICS_VERSION);
        compound.setString("Genes", this.getGenome().getBase64());
        compound.setInteger("Random", this.getSeed());
        compound.setInteger("true_age", this.trueAge);
        compound.setBoolean("gender", this.isMale());
        compound.setBoolean("fertile", this.isFertile());
        setCastrated(compound.getBoolean("is_castrated"));
        compound.setInteger("pregnant_since", this.getPregnancyStart());
        if (this.unbornChildren != null) {
            NBTTagList unbornChildrenTag = new NBTTagList();
            for (AbstractHorseGenetic child : this.unbornChildren) {
                NBTTagCompound childNBT = new NBTTagCompound();
                childNBT.setString("species", child.getSpecies().toString());
                childNBT.setString("genes", child.getGenome().genesToString());
                if (child.motherUUID != null) {
                    childNBT.setUniqueId("MotherUUID", child.motherUUID);
                }
                if (child.fatherUUID != null) {
                    childNBT.setUniqueId("FatherUUID", child.fatherUUID);
                }
                unbornChildrenTag.appendTag(childNBT);
            }
            compound.setTag("unborn_children", unbornChildrenTag);
        }
        compound.setFloat("mother_size", this.getMotherSize());
        if (motherUUID != null) {
            compound.setUniqueId("MotherUUID", this.motherUUID);
        }
        if (fatherUUID != null) {
            compound.setUniqueId("FatherUUID", this.fatherUUID);
        }
        compound.setBoolean("autobreedable", isAutobreedable());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);


        if (compound.hasUniqueId("MotherUUID")) {
            this.motherUUID = compound.getUniqueId("MotherUUID");
        }
        if (compound.hasUniqueId("FatherUUID")) {
            this.fatherUUID = compound.getUniqueId("FatherUUID");
        }
        // Read the main part of the data
        readGeneticData(compound);
        // Ensure the true age matches the age
        if (this.trueAge < 0 != this.growingAge < 0) {
            this.trueAge = this.growingAge;
        }

        // Set any genes that were specified in a human-readable format
        readExtraGenes(compound);

        this.useGeneticAttributes();

        if (this instanceof HorseGeneticEntity) {
            int spawndata = compound.getInteger("VillageSpawn");
            if (spawndata != 0) {
                this.initFromVillageSpawn();
            }
        }

        compound.setBoolean("is_castrated", this.isCastrated());

        this.updatePersistentData();

        if (getSpecies() == Species.MULE || getSpecies() == Species.HINNY) {
            setFertile(false);
        }

        if (compound.hasKey("autobreedable")) {
            setAutobreedable(compound.getBoolean("autobreedable"));
        }
    }

    public void setAutobreedable(boolean allowed) {
        dataManager.set(AUTOBREEDABLE, allowed);
    }

    private void updatePersistentData() {
        // Tell Ride Along how much this horse weighs
        NBTTagCompound rideAlongTag = new NBTTagCompound();
        rideAlongTag.setDouble("WeightKg", this.getGenome().getGeneticWeightKg());
        this.getEntityData().setTag("RideAlong", rideAlongTag);
    }


    // A helper function for reading the data
    private void readGeneticData(NBTTagCompound compound) {
        // Set genes if they exist
        if (compound.hasKey("Genes")) {
            String genes = compound.getString("Genes");
            if (genes.length() > 0 && genes.charAt(0) < 48) {
                // Read genes using the old 1:1 conversion of chars to nums
                this.setGeneData(compound.getString("Genes"));
            }
            else {
                this.getGenome().setFromBase64(compound.getString("Genes"));
            }
        }
        // Otherwise, use a breed for a base if given one
        else if (compound.hasKey("Breed")) {
            randomizeGenes(getBreed(compound.getString("Breed")));
        }
        else {
            randomizeGenes(getRandomBreed());
        }


        if (compound.hasKey("Random")) {
            this.setSeed(compound.getInteger("Random"));
        }
        if (compound.hasKey("true_age")) {
            this.trueAge = compound.getInteger("true_age");
        }
        if (compound.hasKey("gender")) {
            this.setMale(compound.getBoolean("gender"));
        }
        else {
            this.setMale(this.rand.nextBoolean());
        }
        if (compound.hasKey("fertile")) {
            this.setFertile(compound.getBoolean("fertile"));
        }

        int pregnantSince = -1;
        if (compound.hasKey("pregnant_since")) {
            pregnantSince = compound.getInteger("pregnant_since");
        }
        this.dataManager.set(PREGNANT_SINCE, pregnantSince);
        if (compound.hasKey("unborn_children")) {
            NBTBase nbt = compound.getTag("unborn_children");
            if (nbt instanceof NBTTagList) {
                NBTTagList childListTag = (NBTTagList)nbt;
                for (int i = 0; i < childListTag.tagCount(); ++i) {
                    NBTBase cnbt = childListTag.get(i);
                    if (!(cnbt instanceof NBTTagCompound)) {
                        continue;
                    }
                    NBTTagCompound childNBT = (NBTTagCompound)cnbt;
                    Species species = Species.valueOf(childNBT.getString("species"));
                    AbstractHorseGenetic child = null;
                    switch(species) {
                        case HORSE:
                            child = new HorseGeneticEntity(this.world);
                            break;
                        case DONKEY:
                            child = new DonkeyGeneticEntity(this.world);
                            break;
                        case MULE:
                        case HINNY:
                            child = new MuleGeneticEntity(this.world);
                            ((MuleGeneticEntity)child).setSpecies(species);
                            break;
                    }
                    if (child != null) {
                        EquineGenome genome = new EquineGenome(child.getSpecies(), child);
                        genome.genesFromString(childNBT.getString("genes"));
                        if (childNBT.hasUniqueId("MotherUUID")) {
                            child.motherUUID = childNBT.getUniqueId("MotherUUID");
                        }
                        else {
                            child.motherUUID = this.entityUniqueID;
                        }
                        if (childNBT.hasUniqueId("FatherUUID")) {
                            child.fatherUUID = childNBT.getUniqueId("FatherUUID");
                        }
                        this.unbornChildren.add(child);
                    }
                }
            }
        }

        float motherSize = 1f;
        if (compound.hasKey("mother_size")) {
            motherSize = compound.getFloat("mother_size");
        }
        setMotherSize(motherSize);
    }

    protected void readExtraGenes(NBTTagCompound compound) {
        boolean changed = false;
        for (Enum gene : this.getGenome().listGenes()) {
            if (compound.hasKey(gene.toString())) {
                int alleles[] = compound.getIntArray(gene.toString());
                List<Integer> allowedAlleles = getGenome().getAllowedAlleles(gene, getDefaultBreed());
                for (int i = 0; i < 2; ++i) {
                    if (allowedAlleles.contains(alleles[i])) {
                        getGenome().setAllele(gene, i, alleles[i]);
                    }
                }
                changed = true;
            }
        }
        if (changed) {
            getGenome().finalizeGenes();
        }
    }


    public UUID getMotherUUID() {
        return motherUUID;
    }

    public UUID getFatherUUID() {
        return fatherUUID;
    }

    public int getDisplayAge() {
        return this.dataManager.get(DISPLAY_AGE);
    }

    public void setDisplayAge(int age) {
        this.dataManager.set(DISPLAY_AGE, age);
    }


    @Override
    public boolean isMale() {
        return this.dataManager.get(GENDER);
    }

    @Override
    public void setMale(boolean gender) {
        if (gender) {
            // Prepare to become male
            this.unbornChildren = new ArrayList<>();
            this.dataManager.set(PREGNANT_SINCE, -1);
        }
        else {
            // Prepare to become female
            this.setCastrated(false);
        }
        this.dataManager.set(GENDER, gender);
    }

    public boolean isCastrated() {
        return ((Boolean)this.dataManager.get(IS_CASTRATED)).booleanValue();
    }

    public void setCastrated(boolean isCastrated) {
        this.dataManager.set(IS_CASTRATED, isCastrated);
    }

    public boolean isPregnant() {
        return this.getPregnancyStart() >= 0;
    }

    public int getPregnancyStart() {
        return this.dataManager.get(PREGNANT_SINCE);
    }

    public float getPregnancyProgress() {
        int passed = getDisplayAge() - getPregnancyStart();
        int total = HorseConfig.getHorsePregnancyLength();
        return (float)passed / (float)total;
    }

    public int getRebreedTicks() {
        return HorseConfig.getHorseRebreedTicks(this.isMale());
    }

    public int getBirthAge() {
        return HorseConfig.getHorseBirthAge();
    }

    public ContainerHorseChest getInventory() {
        return this.horseChest;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        // Don't stop and rear in response to suffocation or cactus damage
        if (damageSourceIn != DamageSource.IN_WALL && damageSourceIn != DamageSource.CACTUS) {
            // Chance to rear up
            super.getHurtSound(damageSourceIn);
        }
        return null;
    }

    @Override
    public void setGrowingAge(int age) {
        if (age == -24000 && this.getGrowingAge() > age) {
            super.setGrowingAge(this.getBirthAge());
        }
        else {
            super.setGrowingAge(age);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (!itemstack.isEmpty() && itemstack.getItem() == Items.SPAWN_EGG) {
            return super.processInteract(player, hand);
        }

        if (!this.isChild()) {
            if (this.isTame() && player.isSneaking()) {
                this.openGUI(player);
                return true;
            }

            if (this.isBeingRidden()) {
                return super.processInteract(player, hand);
            }
        }

        if (itemstack.isEmpty()) {
            if (this.isChild()) {
                return super.processInteract(player, hand);
            }
            else {
                this.mountTo(player);
                return true;
            }
        }

        if (itemstack.getItem() == Items.BOOK
                && (HorseConfig.getBookShowsGenes()
                    || HorseConfig.getBookShowsTraits())
                && (this.isTame() || player.capabilities.isCreativeMode)) {
            ItemStack book = new ItemStack(ModItems.geneBookItem);
            if (book.getTagCompound() == null) {
                book.setTagCompound(new NBTTagCompound());
            }
            book.getTagCompound().setString("species", this.getSpecies().name());
            book.getTagCompound().setString("genes", this.getGenome().genesToString());
            if (this.hasCustomName()) {
                book.setStackDisplayName(this.getCustomNameTag());
            }
            if (!player.addItemStackToInventory(book)) {
                this.entityDropItem(book, 0);
            }
            if (!player.capabilities.isCreativeMode) {
                itemstack.shrink(1);
            }
            return true;
        }

        if (this.handleEating(player, itemstack)) {
            if (!player.capabilities.isCreativeMode) {
                itemstack.shrink(1);
            }
            return true;
        }

        if (itemstack.interactWithEntity(player, this, hand)) {
            return true;
        }

        if (!this.isTame()) {
            this.makeMad();
            return true;
        }

        if (this.isChild()) {
            return false;
        }

        if (!this.isHorseSaddled() && itemstack.getItem() == Items.SADDLE) {
             if (HorseConfig.getAutoEquipSaddle()) {
                if (!this.world.isRemote) {
                    ItemStack saddle = itemstack.splitStack(1);
                    this.horseChest.setInventorySlotContents(0, saddle);
                }
            }
            else {
                this.openGUI(player);
            }
            return true;
        }

        if (this.isArmor(itemstack) && this.wearsArmor()) {
             if (HorseConfig.getAutoEquipSaddle() && this.horseChest.getStackInSlot(1).isEmpty()) {
                if (!this.world.isRemote) {
                    ItemStack armor = itemstack.splitStack(1);
                    this.horseChest.setInventorySlotContents(1, armor);
                }
            }
            else {
                this.openGUI(player);
            }
            return true;
        }

        if (!this.hasChest() && itemstack.getItem() == Item.getItemFromBlock(Blocks.CHEST)) {
            if (this.canEquipChest()) {
                this.setChested(true);
                this.playChestEquipSound();
                this.initHorseChest();
                if (!player.capabilities.isCreativeMode) {
                    itemstack.shrink(1);
                }
            }
        }

        this.mountTo(player);
        return true;
    }

    protected void useGeneticAttributes()
    {
        if (HorseConfig.GENETICS.useGeneticStats)
        {
            Genome genes = this.getGenome();
            float maxHealth = this.getGenome().getHealth();
            float athletics = (genes.sumGenes(Gene.class, "athletics", 0, 4) / 2f)
                    + (genes.sumGenes(Gene.class, "athletics", 4, 8) / 2f);
            // Vanilla horse speed ranges from 0.1125 to 0.3375, as does ours
            float speedStat = genes.sumGenes(Gene.class, "speed", 0, 4)
                    + genes.sumGenes(Gene.class, "speed", 4, 8)
                    + genes.sumGenes(Gene.class, "speed", 8, 12)
                    + athletics;
            double movementSpeed = 0.1125D + speedStat * (0.225D / 32.0D);
            // Vanilla horse jump strength ranges from 0.4 to 1.0, as does ours
            float jumpStat = genes.sumGenes(Gene.class, "jump", 0, 4)
                    + genes.sumGenes(Gene.class, "jump", 4, 8)
                    + genes.sumGenes(Gene.class, "jump", 8, 12)
                    + athletics;
            double jumpStrength = 0.4D + jumpStat * (0.6D / 32.0D);

            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(maxHealth);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(jumpStrength);
        }
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        genes = new EquineGenome(this.getSpecies(), this);
        float maxHealth = this.getModifiedMaxHealth() + this.getGenome().getBaseHealth();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)maxHealth);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.getModifiedMovementSpeed());
        this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(this.getModifiedJumpStrength());
    }

    // Helper function for createChild that creates and spawns an entity of the 
    // correct species
    abstract AbstractHorse getChild(EntityAgeable otherparent);

    public boolean isOppositeGender(AbstractHorseGenetic other) {
        if (!HorseConfig.isGenderEnabled()) {
            return true;
        }
        return this.isMale() != other.isMale();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable)
    {
        if (!(ageable instanceof EntityAnimal)) {
            return null;
        }
        EntityAnimal otherAnimal = (EntityAnimal)ageable;
        // Have the female create the child if possible
        if (this.isMale() 
                && ageable instanceof AbstractHorseGenetic
                && !((AbstractHorseGenetic)ageable).isMale()) {
            return ageable.createChild(this);
        }
        AbstractHorse child = this.getChild(ageable);
        if (child != null) {
            this.setOffspringAttributes(ageable, child);
        }
        if (child instanceof AbstractHorseGenetic) {
            AbstractHorseGenetic foal = (AbstractHorseGenetic)child;
            if (ageable instanceof AbstractHorseGenetic) {
                AbstractHorseGenetic other = (AbstractHorseGenetic)ageable;
                foal.getGenome().inheritGenes(this.getGenome(), other.getGenome());
            }
            // Dominant white is homozygous lethal early in pregnancy. No child
            // is born.
            if (foal.getGenome().isEmbryonicLethal())
            {
                // Exit love mode
                this.resetInLove();
                otherAnimal.resetInLove();
                // Spawn smoke particles
                this.world.setEntityState(this, (byte)6);
                return null;
            }
            foal.setMale(rand.nextBoolean());
            foal.useGeneticAttributes();
            foal.setGrowingAge(HorseConfig.getMinAge());
            foal.motherUUID = this.getUniqueID();
            foal.fatherUUID = ageable.getUniqueID();
        }
        return child;
    }

    @Override
    public boolean setPregnantWith(EntityAgeable child, EntityAgeable otherParent) {
        if (otherParent instanceof IGeneticEntity) {
            IGeneticEntity otherGenetic = (IGeneticEntity)otherParent;
            if (this.isMale() == otherGenetic.isMale()) {
                return false;
            }
            else if (this.isMale() && !otherGenetic.isMale()) {
                return otherGenetic.setPregnantWith(child, this);
            }
        }
        if (this.isMale()) {
            return false;
        }

        if (child instanceof AbstractHorseGenetic) {
            unbornChildren.add((AbstractHorseGenetic)child);
            if (!this.world.isRemote) {
                // Can't be a child
                this.trueAge = Math.max(0, this.trueAge);
                this.dataManager.set(PREGNANT_SINCE, this.trueAge);
            }
            return true;
        }
        return false;
    }

    public boolean isAutobreedable() {
        return ((Boolean)this.dataManager.get(AUTOBREEDABLE)).booleanValue();
    }
    public boolean shouldRecordAge() {
        return this.getGenome().clientNeedsAge() || this.isPregnant();
    }
    public boolean canAutobreed() {
        // Check elsewhere if autobreeding is allowed in the config
        return !isVehicle() && !getLeashed() && !isHorseSaddled() && !hasChest() && (!isTame() || isAutobreedable()) && isFertile() && growingAge == 0;
    }
    @Override
    public void onUpdate()
    {
        super.onUpdate();
        // Keep track of age
        if (!this.world.isRemote) {
            // For children, align with growing age in case they have been fed
            if (this.growingAge < 0) {
                this.trueAge = this.growingAge;
            }
            else {
                this.trueAge = Math.max(0, Math.max(trueAge, trueAge + 1));
            }
            // Allow imprecision
            final int c = 400;
            if (this.trueAge / c != this.getDisplayAge() / c
                    || (this.trueAge < 0 != this.getDisplayAge() < 0)) {
                this.setDisplayAge(this.trueAge);
            }
        }

        // Pregnancy
        if (!this.world.isRemote && this.isPregnant()) {
            // Check pregnancy
            if (this.unbornChildren == null
                    || this.unbornChildren.size() == 0) {
                this.dataManager.set(PREGNANT_SINCE, -1);
            }
            // Handle birth
            int totalLength = HorseConfig.getHorsePregnancyLength();
            int currentLength = this.trueAge - this.getPregnancyStart();
            if (currentLength >= totalLength) {
                for (AbstractHorseGenetic child : unbornChildren) {
                    if (this.world instanceof WorldServer) {
                        GenderedBreedGoal.spawnChild(this, child, this.world);
                    }
                }
                this.unbornChildren = new ArrayList<>();
                this.dataManager.set(PREGNANT_SINCE, -1);
            }
        }


        else if (HorseConfig.BREEDING.autobreeding
                && !this.world.isRemote
                && ticksExisted % 800 == 0
                && (!isMale() || !HorseConfig.isGenderEnabled())
                && canAutobreed()
                && isInLove()
                && rand.nextFloat() < 0.05f) {
            List<AbstractHorseGenetic> equines = world.getEntitiesWithinAABB(AbstractHorseGenetic.class, getEntityBoundingBox().grow(16, 12, 16));
            if (equines.size() < 16) {
                setInLove(null);
                AbstractHorseGenetic stallion = equines
                        .stream()
                        .filter((h) -> isOppositeGender(h) && h.canAutobreed() && !isDirectRelative(h) && h.isInLove())
                        .sorted((h1, h2) -> Double.compare(h1.getDistanceSq(this), h2.getDistanceSq(this)))
                        .findFirst()
                        .orElse(null);
                if (stallion != null) {
                    stallion.setInLove(null);
                }
            }
        }

        // Overo lethal white syndrome
        if (this.getGenome().isLethalWhite()
                && this.ticksExisted > 80)
        {
            if (!this.isPotionActive(MobEffects.WITHER))
            {
                this.addPotionEffect(new PotionEffect(MobEffects.WITHER, 100, 3));
            }


        }
    }



    /**
     * Called to update the entity's position/logic.
     */
//    @Override
//    public void onUpdate()
//    {
//        super.onUpdate();
//        if (this.world.isRemote && this.dataManager.isDirty()) {
//            this.dataManager.setClean();
//            this.getGenome().resetTexture();
//        }
//
//        // Keep track of age
//        if (!this.world.isRemote && this.shouldRecordAge()) {
//            // For children, align with growing age in case they have been fed
//            if (this.growingAge < 0) {
//                this.trueAge = this.growingAge;
//            }
//            else {
//                this.trueAge = Math.max(0, this.trueAge + 1);
//            }
//            // Allow imprecision
//            final int c = 400;
//            if (this.trueAge / c != this.getDisplayAge() / c
//                    || (this.trueAge < 0 != this.getDisplayAge() < 0)) {
//                this.setDisplayAge(this.trueAge);
//            }
//        }
//
//        // Pregnancy
//        if (!this.world.isRemote && this.isPregnant()) {
//            // Check pregnancy
//            if (this.unbornChildren == null
//                    || this.unbornChildren.size() == 0) {
//                this.dataManager.set(PREGNANT_SINCE, -1);
//            }
//            // Handle birth
//            int totalLength = HorseConfig.getHorsePregnancyLength();
//            int currentLength = this.trueAge - this.getPregnancyStart();
//            if (currentLength >= totalLength) {
//                for (AbstractHorseGenetic child : unbornChildren) {
//                    GenderedBreedGoal.spawnChild(this, child, this.world);
//                }
//                this.unbornChildren = new ArrayList<>();
//                this.dataManager.set(PREGNANT_SINCE, -1);
//            }
//        }
//
//        // Overo lethal white syndrome
//        if ((!this.world.isRemote || true)
//            && this.getGenome().isLethalWhite()
//            && this.ticksExisted > 80)
//        {
//            if (!this.isPotionActive(MobEffects.POISON))
//            {
//                this.addPotionEffect(new PotionEffect(MobEffects.POISON, 100, 3));
//            }
//            if (this.getHealth() < 2)
//            {
//                this.addPotionEffect(new PotionEffect(MobEffects.INSTANT_DAMAGE, 1, 3));
//            }
//        }
//    }

    public void onLivingUpdate() {
        if (this.unbornChildren != null && this.unbornChildren.size() > 0
                && this.getPregnancyStart() < 0) {
            this.dataManager.set(PREGNANT_SINCE, 0);
        }

        if (this.getGenome().isHomozygous(Gene.leopard, HorseAlleles.LEOPARD) && !this.world.isRemote) {
            IAttributeInstance speedAttribute = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            IAttributeInstance jumpAttribute = this.getEntityAttribute(JUMP_STRENGTH);
            float brightness = this.getBrightness();
            if (brightness > 0.5f) {
                //setSprinting(true);
                if (speedAttribute.getModifier(CSNB_SPEED_UUID) != null) {
                    speedAttribute.removeModifier(CSNB_SPEED_MODIFIER);
                }
                if (jumpAttribute.getModifier(CSNB_JUMP_UUID) != null) {
                    jumpAttribute.removeModifier(CSNB_JUMP_MODIFIER);
                }
            }
            else {
                //setSprinting(false);
                if (speedAttribute.getModifier(CSNB_SPEED_UUID) == null) {
                    speedAttribute.applyModifier(CSNB_SPEED_MODIFIER);
                }
                if (jumpAttribute.getModifier(CSNB_JUMP_UUID) == null) {
                    jumpAttribute.applyModifier(CSNB_JUMP_MODIFIER);
                }
            }
        }
        super.onLivingUpdate();
    }

    @Override
    // This is needed so when the mutation chance is high, mules bred
    // with spawn eggs do not produce all splashed white foals.
    public Breed<Gene> getDefaultBreed() {
        return BaseEquine.breed;
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData spawnDataIn)
    {
        this.dataManager.set(PREGNANT_SINCE, -1);
        spawnDataIn = super.onInitialSpawn(difficulty, spawnDataIn);
        GeneticData geneticData = new GeneticData(this.getRandomBreed());
        randomizeGenes(geneticData.breed);
        setMale(rand.nextBoolean());
        boolean foal = !isTame() && rand.nextInt(4) == 0;
        if (foal) {
            // Foals pick a random age within the younger half
            trueAge = getBirthAge() + rand.nextInt(-getBirthAge() / 2);
        }
        else {
            trueAge = rand.nextInt(HorseConfig.GROWTH.getMaxAge());
        }
        // Don't set the growing age to a positive value, that would be bad
        setGrowingAge(Math.min(0, trueAge));
        return spawnDataIn;
    }


    protected void randomizeGenes(Breed breed) {
        setSeed(rand.nextInt());
        this.getGenome().randomize(breed);
        this.useGeneticAttributes();
        // Assume mother was the same size
        this.setMotherSize(this.getGenome().getGeneticScale());
        // Size depends on mother size so call again to stabilize somewhat
        this.setMotherSize(this.getGenome().getGeneticScale());
    }


//    private void randomize() {
//        this.getGenome().randomize(getDefaultBreed());
//        // Choose a random age
//        this.trueAge = this.rand.nextInt(HorseConfig.GROWTH.getMaxAge());
//        // This preserves the ratio of child/adult
//        if (this.rand.nextInt(5) == 0) {
//            // Foals pick a random age within the younger half
//            this.trueAge = this.getBirthAge() + this.rand.nextInt(-this.getBirthAge() / 2);
//        }
//        this.setMale(rand.nextBoolean());
//        // Don't set the growing age to a positive value, that would be bad
//        this.setGrowingAge(Math.min(0, this.trueAge));
//        this.useGeneticAttributes();
//    }

    public void initFromVillageSpawn() {
        randomizeGenes(getRandomBreed());
        // All village horses are easier to tame
        this.increaseTemper(this.getMaxTemper() / 2);
        if (!this.isChild() && rand.nextInt(16) == 0) {
            // Tame and saddle
            this.setHorseTamed(true);
            ItemStack saddle = new ItemStack(Items.SADDLE);
            this.horseChest.setInventorySlotContents(0, saddle);
        }
    }

    public float fractionGrown() {
        if (this.isChild()) {
            if (HorseConfig.getGrowsGradually()) {
                int minAge = HorseConfig.getMinAge();
                int age = Math.min(0, this.getDisplayAge());
                // 0 can't be accurate so assume it hasn't been set yet
                if (this.getDisplayAge() == 0) {
                    age = minAge;
                }
                float fractionGrown = (minAge - age) / (float)minAge;
                return Math.max(0, fractionGrown);
            }
            return 0;
        }
        return 1;
    }

    // Total size change that does not change proportions

    public float getProportionalAgeScale() {
        return getGenome().getCurrentScale() / getGangliness();
    }

    // The horse model uses this number to decide how foal-shaped to make the
    // horse. 0.5 is the most foal-shaped and 1 is the most adult-shaped.
    public float getGangliness() {
        return 0.5f + 0.5f * fractionGrown() * fractionGrown();
    }

    public boolean isFertile() {
        return ((Boolean)this.dataManager.get(FERTILE)).booleanValue();
    }

    public void setFertile(boolean fertile) {
        if (isPregnant()) {
            fertile = true;
        }
        this.dataManager.set(FERTILE, fertile);
    }

    public boolean isGroundTied() {
        return HorseConfig.COMMON.enableGroundTie && this.isHorseSaddled();
    }

    public static class GeneticData {

        public final Breed breed;
        public GeneticData(Breed breed) {
            this.breed = breed;
        }
    }

}
