package carpet.fakes;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.level.ServerWorldProperties;

public interface ServerWorldInterface {
    ServerWorldProperties getWorldPropertiesCM();
    EntityLookup<Entity> getEntityLookupCMPublic();
}
