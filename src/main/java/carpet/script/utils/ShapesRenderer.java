package carpet.script.utils;

import carpet.CarpetSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<DimensionType, List<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private MinecraftClient client;

    private Map<String, BiFunction<MinecraftClient, CompoundTag, RenderedShape<? extends ShapeDispatcher.ExpiringShape >>> renderedShapes
            = new HashMap<String, BiFunction<MinecraftClient, CompoundTag, RenderedShape<? extends ShapeDispatcher.ExpiringShape>>>()
    {{
        put("debugBox", RenderedBox::new);
        put("debugLine", RenderedLine::new);
    }};

    public ShapesRenderer(MinecraftClient minecraftClient)
    {
        this.client = minecraftClient;
        shapes = new HashMap<>();
        shapes.put(DimensionType.OVERWORLD, new ArrayList<>());
        shapes.put(DimensionType.THE_NETHER, new ArrayList<>());
        shapes.put(DimensionType.THE_END, new ArrayList<>());
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ)
    {
        //Camera camera = this.client.gameRenderer.getCamera();
        IWorld iWorld = this.client.world;
        DimensionType dimensionType = iWorld.getDimension().getType();
        if (shapes.get(dimensionType).isEmpty()) return;
        //BlockPos blockPos = new BlockPos(camera.getPos().x, 0.0D, camera.getPos().z);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        long currentTime = client.world.getTime();

        synchronized (shapes)
        {
            Iterator<RenderedShape<? extends ShapeDispatcher.ExpiringShape>> it = shapes.get(dimensionType).iterator();
            while (it.hasNext())
            {
                RenderedShape shape = it.next();
                if (shape.isExpired(currentTime)) it.remove();
                shape.render(matrices, vertexConsumer, (float) cameraX, (float) cameraY, (float) cameraZ);
            }
        }
    }
    public void addShape(String type, DimensionType dim, CompoundTag tag)
    {
        BiFunction<MinecraftClient, CompoundTag, RenderedShape<? extends ShapeDispatcher.ExpiringShape >> shapeFactory;
        shapeFactory = renderedShapes.get(type);
        if (shapeFactory == null)
        {
            CarpetSettings.LOG.error("Unrecognized shape: "+type);
        }
        else
        {
            synchronized (shapes) { shapes.get(dim).add(shapeFactory.apply(client, tag)); }
        }
    }
    public void reset()
    {
        synchronized (shapes)
        {
            shapes.values().forEach(List::clear);
        }
    }


    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        T shape;
        long expiryTick;
        public abstract void render(MatrixStack matrices, VertexConsumer vertexConsumer, float cx, float cy, float cz );
        protected RenderedShape(MinecraftClient client, T shape)
        {
            this.shape = shape;
            expiryTick = client.world.getTime()+shape.getExpiry();
        }

        public boolean isExpired(long currentTick)
        {
            return  expiryTick < currentTick;
        }
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {
        public RenderedBox(MinecraftClient client, CompoundTag boxData)
        {
            super(client, (ShapeDispatcher.Box)ShapeDispatcher.Box.fromTag(boxData));
        }
        @Override
        public void render(MatrixStack matrices, VertexConsumer vertexConsumer, float cx, float cy, float cz)
        {
            drawBox(matrices, vertexConsumer,
                    shape.x1-cx, shape.y1-cy, shape.z1-cz,
                    shape.x2-cx, shape.y2-cy, shape.z2-cz,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }
    }

    public static class RenderedLine extends RenderedShape<ShapeDispatcher.Line>
    {
        public RenderedLine(MinecraftClient client, CompoundTag boxData)
        {
            super(client, (ShapeDispatcher.Line)ShapeDispatcher.Line.fromTag(boxData));
        }
        @Override
        public void render(MatrixStack matrices, VertexConsumer vertexConsumer, float cx, float cy, float cz)
        {
            drawLine(matrices, vertexConsumer,
                    shape.x1-cx, shape.y1-cy, shape.z1-cz,
                    shape.x2-cx, shape.y2-cy, shape.z2-cz,
                    shape.r, shape.g, shape.b, shape.a
            );
        }
    }

    // some raw shit

    public static void drawLine(MatrixStack matrix, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {
        Matrix4f matrix4f = matrix.peek().getModel();
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(red1, grn1, blu1, alpha).next();
    }

    public static void drawBox(MatrixStack matrix, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2) {
        Matrix4f matrix4f = matrix.peek().getModel();
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(red1, grn2, blu2, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z1).color(red1, grn2, blu2, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(red2, grn1, blu2, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z1).color(red2, grn1, blu2, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(red2, grn2, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y1, z2).color(red2, grn2, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(red1, grn1, blu1, alpha).next();
    }
}
