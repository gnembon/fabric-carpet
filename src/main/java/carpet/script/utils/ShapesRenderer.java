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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
        put("sphere", RenderedSphere::new);
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
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        //RenderSystem.shadeModel(7425);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        //RenderSystem.alphaFunc(GL11.GL_GREATER, 0.005F);
        RenderSystem.disableCull();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        //Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        // render
        synchronized (shapes)
        {
            shapes.get(dimensionType).int2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            shapes.get(dimensionType).values().forEach(
                    s -> s.render2pass(tessellator, bufferBuilder, (float) cameraX, (float) cameraY, (float) cameraZ)
            );
            //lines
            shapes.get(dimensionType).values().forEach(
                    s -> s.render(tessellator, bufferBuilder, (float) cameraX, (float) cameraY, (float) cameraZ)
            );
        }
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
    }

    public void addShape(CompoundTag tag)
    {
        ShapeDispatcher.ExpiringShape shape = ShapeDispatcher.fromTag(tag);
        if (shape == null) return;
        BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >> shapeFactory;
        shapeFactory = renderedShapes.get(tag.getString("shape"));
        if (shapeFactory == null)
        {
            CarpetSettings.LOG.info("Unrecognized shape: "+tag.getString("shape"));
        }
        else
        {
            RenderedShape<?> rshape = shapeFactory.apply(client, shape);
            RegistryKey<World> dim = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("dim")));
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
            if (shape.a == 0.0) return;
            RenderSystem.lineWidth(shape.lineWidth);
            drawBoxWireGLLines(tessellator, bufferBuilder,
                    shape.x1 - cx-renderEpsilon, shape.y1 - cy-renderEpsilon, shape.z1 - cz-renderEpsilon,
                    shape.x2 - cx+renderEpsilon, shape.y2 - cy+renderEpsilon, shape.z2 - cz+renderEpsilon,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }
        @Override
        public void render2pass(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            if (shape.fa == 0.0) return;
            RenderSystem.lineWidth(1.0F);

            drawBoxFaces(tessellator, bufferBuilder,
                    shape.x1-cx-renderEpsilon, shape.y1-cy-renderEpsilon, shape.z1-cz-renderEpsilon,
                    shape.x2-cx+renderEpsilon, shape.y2-cy+renderEpsilon, shape.z2-cz+renderEpsilon,
                    shape.fr, shape.fg, shape.fb, shape.fa
            );

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
            RenderSystem.lineWidth(shape.lineWidth);
            drawLine(tessellator, bufferBuilder,
                    shape.x1-cx-renderEpsilon, shape.y1-cy-renderEpsilon, shape.z1-cz-renderEpsilon,
                    shape.x2-cx+renderEpsilon, shape.y2-cy+renderEpsilon, shape.z2-cz+renderEpsilon,
                    shape.r, shape.g, shape.b, shape.a
            );
        }
    }

    public static class RenderedSphere extends RenderedShape<ShapeDispatcher.Sphere>
    {
        public RenderedSphere(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Sphere)shape);
        }
        @Override
        public void render(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            if (shape.a == 0.0) return;
            RenderSystem.lineWidth(shape.lineWidth);
            drawSphereWireframe(tessellator, bufferBuilder,
                    shape.cx-cx-renderEpsilon, shape.cy-cy-renderEpsilon, shape.cz-cz-renderEpsilon,
                    shape.radius, shape.subdivisions,
                    shape.r, shape.g, shape.b, shape.a);
        }
        @Override
        public void render2pass(Tessellator tessellator, BufferBuilder bufferBuilder, float cx, float cy, float cz)
        {
            if (shape.fa == 0.0) return;
            RenderSystem.lineWidth(1.0f);
            drawSphereFaces(tessellator, bufferBuilder,
                    shape.cx-cx-renderEpsilon, shape.cy-cy-renderEpsilon, shape.cz-cz-renderEpsilon,
                    shape.radius, shape.subdivisions,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    // some raw shit

    public static void drawLine(Tessellator tessellator, BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {
        builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
    }

    public static void drawBoxWireGLLines(Tessellator tessellator, BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2)
    {
        builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
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
        tessellator.draw();
    }

    public static void drawBoxFaces(Tessellator tessellator, BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha)
    {
        builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();

        builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
        tessellator.draw();
    }

    public static void drawSphereWireframe(Tessellator tessellator, BufferBuilder builder,
                                           float cx, float cy, float cz,
                                           float r, int subd,
                                           float red, float grn, float blu, float alpha)
    {
        float pihalf = (float)(Math.PI / 180.0f);
        float step = 180f / (subd/2);
        for (float i = 0f; i <= 360.0f; i+=step)
        {
            builder.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
            float theta = i * pihalf;
            for (float j = 0f; j <= 180.0f; j+=step) {
                float phi = j * pihalf;
                float x = r * MathHelper.sin(phi) * MathHelper.cos(theta);
                float z = r * MathHelper.sin(phi) * MathHelper.sin(theta);
                float y = r * MathHelper.cos(phi);
                builder.vertex(x+cx, y+cy, z+cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
        }
        for (float j = 0f; j <= 180.0f; j+=step)
        {
            builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
            float phi = j * pihalf;

            for (float i = 0f; i <= 360.0f; i+=step)
            {
                float theta = i * pihalf;
                float x = r * MathHelper.sin(phi) * MathHelper.cos(theta);
                float z = r * MathHelper.sin(phi) * MathHelper.sin(theta);
                float y = r * MathHelper.cos(phi);
                builder.vertex(x+cx, y+cy, z+cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
        }

    }

    public static void drawSphereFaces(Tessellator tessellator, BufferBuilder builder,
                                           float cx, float cy, float cz,
                                           float r, int subd,
                                           float red, float grn, float blu, float alpha)
    {
        float pihalf = (float)(Math.PI / 180.0f);
        float step = 180f / (subd/2);
        for (float i = 0f; i < 359.99f; i += step)
        {
            float theta = i * pihalf;
            float thetaprime = (i + step) * pihalf;
            builder.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION_COLOR);
            for (float j = 0f; j < 179.9f; j += step)
            {
                float phi = j * pihalf;
                float x = r * MathHelper.sin(phi) * MathHelper.cos(theta);
                float z = r * MathHelper.sin(phi) * MathHelper.sin(theta);
                float y = r * MathHelper.cos(phi);
                float xp = r * MathHelper.sin(phi) * MathHelper.cos(thetaprime);
                float zp = r * MathHelper.sin(phi) * MathHelper.sin(thetaprime);
                builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).next();
                builder.vertex(xp + cx, y + cy, zp + cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
        }
    }
}
