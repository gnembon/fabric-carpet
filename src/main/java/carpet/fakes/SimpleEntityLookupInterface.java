package carpet.fakes;

import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;

public interface SimpleEntityLookupInterface<T extends EntityAccess>
{
    List<T> getChunkEntities(ChunkPos chpos);
}
