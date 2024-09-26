package carpet.mixins;

import carpet.fakes.FoodDataInterface;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FoodData.class)
public class FoodData_scarpetMixin implements FoodDataInterface
{
    @Shadow private float exhaustionLevel;

    @Override
    public float getCMExhaustionLevel()
    {
        return exhaustionLevel;
    }

    @Override
    public void setExhaustion(float aFloat)
    {
        exhaustionLevel = aFloat;
    }
}
