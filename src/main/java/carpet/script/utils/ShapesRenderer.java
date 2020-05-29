package carpet.script.utils;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ShapesRenderer
{
    List<ExpiringShape> shapes;

    void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ){

    }
    void addShape() {}


    public abstract static class ExpiringShape
    {
        protected int r;
        protected int g;
        protected int b;
        protected int a;
        protected int expiryTick = 0;
        public abstract void render(MatrixStack matrix, VertexConsumer vertexConsumer);
        public ExpiringShape expiresIn(int ticks)
        {
            expiryTick = ticks;
            return this;
        }

    }

    public static class SolidLine extends ExpiringShape
    {
        public SolidLine(Vec3d from, Vec3d to, int r, int g, int b, int a)
        {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;

        }
        @Override
        public void render(MatrixStack matrix, VertexConsumer vertexConsumer)
        {

        }
    }

}
