package carpet.fakes;

import carpet.script.value.Value;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockPredicateInterface
{
    default BlockState carpet$getBlockState() { throw new UnsupportedOperationException(); }

    default TagKey<Block> carpet$getTagKey() { throw new UnsupportedOperationException(); }

    default Map<Value, Value> carpet$getProperties() { throw new UnsupportedOperationException(); }

    default CompoundTag carpet$getDataTag() { throw new UnsupportedOperationException(); }
}
