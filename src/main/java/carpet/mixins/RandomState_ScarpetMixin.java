package carpet.mixins;

import carpet.fakes.RandomStateInterface;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RandomState.class)
public class RandomState_ScarpetMixin implements RandomStateInterface {
    @Unique
    private DensityFunction.Visitor visitor;

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
            ),
            index = 0
    )
    private DensityFunction.Visitor captureVisitor(DensityFunction.Visitor visitor) {
        this.visitor = visitor;
        return visitor;
    }

    @Override
    public DensityFunction.Visitor carpet$getVisitor() {
        return this.visitor;
    }
}
