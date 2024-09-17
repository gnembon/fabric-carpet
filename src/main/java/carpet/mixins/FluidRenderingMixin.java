package carpet.mixins;

import carpet.script.utils.ShapesRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LiquidBlockRenderer.class)
public abstract class FluidRenderingMixin {
    @WrapMethod(method = "vertex")
    void addingvertex(VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, float k, float l, float m, int n, Operation<Void> original) {
        var pair=ShapesRenderer.DrawingShape.get();
        if (pair!=null) {
            var x = pair.getSecond().getX();
            var y = pair.getSecond().getY();
            var z = pair.getSecond().getZ();
            x = (x & 0xf);
            y = (y & 0xf);
            z = (z & 0xf);
            var vec = new Vector4f(f - x, g - y, h - z, 1);
            pair.getFirst().pose().transform(vec);
            f = vec.x;
            g = vec.y;
            h = vec.z;
        }
        original.call(vertexConsumer, f, g, h, i, j, k, l, m, n);
    }
}
