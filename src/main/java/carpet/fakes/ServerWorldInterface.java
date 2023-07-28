package carpet.fakes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.ServerLevelData;

public interface ServerWorldInterface {
    ServerLevelData getWorldPropertiesCM();
    LevelEntityGetter<Entity> getEntityLookupCMPublic();
}
