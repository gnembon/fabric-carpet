package carpet.mixins;

import carpet.fakes.TypeFilterableListInterface;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.TypeFilterableList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractCollection;
import java.util.List;

@Mixin(TypeFilterableList.class)
public abstract class TypeFilterableList_entityCollisionsMixin<T> extends AbstractCollection<T> implements TypeFilterableListInterface<T>
{
    @Shadow @Final private List<T> allElements;

    @Override
    public List<T> getAtIndices(IntList indices)
    {
        return null;
    }

    @Override
    public T getAtIndex(int index)
    {
        return allElements.get(index);
    }
}
