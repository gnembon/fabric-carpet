package carpet.mixins;

import carpet.fakes.SimpleEntityLookupInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;

@Mixin(LevelEntityGetterAdapter.class)
public class LevelEntityGetterAdapter_scarpetMixin<T extends EntityAccess> implements SimpleEntityLookupInterface<T>
{

    @Shadow @Final private EntitySectionStorage<T> sectionStorage;

    @Override
    public List<T> getChunkEntities(ChunkPos chpos) {
        return this.sectionStorage.getExistingSectionsInChunk(chpos.toLong()).flatMap(EntitySection::getEntities).collect(Collectors.toList());
    }
}


