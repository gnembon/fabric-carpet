package carpet.fakes;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityLike;

import java.util.List;

public interface SimpleEntityLookupInterface<T extends EntityLike>
{
    List<T> getChunkEntities(ChunkPos chpos);
}
