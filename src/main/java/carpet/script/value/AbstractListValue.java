package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public abstract class AbstractListValue extends Value
{
    public abstract Iterator<Value> iterator();
    public void fatality() { }
}
