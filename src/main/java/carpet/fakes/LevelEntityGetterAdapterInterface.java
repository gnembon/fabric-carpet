package carpet.fakes;

import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;

public interface LevelEntityGetterAdapterInterface<T extends EntityAccess>
{
    default List<T> carpet$getChunkEntities(ChunkPos pos) { throw new UnsupportedOperationException(); }
}
