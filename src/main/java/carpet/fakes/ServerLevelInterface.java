package carpet.fakes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.ServerLevelData;

public interface ServerLevelInterface
{
    default ServerLevelData carpet$getLevelData() { throw new UnsupportedOperationException(); }

    default LevelEntityGetter<Entity> carpet$getEntityGetter() { throw new UnsupportedOperationException(); }
}
