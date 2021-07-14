package carpet.fakes;

import carpet.script.value.Value;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.tag.Tag;

import java.util.Map;

public interface BlockPredicateInterface
{
    BlockState getCMBlockState();
    Tag<Block> getCMBlockTag();
    Map<Value, Value> getCMProperties();
    NbtCompound getCMDataTag();
}
