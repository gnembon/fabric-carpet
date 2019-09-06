package carpet.mixins;

import carpet.fakes.class_4548Interface;
import carpet.settings.CarpetSettings;
import net.minecraft.class_4548;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.class_4548.field_20650;
import static net.minecraft.class_4548.field_20651;

@Mixin(class_4548.class)
public class class_4548_getBiomeArrayMixin implements class_4548Interface
{

    @Shadow @Final private Biome[] field_20654;

    @Shadow @Final private static int field_20652;

    @Override
    public void setBiomeAtIndex(BlockPos pos, World world, Biome what)
    {
        int seaLevel = 0;// seems to be a fixed value for now. world.getSeaLevel();
        int int_4 = (pos.getX() >> 2) & field_20650;
        int int_5 = MathHelper.clamp(seaLevel, 0, field_20651);
        int int_6 = (pos.getZ() >> 2) & field_20650;
        field_20654[(int_5 << (field_20652 + field_20652)) | (int_6 << field_20652) | int_4] = what;
    }
}
