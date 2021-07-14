package carpet.mixins;

import carpet.fakes.SimpleEntityLookupInterface;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(SimpleEntityLookup.class)
public class SimpleEntityLookup_scarpetMixin<T extends EntityLike> implements SimpleEntityLookupInterface
{

    @Shadow @Final private SectionedEntityCache<T> cache;

    @Override
    public List<T> getChunkEntities(ChunkPos chpos) {
        return this.cache.getTrackingSections(chpos.toLong()).flatMap(EntityTrackingSection::stream).collect(Collectors.toList());
    }
}


