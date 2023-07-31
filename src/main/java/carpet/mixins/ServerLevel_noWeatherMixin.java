package carpet.mixins;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_noWeatherMixin extends Level {

    protected ServerLevel_noWeatherMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey,
            RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl,
            boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
    }

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"))
    private void resetWeather(CallbackInfo ci) {
        if (CarpetSettings.noWeatherInSkylightlessDimension && !super.dimensionType().hasSkyLight()) {
            super.oRainLevel = super.rainLevel;
            super.oThunderLevel = super.thunderLevel;
            super.rainLevel = 0;
            super.thunderLevel = 0;
        }
    }
}
