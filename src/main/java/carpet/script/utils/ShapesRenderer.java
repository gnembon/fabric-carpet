package carpet.script.utils;

import carpet.CarpetSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Rotation3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<DimensionType, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private MinecraftClient client;

    private Map<String, BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >>> renderedShapes
            = new HashMap<String, BiFunction<MinecraftClient, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape>>>()
    {{
        put("line", RenderedLine::new);
        put("box", RenderedBox::new);
        put("sphere", RenderedSphere::new);
        put("cylinder", RenderedCylinder::new);
        put("label", RenderedText::new);
    }};

    public ShapesRenderer(MinecraftClient minecraftClient)
    {
        this.client = minecraftClient;
        shapes = new HashMap<>();
    }

    public void render(Camera camera, float partialTick)
    {
        IWorld iWorld = this.client.world;
        DimensionType dimensionType = iWorld.getDimension().getType();
        if (shapes.get(dimensionType) == null || shapes.get(dimensionType).isEmpty()) return;
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
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003f);
        RenderSystem.disableCull();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        // causes water to vanish
        //RenderSystem.depthMask(true);
        //RenderSystem.polygonOffset(-3f, -3f);
        //RenderSystem.enablePolygonOffset();
        //Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        // render
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        synchronized (shapes)
        {
            shapes.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );

            shapes.get(dimensionType).values().forEach(
                    s ->
                    {
                        if ( !s.lastCall() && s.shouldRender(dimensionType))
                            s.renderFaces(tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
                    }
            );
            //lines
            shapes.get(dimensionType).values().forEach(

                    s -> {
                        if (  !s.lastCall() && s.shouldRender(dimensionType))
                            s.renderLines(tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
                    }
            );
            //texts
            // maybe we can move it laster to lines and makes sure we don't overpass and don't have blinky transparency problems
            shapes.get(dimensionType).values().forEach(
                    s ->
                    {
                        if ( s.lastCall() && s.shouldRender(dimensionType))
                            s.renderLines(tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
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

    public void addShapes(ListTag tag)
    {
        for (int i=0, count = tag.size(); i < count; i++)
        {
            addShape(tag.getCompound(i));
        }
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
            DimensionType dim = shape.shapeDimension;
            long key = rshape.key();
            synchronized (shapes)
            {
                RenderedShape<?> existing = shapes.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>()).get(key);
                if (existing != null)
                {   // promoting previous shape
                    existing.promoteWith(rshape);
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
            shapes.values().forEach(Long2ObjectOpenHashMap::clear);
        }
    }


    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        protected T shape;
        protected MinecraftClient client;
        long expiryTick;
        double renderEpsilon = 0;
        public abstract void renderLines(Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick );
        public void renderFaces(Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick ) {}
        protected RenderedShape(MinecraftClient client, T shape)
        {
            this.shape = shape;
            expiryTick = client.world.getTime()+shape.getExpiry();
            renderEpsilon = (3+((double)shape.key())/Long.MAX_VALUE)/1000;
            this.client = client;
        }

        public boolean isExpired(long currentTick)
        {
            return  expiryTick < currentTick;
        }
        public long key()
        {
            return shape.key();
        };
        public boolean shouldRender(DimensionType dim)
        {
            if (shape.followEntity <=0 ) return true;
            if (client.world == null) return false;
            if (client.world.getEntityById(shape.followEntity) == null) return false;
            return true;
        }
        public boolean lastCall()
        {
            return false;
        }

        public void promoteWith(RenderedShape<?> rshape)
        {
            expiryTick = rshape.expiryTick;
        }
    }

    public static class RenderedText extends RenderedShape<ShapeDispatcher.Text>
    {

        protected RenderedText(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Text)shape);
        }

        @Override
        public void renderLines(Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d v1 = shape.relativiseRender(client.world, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getCamera();
            TextRenderer textRenderer = client.textRenderer;
            double d = camera1.getPos().x;
            double e = camera1.getPos().y;
            double f = camera1.getPos().z;
            if (shape.doublesided)
                RenderSystem.disableCull();
            else
                RenderSystem.enableCull();
            RenderSystem.pushMatrix();
            RenderSystem.translatef((float)(v1.x - d), (float)(v1.y - e), (float)(v1.z - f));
            RenderSystem.normal3f(0.0F, 1.0F, 0.0F);

            if (shape.facing == null)
            {
                RenderSystem.multMatrix(new Matrix4f(camera1.getRotation()));
            }
            else
            {
                switch (shape.facing)
                {
                    case NORTH:
                        break;
                    case SOUTH:
                        RenderSystem.rotatef(180.0f, 0.0f, 1.0f, 0.0f);
                        break;
                    case EAST:
                        RenderSystem.rotatef(270.0f, 0.0f, 1.0f, 0.0f);
                        break;
                    case WEST:
                        RenderSystem.rotatef(90.0f, 0.0f, 1.0f, 0.0f);
                        break;
                    case UP:
                        RenderSystem.rotatef(90.0f, 1.0f, 0.0f, 0.0f);
                        break;
                    case DOWN:
                        RenderSystem.rotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                        break;
                }
            }
            RenderSystem.scalef(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            if (shape.tilt!=0.0f)
            {
                RenderSystem.rotatef(shape.tilt, 0.0f, 0.0f, 1.0f);
            }
            RenderSystem.translatef(-10*shape.indent, -10*shape.height-9, (float) (-10*renderEpsilon)-10*shape.raise);
            //if (visibleThroughWalls) RenderSystem.disableDepthTest();
            RenderSystem.scalef(-1.0F, 1.0F, 1.0F);

            float text_x = 0;
            if (shape.align == 0)
            {
                text_x = (float)(-textRenderer.getStringWidth(shape.value)) / 2.0F;
            }
            else if (shape.align == 1)
            {
                text_x = (float)(-textRenderer.getStringWidth(shape.value));
            }
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            textRenderer.draw(shape.value, text_x, 0.0F, shape.textcolor, false, Rotation3.identity().getMatrix(), immediate, false, shape.textbck, 15728880);
            immediate.draw();
            RenderSystem.popMatrix();
            RenderSystem.enableCull();
            //RenderSystem.enableDepthTest();
            //RenderSystem.depthFunc(515);
            //RenderSystem.enableAlphaTest();
            //RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003f);
        }

        @Override
        public boolean lastCall()
        {
            return true;
        }

        @Override
        public void promoteWith(RenderedShape<?> rshape)
        {
            super.promoteWith(rshape);
            try
            {
                this.shape.value = ((ShapeDispatcher.Text) rshape.shape).value;
            }
            catch (ClassCastException ignored)
            {
                CarpetSettings.LOG.error("shape "+rshape.shape.getClass()+" cannot cast to a Label");
            }
        }
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {

        private RenderedBox(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Box)shape);

        }
        @Override
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d v1 = shape.relativiseRender(client.world, shape.from, partialTick);
            Vec3d v2 = shape.relativiseRender(client.world, shape.to, partialTick);
            RenderSystem.lineWidth(shape.lineWidth);
            drawBoxWireGLLines(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
                    v1.x!=v2.x, v1.y!=v2.y, v1.z!=v2.z,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }
        @Override
        public void renderFaces(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3d v1 = shape.relativiseRender(client.world, shape.from, partialTick);
            Vec3d v2 = shape.relativiseRender(client.world, shape.to, partialTick);
            RenderSystem.lineWidth(1.0F);
            drawBoxFaces(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
                    v1.x!=v2.x, v1.y!=v2.y, v1.z!=v2.z,
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
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            Vec3d v1 = shape.relativiseRender(client.world, shape.from, partialTick);
            Vec3d v2 = shape.relativiseRender(client.world, shape.to, partialTick);
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
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
            RenderSystem.lineWidth(shape.lineWidth);
            drawSphereWireframe(tessellator, bufferBuilder,
                    (float)(vc.x-cx), (float)(vc.y-cy), (float)(vc.z-cz),
                    (float)(shape.radius+renderEpsilon), shape.subdivisions,
                    shape.r, shape.g, shape.b, shape.a);
        }
        @Override
        public void renderFaces(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
            RenderSystem.lineWidth(1.0f);
            drawSphereFaces(tessellator, bufferBuilder,
                    (float)(vc.x-cx), (float)(vc.y-cy), (float)(vc.z-cz),
                    (float)(shape.radius+renderEpsilon), shape.subdivisions,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    public static class RenderedCylinder extends RenderedShape<ShapeDispatcher.Cylinder>
    {
        public RenderedCylinder(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Cylinder)shape);
        }
        @Override
        public void renderLines(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
            RenderSystem.lineWidth(shape.lineWidth);
            double dir = MathHelper.sign(shape.height);
            drawCylinderWireframe(tessellator, bufferBuilder,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2*dir*renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
                    shape.r, shape.g, shape.b, shape.a);

        }
        @Override
        public void renderFaces(Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
            RenderSystem.lineWidth(1.0f);
            double dir = MathHelper.sign(shape.height);
            drawCylinderFaces(tessellator, bufferBuilder,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2*dir*renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
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

    public static void drawBoxWireGLLines(
            Tessellator tessellator, BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2)
    {
        builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR); // 3
        if (xthick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn2, blu2, alpha).next();
            builder.vertex(x2, y1, z1).color(red1, grn2, blu2, alpha).next();

            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        }
        if (ythick)
        {
            builder.vertex(x1, y1, z1).color(red2, grn1, blu2, alpha).next();
            builder.vertex(x1, y2, z1).color(red2, grn1, blu2, alpha).next();

            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        }
        if (zthick)
        {
            builder.vertex(x1, y1, z1).color(red2, grn2, blu1, alpha).next();
            builder.vertex(x1, y1, z2).color(red2, grn2, blu1, alpha).next();

            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();

            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
        }
        tessellator.draw();
    }

    public static void drawBoxFaces(
            Tessellator tessellator, BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha)
    {
        builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        if (xthick && ythick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
            if (zthick)
            {
                builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
            }
        }


        if (zthick && ythick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();

            if (xthick)
            {
                builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
            }
        }

        // now at least drawing one
        if (zthick && xthick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).next();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).next();


            if (ythick)
            {
                builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).next();
                builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).next();
            }
        }
        tessellator.draw();
    }

    public static void drawCylinderWireframe(Tessellator tessellator, BufferBuilder builder,
                                             float cx, float cy, float cz,
                                             float r, float h, Direction.Axis axis,  int subd, boolean isFlat,
                                             float red, float grn, float blu, float alpha)
    {
        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step);
        int hsteps = 1;
        float hstep = 1.0f;
        if (!isFlat)
        {
            hsteps = (int) Math.ceil(MathHelper.abs(h) / (step * r)) + 1;
            hstep = h / (hsteps - 1);
        }// draw base

        if (axis == Direction.Axis.Y)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh*hstep;
                builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = hh;
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);

                    float z = r * MathHelper.sin(theta);

                    builder.vertex(cx - x, cy + 0, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + 0, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + h, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + h, cz + z).color(red, grn, blu, alpha).next();
                    tessellator.draw();
                }
            }
            else
            {
                builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx - x, cy, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy, cz - z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }

        }
        else if (axis == Direction.Axis.X)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float z = r * MathHelper.cos(theta);
                    float x = hh;
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);

                    float z = r * MathHelper.sin(theta);

                    builder.vertex(cx + 0, cy - y, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + 0, cy + y, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + h, cy + y, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + h, cy - y, cz + z).color(red, grn, blu, alpha).next();
                    tessellator.draw();
                }
            }
            else
            {
                builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx, cy - y, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx, cy + y, cz - z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = hh;
                    float x = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }
            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);

                    float y = r * MathHelper.sin(theta);

                    builder.vertex(cx + x, cy - y, cz + 0).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + y, cz + 0).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + y, cz + h).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy - y, cz + h).color(red, grn, blu, alpha).next();
                    tessellator.draw();
                }
            }
            else
            {
                builder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(cx + x, cy - y, cz).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + y, cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }

        }
    }

    public static void drawCylinderFaces(Tessellator tessellator, BufferBuilder builder,
                                             float cx, float cy, float cz,
                                             float r, float h, Direction.Axis axis,  int subd, boolean isFlat,
                                             float red, float grn, float blu, float alpha)
    {
        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step)+1;

        if (axis == Direction.Axis.Y)
        {

            builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).next();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * MathHelper.cos(theta);
                float z = r * MathHelper.sin(theta);
                builder.vertex(x + cx, cy, z + cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
            if (!isFlat)
            {
                builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx, cy+h, cz).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, cy+h, z + cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx + x, cy + 0, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + h, cz + z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }

        }
        else if (axis == Direction.Axis.X)
        {
            builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).next();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float y = r * MathHelper.cos(theta);
                float z = r * MathHelper.sin(theta);
                builder.vertex(cx, cy + y, z + cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
            if (!isFlat)
            {
                builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx+h, cy, cz).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx+h, cy + y, cz + z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx + 0, cy + y, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + h, cy + y, cz + z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).next();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * MathHelper.cos(theta);
                float y = r * MathHelper.sin(theta);
                builder.vertex(x + cx, cy+y, cz).color(red, grn, blu, alpha).next();
            }
            tessellator.draw();
            if (!isFlat)
            {
                builder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx, cy, cz+h).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, cy+y, cz+h).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(cx + x, cy + y, cz + 0).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + y, cz + h).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();
            }
        }
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
