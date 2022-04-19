package carpet.fakes;

import carpet.script.value.Value;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockPredicateInterface
{
    BlockState getCMBlockState();
    TagKey<Block> getCMBlockTagKey();
    Map<Value, Value> getCMProperties();
    CompoundTag getCMDataTag();
}
