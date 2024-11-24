package sekelsta.horse_colors.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;


import net.minecraft.util.math.Vec3d;
import sekelsta.horse_colors.entity.AbstractHorseGenetic;

public class FleeGoal extends EntityAIBase {
    public final AbstractHorseGenetic entity;
    public Entity toAvoid = null;
    public float maxDist = 24;
    public float runSpeed = 1.6f;
    public float walkSpeed = 1f;
    protected Path path = null;

    public FleeGoal(AbstractHorseGenetic entityIn) {
        this.entity = entityIn;
        this.setMutexBits(1);
    }


    @Override
    public boolean shouldExecute() {
        if (toAvoid == null) {
            return false;
        }
        if (!shouldContinueExecuting()) {
            return false;
        }
        setPath();
        return path != null;
    }

    private void setPath() {
//        path = null;
//        Vec3d loc = LandRandomPos.getPosAway(entity, 16, 7, toAvoid.position());
//        if (loc == null) {
//            loc = DefaultRandomPos.getPosAway(entity, 12, 7, toAvoid.position());
//        }
//        if (loc != null) {
//            path = entity.getNavigator().setPath(loc.x, loc.y, loc.z, 0);
//        }
    }

    public boolean isDone() {
        return entity.getNavigator().getPath() == null || entity.getNavigator().getPath().isFinished();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.isGroundTied() || entity.getLeashed() || entity.isVehicle()) {
            return false;
        }
        return entity.getDistanceSq(toAvoid) < maxDist * maxDist;
    }

    @Override
    public void startExecuting() {
        entity.getNavigator().tryMoveToXYZ(path.getCurrentPos().x, path.getCurrentPos().y, path.getCurrentPos().z, walkSpeed);
    }

    @Override
    public void updateTask() {
        if (isDone()) {
            setPath();
            entity.getNavigator().tryMoveToXYZ(path.getCurrentPos().x, path.getCurrentPos().y, path.getCurrentPos().z, walkSpeed);
        }
        if (entity.getDistanceSq(toAvoid) < 49.0) {
            entity.getNavigator().setSpeed(runSpeed);
        }
        else {
            entity.getNavigator().setSpeed(walkSpeed);
        }
    }
}

