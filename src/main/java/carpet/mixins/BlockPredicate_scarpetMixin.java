package carpet.mixins;

import carpet.fakes.BlockPredicateInterface;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@Mixin(targets = "net.minecraft.commands.arguments.blocks.BlockPredicateArgument$BlockPredicate")
public class BlockPredicate_scarpetMixin implements BlockPredicateInterface
{

    @Shadow @Final private BlockState state;

    @Shadow @Final /*@Nullable*/ private CompoundTag nbt;

    @Shadow @Final private Set<Property<?>> properties;

    @Override
    public BlockState getCMBlockState()
    {
        return state;
    }

    @Override
    public TagKey<Block> getCMBlockTagKey()
    {
        return null;
    }

    @Override
    public Map<Value, Value> getCMProperties()
    {
        return properties.stream().collect(Collectors.toMap(
                p -> StringValue.of(p.getName()),
                p -> ValueConversions.fromProperty(state, p),
                (a, b) -> b
        ));
    }

    @Override
    public CompoundTag getCMDataTag()
    {
        return nbt;
    }
}
