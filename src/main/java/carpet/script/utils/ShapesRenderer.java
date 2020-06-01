package carpet.script.utils;

import carpet.CarpetSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<RegistryKey<World>, Int2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private MinecraftClient client;

    private Map<String, BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >>> renderedShapes
            = new HashMap<String, BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape>>>()
    {{
        put("line", RenderedLine::new);
        put("box", RenderedBox::new);
    }};

    public ShapesRenderer(MinecraftClient minecraftClient)
    {
        this.client = minecraftClient;
        shapes = new HashMap<>();
        shapes.put(World.field_25179, new Int2ObjectOpenHashMap<>());
        shapes.put(World.field_25180, new Int2ObjectOpenHashMap<>());
        shapes.put(World.field_25181, new Int2ObjectOpenHashMap<>());
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ)
    {
        //Camera camera = this.client.gameRenderer.getCamera();
        ClientWorld iWorld = this.client.world;
        RegistryKey<World> dimensionType = iWorld.method_27983();
        if (shapes.get(dimensionType).isEmpty()) return;
        long currentTime = client.world.getTime();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.shadeModel(7425);
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        double d = 0.0D - cameraY;
        double e = 256.0D - cameraY;
        RenderSystem.disableTexture();
        //RenderSystem.disableBlend();
        double f = (double)(entity.chunkX << 4) - cameraX;
        double g = (double)(entity.chunkZ << 4) - cameraZ;
        // render
        synchronized (shapes)
        {
            shapes.get(dimensionType).int2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            shapes.get(dimensionType).values().forEach(
                    s -> s.render(tessellator, bufferBuilder, (float) cameraX, (float) cameraY, (float) cameraZ)
            );
            shapes.get(dimensionType).values().forEach(
                    s -> s.render2pass(tessellator, bufferBuilder, (float) cameraX, (float) cameraY, (float) cameraZ)
            );
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
    }

    public void addShape(CompoundTag tag)
    {
        //CarpetSettings.LOG.error("Received tag: "+tag.asString());
        ShapeDispatcher.ExpiringShape shape = ShapeDispatcher.fromTag(tag);
        if (shape == null) return;
        BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >> shapeFactory;
        shapeFactory = renderedShapes.get(tag.getString("shape"));



        if (shapeFactory == null)
        {
            CarpetSettings.LOG.error("Unrecognized shape: "+tag.getString("shape"));
        }
        else
        {
            RenderedShape<?> rshape = shapeFactory.apply(client, shape);
            DimensionType dim = Registry.DIMENSION_TYPE.get(new Identifier(tag.getString("dim")));
            int key = rshape.key();
            synchronized (shapes)
            {
                RenderedShape<?> existing = shapes.get(dim).get(key);
                if (existing != null)
                {   // promoting previous shape
                    existing.expiryTick = rshape.expiryTick;
                }
                else
                {
                    shapes.get(dim).put(key, rshape);
                }
            }
        }
    }
    public void reset()
    {
        synchronized (shapes)
        {
            shapes.values().forEach(Int2ObjectOpenHashMap::clear);
        }
    }


    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        protected T shape;
        long expiryTick;
        float renderEpsilon = 0;
        public abstract void render(Tessellator tessellator, BufferBuilder builder, float cx, float cy, float cz );
        public void render2pass(Tessellator tessellator, BufferBuilder builder, float cx, float cy, float cz ) {};
        protected RenderedShape(MinecraftClient client, T shape)
        {
            this.shape = shape;
            expiryTick = client.world.getTime()+shape.getExpiry();
            renderEpsilon = (2+((float)shape.key())/Integer.MAX_VALUE)/1000;
        }

        public boolean isExpired(long currentTick)
        {
            return  expiryTick < currentTick;
        }
        public int key()
        {
            return shape.key();
        };
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {

        private RenderedBox(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Box)shape);

        }
        @Override
        public void render(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            RenderSystem.lineWidth(2.0F);
            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
            drawBoxWireGLLines(bufferBuilder,
                    shape.x1 - cx-renderEpsilon, shape.y1 - cy-renderEpsilon, shape.z1 - cz-renderEpsilon,
                    shape.x2 - cx+renderEpsilon, shape.y2 - cy+renderEpsilon, shape.z2 - cz+renderEpsilon,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
            tessellator.draw();
        }
        @Override
        public void render2pass(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            RenderSystem.lineWidth(1.0F);
            bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR); // 3
            drawBox(bufferBuilder,
                    shape.x1-cx-renderEpsilon, shape.y1-cy-renderEpsilon, shape.z1-cz-renderEpsilon,
                    shape.x2-cx+renderEpsilon, shape.y2-cy+renderEpsilon, shape.z2-cz+renderEpsilon,
                    shape.r, shape.g, shape.b, shape.a/6
            );
            tessellator.draw();
        }
    }

    public static class RenderedLine extends RenderedShape<ShapeDispatcher.Line>
    {
        public RenderedLine(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Line)shape);
        }
        @Override
        public void render(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            RenderSystem.lineWidth(2.0F);
            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
            drawLine(bufferBuilder,
                    shape.x1-cx-renderEpsilon, shape.y1-cy-renderEpsilon, shape.z1-cz-renderEpsilon,
                    shape.x2-cx+renderEpsilon, shape.y2-cy+renderEpsilon, shape.z2-cz+renderEpsilon,
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

    public static void drawBoxWireGLLines(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2) {
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

    public static void drawBox(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {
        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
    }
}
