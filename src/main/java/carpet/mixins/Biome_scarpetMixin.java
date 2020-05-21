package carpet.mixins;

import carpet.fakes.BiomeInterface;
import net.minecraft.class_5312;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(Biome.class)
public class Biome_scarpetMixin implements BiomeInterface
{
    @Shadow @Final private Map<StructureFeature<?>, class_5312<?, ?>> structureFeatures;

    @Override
    public class_5312<?, ?> getConfiguredFeature(StructureFeature<?> arg)
    {
        return structureFeatures.get(arg);
    }
}
