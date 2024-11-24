package sekelsta.horse_colors.entity.ai;


import net.minecraft.entity.ai.EntityAIBase;
import sekelsta.horse_colors.entity.AbstractHorseGenetic;

public class OustGoal extends EntityAIBase {

    public final AbstractHorseGenetic entity;
    public AbstractHorseGenetic target = null;
    public AbstractHorseGenetic stayNear = null;
    public float stayNearDistance = 16;
    public float maxDist = 18;
    public float runSpeed = 1.2f;
    public float walkSpeed = 0.9f;

    public OustGoal(AbstractHorseGenetic entityIn) {
        this.entity = entityIn;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (target == null) return false;
        if (stayNear == null) return true;
        if (entity.getDistanceSq(stayNear) > stayNearDistance * stayNearDistance) return false;
        return shouldContinueExecuting();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.isGroundTied() || entity.getLeashed() || entity.isVehicle() || target.getLeashed() || target.isVehicle()) return false;
        return entity.getDistanceSq(target) < maxDist * maxDist && !isDone() && (stayNear == null || entity.getDistanceSq(stayNear) < stayNearDistance * stayNearDistance);
    }

    public boolean isDone() {
        return entity.getNavigator().getPath() == null || entity.getNavigator().getPath().isFinished();
    }


    @Override
    public void startExecuting() {
        entity.getNavigator().tryMoveToEntityLiving(target, walkSpeed);
        target.fleeFrom(entity);
    }


    @Override
    public void updateTask() {
        if (entity.getDistanceSq(target) < 49.0) entity.getNavigator().setSpeed(walkSpeed);
        else entity.getNavigator().setSpeed(runSpeed);
    }
}

