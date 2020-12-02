package carpet.mixins;

import carpet.fakes.BiomeArrayInterface;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeArray.class)
public class BiomeArray_getBiomeArrayMixin implements BiomeArrayInterface
{

    @Shadow @Final private static int HORIZONTAL_SECTION_COUNT;

    @Shadow @Final public static int HORIZONTAL_BIT_MASK;

    //@Shadow @Final public static int VERTICAL_BIT_MASK;

    @Shadow @Final private Biome[] data;

    @Shadow @Final private int field_28126;

    @Shadow @Final private int field_28127;

    @Override
    public void setBiomeAtIndex(BlockPos pos, World world, Biome what)
    {
        int int_4 = (pos.getX() >> 2) & HORIZONTAL_BIT_MASK;
        int int_5 = MathHelper.clamp((pos.getY() >> 2)-field_28126, 0, field_28127);
        int int_6 = (pos.getZ() >> 2) & HORIZONTAL_BIT_MASK;
        data[(int_5 << (HORIZONTAL_SECTION_COUNT + HORIZONTAL_SECTION_COUNT)) | (int_6 << HORIZONTAL_SECTION_COUNT) | int_4] = what;
    }
}
