package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.utils.CarpetProfiler;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ShapesRenderer
{
    private final Map<RegistryKey<World>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private final Map<RegistryKey<World>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> labels;
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
        labels = new HashMap<>();
    }

    public void render(MatrixStack matrices, Camera camera, float partialTick)
    {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        //Camera camera = this.client.gameRenderer.getCamera();
        ClientWorld iWorld = this.client.world;
        RegistryKey<World> dimensionType = iWorld.getRegistryKey();
        if ((shapes.get(dimensionType) == null || shapes.get(dimensionType).isEmpty()) &&
                (labels.get(dimensionType) == null || labels.get(dimensionType).isEmpty())) return;
        long currentTime = client.world.getTime();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthFunc(515);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // too bright
        //RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        // meh
        //RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        //RenderSystem.polygonOffset(-3f, -3f);
        //RenderSystem.enablePolygonOffset();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        // render
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        if (shapes.size() != 0) { synchronized (shapes) {
            shapes.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();
            matrixStack.method_34425(matrices.peek().getPositionMatrix());
            RenderSystem.applyModelViewMatrix();

            // lines
            RenderSystem.lineWidth(0.5F);
            shapes.get(dimensionType).values().forEach( s -> {
                if (s.shouldRender(dimensionType)) s.renderLines(matrices, tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
            // faces
            RenderSystem.lineWidth(0.1F);
            shapes.get(dimensionType).values().forEach(s -> {
                        if (s.shouldRender(dimensionType)) s.renderFaces(tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
            RenderSystem.lineWidth(1.0F);
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();

        }}
        if (labels.size() != 0) { synchronized (labels)
        {
            labels.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            labels.get(dimensionType).values().forEach(s -> {
                if (s.shouldRender(dimensionType)) s.renderLines(matrices, tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
        }}
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        CarpetProfiler.end_current_section(token);
    }

    public void addShapes(NbtList tag)
    {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        for (int i=0, count = tag.size(); i < count; i++)
        {
            addShape(tag.getCompound(i));
        }
        CarpetProfiler.end_current_section(token);
    }

    public void addShape(NbtCompound tag)
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
            RegistryKey<World> dim = shape.shapeDimension;
            long key = rshape.key();
            Map<RegistryKey<World>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> container =
                    rshape.stageDeux()?labels:shapes;
            synchronized (container)
            {
                RenderedShape<?> existing = container.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>()).get(key);
                if (existing != null)
                {   // promoting previous shape
                    existing.promoteWith(rshape);
                }
                else
                {
                    container.get(dim).put(key, rshape);
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
        synchronized (labels)
        {
            labels.values().forEach(Long2ObjectOpenHashMap::clear);
        }
    }

    public void renewShapes()
    {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        synchronized (shapes)
        {
            shapes.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));
        }
        synchronized (labels)
        {
            labels.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));
        }
        CarpetProfiler.end_current_section(token);
    }

    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        protected T shape;
        protected MinecraftClient client;
        long expiryTick;
        double renderEpsilon = 0;
        public abstract void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick );
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
        public boolean shouldRender(RegistryKey<World> dim)
        {
            if (shape.followEntity <=0 ) return true;
            if (client.world == null) return false;
            if (client.world.getRegistryKey() != dim) return false;
            return client.world.getEntityById(shape.followEntity) != null;
        }
        public boolean stageDeux()
        {
            return false;
        }

        public void promoteWith(RenderedShape<?> rshape)
        {
            expiryTick = rshape.expiryTick;
        }
    }

    public static class RenderedText extends RenderedShape<ShapeDispatcher.DisplayedText>
    {

        protected RenderedText(MinecraftClient client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.DisplayedText)shape);
        }

        @Override
        public void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d v1 = shape.relativiseRender(client.world, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getCamera();
            TextRenderer textRenderer = client.textRenderer;
            if (shape.doublesided)
                RenderSystem.disableCull();
            else
                RenderSystem.enableCull();
            matrices.push();
            matrices.translate(v1.x - cx,v1.y - cy,v1.z - cz);
            if (shape.facing == null)
            {
                //matrices.method_34425(new Matrix4f(camera1.getRotation()));
                matrices.multiply(camera1.getRotation());
            }
            else
            {
                switch (shape.facing)
                {
                    case NORTH:
                        break;
                    case SOUTH:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                        break;
                    case EAST:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270));
                        break;
                    case WEST:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
                        break;
                    case UP:
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90));
                        break;
                    case DOWN:
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90));
                        break;
                }
            }
            matrices.scale(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            //RenderSystem.scalef(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            if (shape.tilt!=0.0f) matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(shape.tilt));
            if (shape.lean!=0.0f) matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(shape.lean));
            if (shape.turn!=0.0f) matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(shape.turn));
            matrices.translate(-10*shape.indent, -10*shape.height-9,  (-10*renderEpsilon)-10*shape.raise);
            //if (visibleThroughWalls) RenderSystem.disableDepthTest();
            matrices.scale(-1, 1, 1);
            //RenderSystem.applyModelViewMatrix(); // passed matrix directly to textRenderer.draw, not AffineTransformation.identity().getMatrix(),

            float text_x = 0;
            if (shape.align == 0)
            {
                text_x = (float)(-textRenderer.getWidth(shape.value.getString())) / 2.0F;
            }
            else if (shape.align == 1)
            {
                text_x = (float)(-textRenderer.getWidth(shape.value.getString()));
            }
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(builder);
            textRenderer.draw(shape.value, text_x, 0.0F, shape.textcolor, false, matrices.peek().getPositionMatrix(), immediate, false, shape.textbck, 15728880);
            immediate.draw();
            matrices.pop();
            RenderSystem.enableCull();
        }

        @Override
        public boolean stageDeux()
        {
            return true;
        }

        @Override
        public void promoteWith(RenderedShape<?> rshape)
        {
            super.promoteWith(rshape);
            try
            {
                this.shape.value = ((ShapeDispatcher.DisplayedText) rshape.shape).value;
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
        public void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d v1 = shape.relativiseRender(client.world, shape.from, partialTick);
            Vec3d v2 = shape.relativiseRender(client.world, shape.to, partialTick);
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
            // consider using built-ins
            //DebugRenderer.drawBox(new Box(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z), 0.5f, 0.5f, 0.5f, 0.5f);//shape.r, shape.g, shape.b, shape.a);
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
        public void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            Vec3d v1 = shape.relativiseRender(client.world, shape.from, partialTick);
            Vec3d v2 = shape.relativiseRender(client.world, shape.to, partialTick);
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
        public void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
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
        public void renderLines(MatrixStack matrices, Tessellator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3d vc = shape.relativiseRender(client.world, shape.center, partialTick);
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
        builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
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
        builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
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
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

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
                builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);  // line loop to line strip
                for (int i = 0; i <= num_steps360+1; i++)
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
                    builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);

                    float z = r * MathHelper.sin(theta);

                    builder.vertex(cx - x, cy + 0, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + 0, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + h, cz - z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + h, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx - x, cy + 0, cz + z).color(red, grn, blu, alpha).next();
                    tessellator.draw();
                }
            }
            else
            {
                builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
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
                builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
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
                    builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
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
                builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
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
                builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
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
                    builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
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
                builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
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

            builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
                builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx, cy+h, cz).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, cy+h, z + cz).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);  // quad strip to quads
                float xp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx + xp, cy + 0, cz + zp).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + xp, cy + h, cz + zp).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + h, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + 0, cz + z).color(red, grn, blu, alpha).next();
                    xp = x;
                    zp = z;
                }
                tessellator.draw();
            }

        }
        else if (axis == Direction.Axis.X)
        {
            builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
                builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx+h, cy, cz).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx+h, cy + y, cz + z).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);  // quad strip to quads
                float yp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * MathHelper.cos(theta);
                    float z = r * MathHelper.sin(theta);
                    builder.vertex(cx + 0, cy + yp, cz + zp).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + h, cy + yp, cz + zp).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + h, cy + y, cz + z).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + 0, cy + y, cz + z).color(red, grn, blu, alpha).next();
                    yp = y;
                    zp = z;
                }
                tessellator.draw();
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
                builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                builder.vertex(cx, cy, cz+h).color(red, grn, blu, alpha).next();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(x + cx, cy+y, cz+h).color(red, grn, blu, alpha).next();
                }
                tessellator.draw();

                builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);  // quad strip to quads
                float xp = r;
                float yp = 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * MathHelper.cos(theta);
                    float y = r * MathHelper.sin(theta);
                    builder.vertex(cx + xp, cy + yp, cz + 0).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + xp, cy + yp, cz + h).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + y, cz + h).color(red, grn, blu, alpha).next();
                    builder.vertex(cx + x, cy + y, cz + 0).color(red, grn, blu, alpha).next();
                    xp = x;
                    yp = y;
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
        int num_steps360 = (int)(2*Math.PI / step)+1;
        for (int i = 0; i <= num_steps360; i++)
        {
            builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
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
            builder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR); // line loop to line strip
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
            builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);  // quad strip to quads
            float xb = 0;
            float zb = 0;
            float xbp = 0;
            float zbp = 0;
            float yp = r;
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = j * step;
                float x = r * MathHelper.sin(phi) * MathHelper.cos(theta);
                float z = r * MathHelper.sin(phi) * MathHelper.sin(theta);
                float y = r * MathHelper.cos(phi);
                float xp = r * MathHelper.sin(phi) * MathHelper.cos(thetaprime);
                float zp = r * MathHelper.sin(phi) * MathHelper.sin(thetaprime);
                builder.vertex(xb + cx, yp + cy, zb + cz).color(red, grn, blu, alpha).next();
                builder.vertex(xbp + cx, yp + cy, zbp + cz).color(red, grn, blu, alpha).next();
                builder.vertex(xp + cx, y + cy, zp + cz).color(red, grn, blu, alpha).next();
                builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).next();
                xb = x;
                zb = z;
                xbp = xp;
                zbp = zp;
                yp = y;
            }
            tessellator.draw();
        }
    }
}
