package sekelsta.horse_colors.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.AbstractHorse;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.AbstractHorseGenetic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StayWithHerd extends EntityAIBase {
    protected final AbstractHorseGenetic horse;
    protected AbstractHorse target;
    protected float distanceModifier = 1;

    protected double walkSpeed = 1;
    protected double runSpeed = 1.4;
    protected int timeUntilRecalculatePath = 0;

    protected int lastSearchTick = 0;
    protected int acceptableDelay = 50;
    public StayWithHerd(AbstractHorseGenetic entityIn) {
        this.horse = entityIn;
        this.setMutexBits(1);
    }

    protected double closeEnoughDistance() {
        return 2 + 10 * horse.getFractionGrown();
    }

    protected double tooFarDistance() {
        return 16 + 8 * horse.getFractionGrown();
    }


    @Override
    public boolean shouldExecute() {
        if (horse.isVehicle() || horse.getLeashed() || horse.isGroundTied()) {
            return false;
        }

        distanceModifier = 1;
        if (horse.isChild() && target != null && !target.isDead && target.getUniqueID().equals(horse.getMotherUUID())) {
            double distSq = target.getDistanceSq(horse);
            double min = closeEnoughDistance();
            if (distSq < min * min) {
                return false;
            }
            double max = tooFarDistance();
            if (distSq < max * max) {
                return true;
            }
        }

        float age = horse.getFractionGrown();
        if (horse.ticksExisted - lastSearchTick > acceptableDelay * age) {
            lastSearchTick = horse.ticksExisted + horse.getRand().nextInt(8);
            double horizontalSearch = 4 + 16 * age;
            double verticalSearch = 4 + 8 * age;
            List<AbstractHorse> equines = horse.world.getEntitiesWithinAABB(AbstractHorse.class, horse.getEntityBoundingBox().grow(horizontalSearch, verticalSearch, horizontalSearch));
            target = getBestTarget(equines.stream().filter((h) -> h != horse).collect(Collectors.toList()));
        }
        return shouldContinueExecuting();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (target == null || target.isDead) {
            return false;
        }
        if (target instanceof AbstractHorseGenetic && ((AbstractHorseGenetic)target).isDrivingAwayCompetitor()) {
            return false;
        }
        if (!HorseConfig.COMMON.herdsFollowRidden && !horse.isChild() && (!target.getPassengers().isEmpty() || target.getLeashed())) {
            return false;
        }
        double distSq = target.getDistanceSq(horse);
        double max = tooFarDistance() * distanceModifier;
        double min = closeEnoughDistance() * distanceModifier;
        return (distSq < max * max || horse.isDrivingAwayCompetitor()) && distSq > min * min;
    }

    @Override
    public void startExecuting() {
        timeUntilRecalculatePath = 0;
    }


    public int bestMother(AbstractHorse h1, AbstractHorse h2) {
        if (h1.getUniqueID().equals(horse.getMotherUUID())) {
            return -1;
        }
        else if (h2.getUniqueID().equals(horse.getMotherUUID())) {
            return 1;
        }
        boolean h1b = h1.isChild();
        boolean h2b = h2.isChild();
        if (h1b != h2b) {
            return Boolean.compare(h2b, h1b);
        }
        return nearestIdeallyMatchingClass(h1, h2);
    }

    public int nearestIdeallyMatchingClass(AbstractHorse h1, AbstractHorse h2) {
        boolean h1c = h1.getClass().equals(horse.getClass());
        boolean h2c = h2.getClass().equals(horse.getClass());
        if (h1c != h2c) {
            return Boolean.compare(h2c, h1c);
        }
        return Double.compare(h1.getDistanceSq(horse), h2.getDistanceSq(horse));
    }

    public int strongestIdeallyMatchingClass(AbstractHorse h1, AbstractHorse h2) {
        boolean h1c = h1.getClass().equals(horse.getClass());
        boolean h2c = h2.getClass().equals(horse.getClass());
        if (h1c != h2c) {
            return Boolean.compare(h2c, h1c);
        }
        return Float.compare(h2.getMaxHealth(), h1.getMaxHealth());
    }

    public int highestHealth(AbstractHorse h1, AbstractHorse h2) {
        return Float.compare(h2.getMaxHealth(), h1.getMaxHealth());
    }

    public AbstractHorse getBestTarget(List<AbstractHorse> equines) {
        if (horse.isChild()) {
            return equines.stream().min(this::bestMother).orElse(null);
        }

        if (horse.isDrivingAwayCompetitor() && horse.oustGoal.target == null && horse.oustGoal.stayNear != null) {
            return horse.oustGoal.stayNear;
        }

        List<AbstractHorseGenetic> geneticEquines = new ArrayList<>();
        List<AbstractHorse> vanillaEquines = new ArrayList<>();
        for (AbstractHorse h : equines) {
            if (!HorseConfig.COMMON.herdsFollowRidden && (!h.getPassengers().isEmpty() || h.getLeashed())) {
                continue;
            }
            if (h instanceof AbstractHorseGenetic) {
                geneticEquines.add((AbstractHorseGenetic)h);
            }
            else {
                vanillaEquines.add(h);
            }
        }

        if (!horse.isMale() || !HorseConfig.BREEDING.enableGenders) {
            AbstractHorseGenetic foal = null;
            for (AbstractHorseGenetic h : geneticEquines) {
                if (h.isChild() && horse.getUniqueID().equals(h.getMotherUUID())) {
                    if (foal == null || foal.getDistanceSq(horse) < h.getDistanceSq(horse)) {
                        foal = h;
                    }
                }
            }
            if (foal != null && foal.getDistanceSq(horse) > 12 * 12) {
                distanceModifier = 0.1f * foal.getFractionGrown();
                return foal;
            }
        }
        else if (horse.isFertile()) {
            AbstractHorseGenetic mare = geneticEquines.stream()
                    .filter((h) -> isFertileMare(h) && h.getClass().equals(horse.getClass()))
                    .sorted((h1, h2) -> Double.compare(h1.getDistanceSq(horse), h2.getDistanceSq(horse)))
                    .findFirst().orElse(null);
            if (mare != null) {
                if (!HorseConfig.COMMON.jealousStallions) {
                    return mare;
                }
                AbstractHorseGenetic competitor = geneticEquines.stream()
                        .filter((h) -> isFertileStallion(h) && !h.getLeashed() && !h.isVehicle()).min(this::highestHealth).orElse(null);
                if (competitor == null) {
                    return mare;
                }
                if (competitor.getMaxHealth() < horse.getMaxHealth()) {
                    horse.oust(competitor, mare);
                    return null;
                }
            }
        }

        AbstractHorse t = geneticEquines.stream().min(horse.isMale() ? this::nearestIdeallyMatchingClass : this::strongestIdeallyMatchingClass).orElse(null);
        if (t == null) {
            t = vanillaEquines.stream().min(this::highestHealth).orElse(null);
        }
        if (t != null && t.getMaxHealth() <= horse.getMaxHealth()) {
            return null;
        }
        return t;
    }

    public boolean isFertileMare(AbstractHorseGenetic h) {
        return !h.isChild() && !h.isMale() && h.isFertile();
    }

    public boolean isFertileStallion(AbstractHorseGenetic h) {
        return !h.isChild() && h.isMale() && h.isFertile();
    }

    @Override
    public void updateTask() {
        timeUntilRecalculatePath -= 1;
        if (timeUntilRecalculatePath < 0) {
            timeUntilRecalculatePath = adjustedTickDelay(10);
            double speed = horse.isChild() || horse.getDistanceSq(target) > 16 * 16 ? runSpeed : walkSpeed;
            horse.getNavigator().tryMoveToEntityLiving(target, speed);
        }
    }
    public boolean requiresUpdateEveryTick() {
        return false;
    }
    protected int adjustedTickDelay(int x) {
        return this.requiresUpdateEveryTick() ? x : reducedTickDelay(x);
    }

    protected static int reducedTickDelay(int x) {
        return positiveCeilDiv(x, 2);
    }

    public static int positiveCeilDiv(int x, int x1) {
        return -Math.floorDiv(-x, x1);
    }
}
