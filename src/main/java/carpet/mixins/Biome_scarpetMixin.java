package carpet.mixins;

import carpet.fakes.BiomeInterface;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public class Biome_scarpetMixin implements BiomeInterface {
    @Shadow
    private Biome.ClimateSettings climateSettings;

    @Override
    public Biome.ClimateSettings getClimateSettings() {
        return climateSettings;
    }
}
