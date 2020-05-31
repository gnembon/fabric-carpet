package carpet.script.utils;

import carpet.CarpetSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<RegistryKey<World>, List<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
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
        shapes.put(World.field_25179, new ArrayList<>());
        shapes.put(World.field_25180, new ArrayList<>());
        shapes.put(World.field_25181, new ArrayList<>());
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ)
    {
        //Camera camera = this.client.gameRenderer.getCamera();
        ClientWorld iWorld = this.client.world;
        RegistryKey<World> dimensionType = iWorld.method_27983();
        if (shapes.get(dimensionType).isEmpty()) return;
        long currentTime = client.world.getTime();

        RenderSystem.enableDepthTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        double d = 0.0D - cameraY;
        double e = 256.0D - cameraY;
        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        double f = (double)(entity.chunkX << 4) - cameraX;
        double g = (double)(entity.chunkZ << 4) - cameraZ;

        // render
        synchronized (shapes)
        {
            Iterator<RenderedShape<? extends ShapeDispatcher.ExpiringShape>> it = shapes.get(dimensionType).iterator();
            while (it.hasNext())
            {
                RenderedShape shape = it.next();
                if (shape.isExpired(currentTime)) it.remove();
                shape.render(tessellator, bufferBuilder, (float) cameraX, (float) cameraY, (float) cameraZ);
            }
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
    }

    public void addShape(String type, RegistryKey<World> dim, CompoundTag tag)
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
        public abstract void render(Tessellator tessellator, BufferBuilder builder, float cx, float cy, float cz );
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
        public void render(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            RenderSystem.lineWidth(2.0F);
            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
            drawBox(bufferBuilder,
                    shape.x1-cx, shape.y1-cy, shape.z1-cz,
                    shape.x2-cx, shape.y2-cy, shape.z2-cz,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
            tessellator.draw();
        }

    }

    public static class RenderedLine extends RenderedShape<ShapeDispatcher.Line>
    {
        public RenderedLine(MinecraftClient client, CompoundTag boxData)
        {
            super(client, (ShapeDispatcher.Line)ShapeDispatcher.Line.fromTag(boxData));
        }
        @Override
        public void render(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            RenderSystem.lineWidth(2.0F);
            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
            drawLine(bufferBuilder,
                    shape.x1-cx, shape.y1-cy, shape.z1-cz,
                    shape.x2-cx, shape.y2-cy, shape.z2-cz,
                    shape.r, shape.g, shape.b, shape.a
            );
            tessellator.draw();
        }
    }

    // some raw shit

    public static void drawLine(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {

        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
    }

    public static void drawBox(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2) {
        builder.vertex(x1, y1, z1).color(red1, grn2, blu2, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn2, blu2, alpha).next();
        builder.vertex(x1, y1, z1).color(red2, grn1, blu2, alpha).next();
        builder.vertex(x1, y2, z1).color(red2, grn1, blu2, alpha).next();
        builder.vertex(x1, y1, z1).color(red2, grn2, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red2, grn2, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
    }
}
