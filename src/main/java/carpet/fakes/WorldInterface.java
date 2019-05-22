package carpet.fakes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.Map;

public interface WorldInterface
{
    Map<EntityType, Entity> getPrecookedMobs();
}
