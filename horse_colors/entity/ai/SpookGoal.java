package sekelsta.horse_colors.entity.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.AbstractHorse;

public class SpookGoal extends EntityAIAvoidEntity<EntityMob> {

    public SpookGoal(EntityCreature entity) {
        super(entity, EntityMob.class, 8.0F, 1.5, 1.5);
    }

    @Override
    public void startExecuting() {
        AbstractHorse horse = (AbstractHorse)this.entity;
        if (horse.isBeingRidden()) {
            horse.removePassengers();
            horse.makeMad();
        }
        super.startExecuting();
    }
}
