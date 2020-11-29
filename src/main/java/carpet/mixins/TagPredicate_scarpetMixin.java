package carpet.mixins;

import carpet.fakes.BlockPredicateInterface;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tag.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.stream.Collectors;

@Mixin(BlockPredicateArgumentType.TagPredicate.class)
public class TagPredicate_scarpetMixin implements BlockPredicateInterface
{
    @Shadow @Final private Tag<Block> tag;

    @Shadow @Final private Map<String, String> properties;

    @Shadow @Final /*@Nullable*/ private CompoundTag nbt;

    @Override
    public BlockState getCMBlockState()
    {
        return null;
    }

    @Override
    public Tag<Block> getCMBlockTag()
    {
        return tag;
    }

    @Override
    public Map<Value, Value> getCMProperties()
    {
        return properties.entrySet().stream().collect(Collectors.toMap(
                e -> new StringValue(e.getKey()),
                e -> new StringValue(e.getValue())
        ));
    }

    @Override
    public CompoundTag getCMDataTag()
    {
        return nbt;
    }
}
