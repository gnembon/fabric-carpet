package carpet.mixins;

import carpet.fakes.BlockPredicateInterface;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(targets = "net.minecraft.commands.arguments.blocks.BlockPredicateArgument$TagPredicate")
public class TagPredicate_scarpetMixin implements BlockPredicateInterface
{
    @Shadow @Final private HolderSet<Block> tag;

    @Shadow @Final private Map<String, String> vagueProperties;

    @Shadow @Final /*@Nullable*/ private CompoundTag nbt;

    @Override
    public BlockState getCMBlockState()
    {
        return null;
    }

    @Override
    public TagKey<Block> getCMBlockTagKey()
    {
        // might be good to explose the holder set nature of it.
        return tag.unwrap().left().orElse(null);
    }

    @Override
    public Map<Value, Value> getCMProperties()
    {
        return vagueProperties.entrySet().stream().collect(Collectors.toMap(
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
