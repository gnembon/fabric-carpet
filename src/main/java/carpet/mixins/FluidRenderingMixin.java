package carpet.mixins;

import carpet.script.utils.ShapesRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LiquidBlockRenderer.class)
public abstract class FluidRenderingMixin {
    @ModifyArgs(method = "vertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private void injected(Args args) {
        var pair = ShapesRenderer.DrawingShape.get();
        if (pair == null) return;
        float f = args.get(0);
        float g = args.get(1);
        float h = args.get(2);
        int x = pair.getSecond().getX() & 0xf;
        int y = pair.getSecond().getY() & 0xf;
        int z = pair.getSecond().getZ() & 0xf;
        var vec = new Vector4f(f - x, g - y, h - z, 1);
        pair.getFirst().pose().transform(vec);
        args.set(0, vec.x);
        args.set(1, vec.y);
        args.set(2, vec.z);
    }
}
