package carpet.script.utils;

import carpet.CarpetSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<DimensionType, Int2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
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
        shapes.put(DimensionType.OVERWORLD, new Int2ObjectOpenHashMap<>());
        shapes.put(DimensionType.THE_NETHER, new Int2ObjectOpenHashMap<>());
        shapes.put(DimensionType.THE_END, new Int2ObjectOpenHashMap<>());
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ)
    {
        IWorld iWorld = this.client.world;
        DimensionType dimensionType = iWorld.getDimension().getType();
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
        //RenderSystem.polygonOffset(-3f, -3f);
        //RenderSystem.enablePolygonOffset();
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
                    s ->
                    {
                        if ( s.shouldRender(dimensionType))
                            s.renderFaces(tessellator, bufferBuilder, cameraX, cameraY, cameraZ);
                    }
            );
            //lines
            shapes.get(dimensionType).values().forEach(

                    s -> {
                        if ( s.shouldRender(dimensionType))
                            s.renderLines(tessellator, bufferBuilder, cameraX, cameraY, cameraZ);
                    }
            );
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
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
        protected MinecraftClient client;
        long expiryTick;
        double renderEpsilon = 0;
        public abstract void renderLines(Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz );
        public void renderFaces(Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz ) {}
        protected RenderedShape(MinecraftClient client, T shape)
        {
            this.shape = shape;
            expiryTick = client.world.getTime()+shape.getExpiry();
            renderEpsilon = (2+((double)shape.key())/Integer.MAX_VALUE)/1000;
            this.client = client;
        }

        public boolean isExpired(long currentTick)
        {
            return  expiryTick < currentTick;
        }
        public int key()
        {
            return shape.key();
        };
        public boolean shouldRender(DimensionType dim)
        {
            if (shape.followEntity <=0 ) return true;
            if (client.world == null) return false;
            if (client.world.dimension.getType() != dim) return false;
            if (client.world.getEntityById(shape.followEntity) == null) return false;
            return true;
        }
        protected Vec3d relativiseRender(Vec3d vec)
        {
            if (shape.followEntity <= 0) return vec;
            Entity e = client.world.getEntityById(shape.followEntity);
            return shape.toAbsolute(e, vec);
        }
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {

        private RenderedBox(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Box)shape);

        }
        @Override
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz)
        {
            if (shape.a == 0.0) return;
            Vec3d v1 = relativiseRender(shape.from);
            Vec3d v2 = relativiseRender(shape.to);
            RenderSystem.lineWidth(shape.lineWidth);
            drawBoxWireGLLines(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }
        @Override
        public void renderFaces(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz)
        {
            if (shape.fa == 0.0) return;
            Vec3d v1 = relativiseRender(shape.from);
            Vec3d v2 = relativiseRender(shape.to);
            RenderSystem.lineWidth(1.0F);
            drawBoxFaces(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
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
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz)
        {
            Vec3d v1 = relativiseRender(shape.from);
            Vec3d v2 = relativiseRender(shape.to);
            RenderSystem.lineWidth(shape.lineWidth);
            drawLine(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
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
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz)
        {
            if (shape.a == 0.0) return;
            Vec3d vc = relativiseRender(shape.center);
            RenderSystem.lineWidth(shape.lineWidth);
            drawSphereWireframe(tessellator, bufferBuilder,
                    (float)(vc.x-cx-renderEpsilon), (float)(vc.y-cy-renderEpsilon), (float)(vc.z-cz-renderEpsilon),
                    shape.radius, shape.subdivisions,
                    shape.r, shape.g, shape.b, shape.a);
        }
        @Override
        public void renderFaces(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz)
        {
            if (shape.fa == 0.0) return;
            Vec3d vc = relativiseRender(shape.center);
            RenderSystem.lineWidth(1.0f);
            drawSphereFaces(tessellator, bufferBuilder,
                    (float)(vc.x-cx-renderEpsilon), (float)(vc.y-cy-renderEpsilon), (float)(vc.z-cz-renderEpsilon),
                    shape.radius, shape.subdivisions,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    // some raw shit

    public static void drawLine(Tessellator tessellator, BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {
        builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        tessellator.draw();
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
        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step);
        for (int i = 0; i <= num_steps360; i++)
        {
            builder.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
            float theta = step * i ;
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = step * j ;
                float x = r * MathHelper.sin(phi) * MathHelper.cos(theta);
                float z = r * MathHelper.sin(phi) * MathHelper.sin(theta);
                float y = r * MathHelper.cos(phi);
                builder.vertex(x+cx, y+cy, z+cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
        }
        for (int j = 0; j <= num_steps180; j++)
        {
            builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
            float phi = step * j ;

            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
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

        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step);
        for (int i = 0; i <= num_steps360; i++)
        {
            float theta = i * step;
            float thetaprime = theta+step;
            builder.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION_COLOR);
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = j * step;
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
