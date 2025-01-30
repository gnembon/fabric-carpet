package carpet.script.utils;

import carpet.script.CarpetScriptServer;
import carpet.script.external.Carpet;
import carpet.script.utils.shapes.ShapeDirection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class ShapesRenderer
{
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> labels;
    private final Minecraft client;

    private final Map<String, BiFunction<Minecraft, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> renderedShapes
            = new HashMap<>()
    {{
        put("line", RenderedLine::new);
        put("box", RenderedBox::new);
        put("sphere", RenderedSphere::new);
        put("cylinder", RenderedCylinder::new);
        put("label", RenderedText::new);
        put("polygon", RenderedPolyface::new);
        put("block", (c, s) -> new RenderedSprite(c, s, false));
        put("item", (c, s) -> new RenderedSprite(c, s, true));
    }};

    public static void rotatePoseStackByShapeDirection(PoseStack poseStack, ShapeDirection shapeDirection, Camera camera, Vec3 objectPos)
    {
        switch (shapeDirection)
        {
            case NORTH -> {}
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case CAMERA -> poseStack.mulPose(camera.rotation());
            case PLAYER -> {
                Vec3 vector = objectPos.subtract(camera.getPosition());
                double x = vector.x;
                double y = vector.y;
                double z = vector.z;
                double d = Math.sqrt(x * x + z * z);
                float rotX = (float) (Math.atan2(x, z));
                float rotY = (float) (Math.atan2(y, d));

                // that should work somehow but it doesn't for some reason
                //matrices.mulPose(new Quaternion( -rotY, rotX, 0, false));

                poseStack.mulPose(Axis.YP.rotation(rotX));
                poseStack.mulPose(Axis.XP.rotation(-rotY));
            }
        }
    }

    public ShapesRenderer(Minecraft minecraftClient)
    {
        this.client = minecraftClient;
        shapes = new HashMap<>();
        labels = new HashMap<>();
    }

    public void render(Matrix4f modelViewMatrix, Camera camera, float partialTick)
    {
        Runnable token = Carpet.startProfilerSection("Scarpet client");
        // posestack is not needed anymore - left as TODO to cleanup later
        PoseStack matrices = new PoseStack();

        //Camera camera = this.client.gameRenderer.getCamera();
        ClientLevel iWorld = this.client.level;
        ResourceKey<Level> dimensionType = iWorld.dimension();
        if ((shapes.get(dimensionType) == null || shapes.get(dimensionType).isEmpty()) &&
                (labels.get(dimensionType) == null || labels.get(dimensionType).isEmpty()))
        {
            return;
        }
        long currentTime = client.level.getGameTime();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
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

        Tesselator tesselator = Tesselator.getInstance();

        // render
        double cameraX = camera.getPosition().x;
        double cameraY = camera.getPosition().y;
        double cameraZ = camera.getPosition().z;
        boolean entityBoxes = client.getEntityRenderDispatcher().shouldRenderHitBoxes();

        if (!shapes.isEmpty())
        {
            shapes.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.pushMatrix();
            matrixStack.mul(matrices.last().pose());

            // lines
            RenderSystem.lineWidth(0.5F);
            shapes.get(dimensionType).values().forEach(s -> {
                if ((!s.shape.debug || entityBoxes) && s.shouldRender(dimensionType))
                {
                    s.renderLines(matrices, tesselator, cameraX, cameraY, cameraZ, partialTick);
                }
            });
            // faces
            RenderSystem.lineWidth(0.1F);
            shapes.get(dimensionType).values().forEach(s -> {
                if ((!s.shape.debug || entityBoxes) && s.shouldRender(dimensionType))
                {
                    s.renderFaces(tesselator, cameraX, cameraY, cameraZ, partialTick);
                }
            });
            RenderSystem.lineWidth(1.0F);
            matrixStack.popMatrix();

        }
        if (!labels.isEmpty())
        {
            labels.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            labels.get(dimensionType).values().forEach(s -> {
                if ((!s.shape.debug || entityBoxes) && s.shouldRender(dimensionType))
                {
                    s.renderLines(matrices, tesselator, cameraX, cameraY, cameraZ, partialTick);
                }
            });
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        token.run();
    }

    public void addShapes(ListTag tag)
    {
        Runnable token = Carpet.startProfilerSection("Scarpet client");
        for (int i = 0, count = tag.size(); i < count; i++)
        {
            addShape(tag.getCompound(i));
        }
        token.run();
    }

    public void addShape(CompoundTag tag)
    {
        ShapeDispatcher.ExpiringShape shape = ShapeDispatcher.fromTag(tag, client.level);
        if (shape == null)
        {
            return;
        }
        BiFunction<Minecraft, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape>> shapeFactory;
        shapeFactory = renderedShapes.get(tag.getString("shape"));
        if (shapeFactory == null)
        {
            CarpetScriptServer.LOG.info("Unrecognized shape: " + tag.getString("shape"));
        }
        else
        {
            RenderedShape<?> rshape = shapeFactory.apply(client, shape);
            ResourceKey<Level> dim = shape.shapeDimension;
            long key = rshape.key();
            Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> container =
                    rshape.stageDeux() ? labels : shapes;
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

    public void reset()
    {
        shapes.values().forEach(Long2ObjectOpenHashMap::clear);
        labels.values().forEach(Long2ObjectOpenHashMap::clear);
    }

    public void renewShapes()
    {
        Runnable token = Carpet.startProfilerSection("Scarpet client");
        shapes.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));
        labels.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));

        token.run();
    }

    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        protected T shape;
        protected Minecraft client;
        long expiryTick;
        double renderEpsilon;

        public abstract void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick);

        public void renderFaces(Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
        }

        protected RenderedShape(Minecraft client, T shape)
        {
            this.shape = shape;
            this.client = client;
            expiryTick = client.level.getGameTime() + shape.getExpiry();
            renderEpsilon = (3 + ((double) key()) / Long.MAX_VALUE) / 1000;
        }

        public boolean isExpired(long currentTick)
        {
            return expiryTick < currentTick;
        }

        public long key()
        {
            return shape.key(client.level.registryAccess());
        }

        public boolean shouldRender(ResourceKey<Level> dim)
        {
            if (shape.followEntity <= 0)
            {
                return true;
            }
            if (client.level == null)
            {
                return false;
            }
            if (client.level.dimension() != dim)
            {
                return false;
            }
            return client.level.getEntity(shape.followEntity) != null;
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

    public static class RenderedSprite extends RenderedShape<ShapeDispatcher.DisplayedSprite>
    {

        private final boolean isitem;
        private ItemDisplayContext transformType = ItemDisplayContext.NONE;

        private BlockPos blockPos;
        private BlockState blockState;
        private BlockEntity BlockEntity = null;

        protected RenderedSprite(Minecraft client, ShapeDispatcher.ExpiringShape shape, boolean isitem)
        {
            super(client, (ShapeDispatcher.DisplayedSprite) shape);
            this.isitem = isitem;
            if (isitem)
            {
                this.transformType = ItemDisplayContext.valueOf(((ShapeDispatcher.DisplayedSprite) shape).itemTransformType.toUpperCase(Locale.ROOT));
            }
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy,
                                double cz, float partialTick)
        {
            if (shape.a == 0.0)
            {
                return;
            }

            Vec3 v1 = shape.relativiseRender(client.level, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getMainCamera();

            matrices.pushPose();
            if (!isitem)// blocks should use its center as the origin
            {
                matrices.translate(0.5, 0.5, 0.5);
            }

            matrices.translate(v1.x - cx, v1.y - cy, v1.z - cz);
            rotatePoseStackByShapeDirection(matrices, shape.facing, camera1, isitem ? v1 : v1.add(0.5, 0.5, 0.5));
            if (shape.tilt != 0.0f)
            {
                matrices.mulPose(Axis.ZP.rotationDegrees(-shape.tilt));
            }
            if (shape.lean != 0.0f)
            {
                matrices.mulPose(Axis.XP.rotationDegrees(-shape.lean));
            }
            if (shape.turn != 0.0f)
            {
                matrices.mulPose(Axis.YP.rotationDegrees(shape.turn));
            }
            matrices.scale(shape.scaleX, shape.scaleY, shape.scaleZ);

            if (!isitem)
            {
                // blocks should use its center as the origin
                matrices.translate(-0.5, -0.5, -0.5);
            }
            else
            {
                // items seems to be flipped by default
                matrices.mulPose(Axis.YP.rotationDegrees(180));
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();

            blockPos = BlockPos.containing(v1);
            int light = 0;
            if (client.level != null)
            {
                light = LightTexture.pack(
                        shape.blockLight < 0 ? client.level.getBrightness(LightLayer.BLOCK, blockPos) : shape.blockLight,
                        shape.skyLight < 0 ? client.level.getBrightness(LightLayer.SKY, blockPos) : shape.skyLight
                );
            }

            blockState = shape.blockState;

            MultiBufferSource.BufferSource immediate = client.renderBuffers().bufferSource();
            if (!isitem)
            {
                // draw the block itself
                if (blockState.getRenderShape() == RenderShape.MODEL)
                {

                    var bakedModel = client.getBlockRenderer().getBlockModel(blockState);
                    int color = client.getBlockColors().getColor(blockState, client.level, blockPos, 0);
                    //dont know why there is a 0. 
                    //see https://github.com/senseiwells/EssentialClient/blob/4db1f291936f502304791ee323f369c206b3021d/src/main/java/me/senseiwells/essentialclient/utils/render/RenderHelper.java#L464
                    float red = (color >> 16 & 0xFF) / 255.0F;
                    float green = (color >> 8 & 0xFF) / 255.0F;
                    float blue = (color & 0xFF) / 255.0F;
                    RenderType type;
                    if (blockState.getBlock() instanceof LeavesBlock && !Minecraft.useFancyGraphics()) {
                        type = RenderType.solid();
                    } else {
                        type = ItemBlockRenderTypes.getRenderType(blockState);
                    }
                    client.getBlockRenderer().getModelRenderer().renderModel(matrices.last(), immediate.getBuffer(type), blockState, bakedModel, red, green, blue, light, OverlayTexture.NO_OVERLAY);
                }

                // draw the block`s entity part
                if (BlockEntity == null)
                {
                    if (blockState.getBlock() instanceof EntityBlock eb)
                    {
                        BlockEntity = eb.newBlockEntity(blockPos, blockState);
                        if (BlockEntity != null)
                        {
                            BlockEntity.setLevel(client.level);
                            if (shape.blockEntity != null)
                            {
                                BlockEntity.loadWithComponents(shape.blockEntity, client.level.registryAccess());
                            }
                        }
                    }
                }
                if (BlockEntity != null)
                {
                        BlockEntityRenderer<BlockEntity> blockEntityRenderer = client.getBlockEntityRenderDispatcher().getRenderer(BlockEntity);
                        if (blockEntityRenderer != null)
                        {
                            blockEntityRenderer.render(BlockEntity, partialTick,
                                    matrices, immediate, light, OverlayTexture.NO_OVERLAY, camera1.getPosition());

                        }
                }
            }
            else
            {
                if (shape.item != null)
                {
                    // draw the item
                    client.getItemRenderer().renderStatic(shape.item, transformType, light,
                            OverlayTexture.NO_OVERLAY, matrices, immediate, client.level, (int) shape.key(client.level.registryAccess()));
                }
            }
            matrices.popPose();
            immediate.endBatch();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

        }

        @Override
        public boolean stageDeux()
        {
            return true;
        }
    }


    public static class RenderedText extends RenderedShape<ShapeDispatcher.DisplayedText>
    {

        protected RenderedText(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.DisplayedText) shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0)
            {
                return;
            }
            Vec3 v1 = shape.relativiseRender(client.level, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getMainCamera();
            Font textRenderer = client.font;
            if (shape.doublesided)
            {
                RenderSystem.disableCull();
            }
            else
            {
                RenderSystem.enableCull();
            }
            matrices.pushPose();
            matrices.translate(v1.x - cx, v1.y - cy, v1.z - cz);

            rotatePoseStackByShapeDirection(matrices, shape.facing, camera1, v1);

            matrices.scale(shape.size * 0.0025f, -shape.size * 0.0025f, shape.size * 0.0025f);
            //RenderSystem.scalef(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            if (shape.tilt != 0.0f)
            {
                matrices.mulPose(Axis.ZP.rotationDegrees(shape.tilt));
            }
            if (shape.lean != 0.0f)
            {
                matrices.mulPose(Axis.XP.rotationDegrees(shape.lean));
            }
            if (shape.turn != 0.0f)
            {
                matrices.mulPose(Axis.YP.rotationDegrees(shape.turn));
            }
            matrices.translate(-10 * shape.indent, -10 * shape.height - 9, (-10 * renderEpsilon) - 10 * shape.raise);
            //if (visibleThroughWalls) RenderSystem.disableDepthTest();
            matrices.scale(-1, 1, 1);
            //RenderSystem.applyModelViewMatrix(); // passed matrix directly to textRenderer.draw, not AffineTransformation.identity().getMatrix(),

            float text_x = 0;
            if (shape.align == 0)
            {
                text_x = (float) (-textRenderer.width(shape.value.getString())) / 2.0F;
            }
            else if (shape.align == 1)
            {
                text_x = (float) (-textRenderer.width(shape.value.getString()));
            }
            MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE));
            textRenderer.drawInBatch(shape.value, text_x, 0.0F, shape.textcolor, false, matrices.last().pose(), immediate, Font.DisplayMode.NORMAL, shape.textbck, 15728880);
            immediate.endBatch();
            matrices.popPose();
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
                CarpetScriptServer.LOG.error("shape " + rshape.shape.getClass() + " cannot cast to a Label");
            }
        }
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {

        private RenderedBox(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Box) shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0)
            {
                return;
            }
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
            drawBoxWireGLLines(tesselator,
                    (float) (v1.x - cx - renderEpsilon), (float) (v1.y - cy - renderEpsilon), (float) (v1.z - cz - renderEpsilon),
                    (float) (v2.x - cx + renderEpsilon), (float) (v2.y - cy + renderEpsilon), (float) (v2.z - cz + renderEpsilon),
                    v1.x != v2.x, v1.y != v2.y, v1.z != v2.z,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }

        @Override
        public void renderFaces(Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0)
            {
                return;
            }
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
            // consider using built-ins
            //DebugRenderer.drawBox(new Box(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z), 0.5f, 0.5f, 0.5f, 0.5f);//shape.r, shape.g, shape.b, shape.a);
            drawBoxFaces(tesselator,
                    (float) (v1.x - cx - renderEpsilon), (float) (v1.y - cy - renderEpsilon), (float) (v1.z - cz - renderEpsilon),
                    (float) (v2.x - cx + renderEpsilon), (float) (v2.y - cy + renderEpsilon), (float) (v2.z - cz + renderEpsilon),
                    v1.x != v2.x, v1.y != v2.y, v1.z != v2.z,
                    shape.fr, shape.fg, shape.fb, shape.fa
            );
        }
    }

    public static class RenderedLine extends RenderedShape<ShapeDispatcher.Line>
    {
        public RenderedLine(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Line) shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
            drawLine(tesselator,
                    (float) (v1.x - cx - renderEpsilon), (float) (v1.y - cy - renderEpsilon), (float) (v1.z - cz - renderEpsilon),
                    (float) (v2.x - cx + renderEpsilon), (float) (v2.y - cy + renderEpsilon), (float) (v2.z - cz + renderEpsilon),
                    shape.r, shape.g, shape.b, shape.a
            );
        }
    }

    public static class RenderedPolyface extends RenderedShape<ShapeDispatcher.Polyface>
    {
        private static final VertexFormat.Mode[] faceIndices = new VertexFormat.Mode[]{
                Mode.LINES, Mode.LINE_STRIP, Mode.DEBUG_LINES, Mode.DEBUG_LINE_STRIP, Mode.TRIANGLES, Mode.TRIANGLE_STRIP, Mode.TRIANGLE_FAN, Mode.QUADS};

        public RenderedPolyface(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Polyface) shape);
        }

        @Override
        public void renderFaces(Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0)
            {
                return;
            }

            if (shape.doublesided)
            {
                RenderSystem.disableCull();
            }
            else
            {
                RenderSystem.enableCull();
            }

            BufferBuilder builder = tesselator.begin(faceIndices[shape.mode], DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < shape.vertexList.size(); i++)
            {
                Vec3 vec = shape.vertexList.get(i);
                if (shape.relative.get(i))
                {
                    vec = shape.relativiseRender(client.level, vec, partialTick);
                }
                builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.fr, shape.fg, shape.fb, shape.fa);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());

            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            //RenderSystem.enableDepthTest();


        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy,
                                double cz, float partialTick)
        {
            if (shape.a == 0)
            {
                return;
            }

            if (shape.mode == 6)
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                Vec3 vec0 = null;
                for (int i = 0; i < shape.vertexList.size(); i++)
                {
                    Vec3 vec = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vec = shape.relativiseRender(client.level, vec, partialTick);
                    }
                    if (i == 0)
                    {
                        vec0 = vec;
                    }
                    builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                }
                builder.addVertex((float) (vec0.x() - cx), (float) (vec0.y() - cy), (float) (vec0.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                BufferUploader.drawWithShader(builder.buildOrThrow());
                if (shape.inneredges)
                {
                    BufferBuilder builderr = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                    for (int i = 1; i < shape.vertexList.size() - 1; i++)
                    {
                        Vec3 vec = shape.vertexList.get(i);
                        if (shape.relative.get(i))
                        {
                            vec = shape.relativiseRender(client.level, vec, partialTick);
                        }

                        builderr.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                        builderr.addVertex((float) (vec0.x() - cx), (float) (vec0.y() - cy), (float) (vec0.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                    }
                    BufferUploader.drawWithShader(builderr.buildOrThrow());
                }
                return;
            }
            if (shape.mode == 5)
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                Vec3 vec = shape.vertexList.get(1);
                if (shape.relative.get(1))
                {
                    vec = shape.relativiseRender(client.level, vec, partialTick);
                }
                builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                int i;
                for (i = 0; i < shape.vertexList.size(); i += 2)
                {
                    vec = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vec = shape.relativiseRender(client.level, vec, partialTick);
                    }
                    builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                }
                i = shape.vertexList.size() - 1;
                for (i -= 1 - i % 2; i > 0; i -= 2)
                {
                    vec = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vec = shape.relativiseRender(client.level, vec, partialTick);
                    }
                    builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                }
                if (shape.inneredges)
                {
                    for (i = 2; i < shape.vertexList.size() - 1; i++)
                    {
                        vec = shape.vertexList.get(i);
                        if (shape.relative.get(i))
                        {
                            vec = shape.relativiseRender(client.level, vec, partialTick);
                        }
                        builder.addVertex((float) (vec.x() - cx), (float) (vec.y() - cy), (float) (vec.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                    }
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
                return;
            }
            if (shape.mode == 4)
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i < shape.vertexList.size(); i++)
                {
                    Vec3 vecA = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vecA = shape.relativiseRender(client.level, vecA, partialTick);
                    }
                    i++;
                    Vec3 vecB = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vecB = shape.relativiseRender(client.level, vecB, partialTick);
                    }
                    i++;
                    Vec3 vecC = shape.vertexList.get(i);
                    if (shape.relative.get(i))
                    {
                        vecC = shape.relativiseRender(client.level, vecC, partialTick);
                    }
                    builder.addVertex((float) (vecA.x() - cx), (float) (vecA.y() - cy), (float) (vecA.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                    builder.addVertex((float) (vecB.x() - cx), (float) (vecB.y() - cy), (float) (vecB.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);

                    builder.addVertex((float) (vecB.x() - cx), (float) (vecB.y() - cy), (float) (vecB.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                    builder.addVertex((float) (vecC.x() - cx), (float) (vecC.y() - cy), (float) (vecC.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);

                    builder.addVertex((float) (vecC.x() - cx), (float) (vecC.y() - cy), (float) (vecC.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                    builder.addVertex((float) (vecA.x() - cx), (float) (vecA.y() - cy), (float) (vecA.z() - cz)).setColor(shape.r, shape.g, shape.b, shape.a);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
        }
    }

    public static class RenderedSphere extends RenderedShape<ShapeDispatcher.Sphere>
    {
        public RenderedSphere(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Sphere) shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0)
            {
                return;
            }
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            drawSphereWireframe(tesselator,
                    (float) (vc.x - cx), (float) (vc.y - cy), (float) (vc.z - cz),
                    (float) (shape.radius + renderEpsilon), shape.subdivisions,
                    shape.r, shape.g, shape.b, shape.a);
        }

        @Override
        public void renderFaces(Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0)
            {
                return;
            }
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            drawSphereFaces(tesselator,
                    (float) (vc.x - cx), (float) (vc.y - cy), (float) (vc.z - cz),
                    (float) (shape.radius + renderEpsilon), shape.subdivisions,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    public static class RenderedCylinder extends RenderedShape<ShapeDispatcher.Cylinder>
    {
        public RenderedCylinder(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Cylinder) shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0)
            {
                return;
            }
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            double dir = Mth.sign(shape.height);
            drawCylinderWireframe(tesselator,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2 * dir * renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
                    shape.r, shape.g, shape.b, shape.a);

        }

        @Override
        public void renderFaces(Tesselator tesselator, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0)
            {
                return;
            }
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            double dir = Mth.sign(shape.height);
            drawCylinderFaces(tesselator,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2 * dir * renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    // some raw shit

    public static void drawLine(Tesselator tesselator, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha)
    {
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(x1, y1, z1).setColor(red1, grn1, blu1, alpha);
        builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void drawBoxWireGLLines(
            Tesselator tesselator,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2)
    {
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        if (xthick)
        {
            builder.addVertex(x1, y1, z1).setColor(red1, grn2, blu2, alpha);
            builder.addVertex(x2, y1, z1).setColor(red1, grn2, blu2, alpha);

            builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y2, z1).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x1, y1, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
        }
        if (ythick)
        {
            builder.addVertex(x1, y1, z1).setColor(red2, grn1, blu2, alpha);
            builder.addVertex(x1, y2, z1).setColor(red2, grn1, blu2, alpha);

            builder.addVertex(x2, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y1, z2).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
        }
        if (zthick)
        {
            builder.addVertex(x1, y1, z1).setColor(red2, grn2, blu1, alpha);
            builder.addVertex(x1, y1, z2).setColor(red2, grn2, blu1, alpha);

            builder.addVertex(x1, y2, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y1, z1).setColor(red1, grn1, blu1, alpha);

            builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void drawBoxFaces(
            Tesselator tesselator,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha)
    {
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (xthick && ythick)
        {
            builder.addVertex(x1, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y2, z1).setColor(red1, grn1, blu1, alpha);
            if (zthick)
            {
                builder.addVertex(x1, y1, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);
            }
        }


        if (zthick && ythick)
        {
            builder.addVertex(x1, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y2, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y1, z2).setColor(red1, grn1, blu1, alpha);

            if (xthick)
            {
                builder.addVertex(x2, y1, z1).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);
            }
        }

        // now at least drawing one
        if (zthick && xthick)
        {
            builder.addVertex(x1, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y1, z1).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x2, y1, z2).setColor(red1, grn1, blu1, alpha);
            builder.addVertex(x1, y1, z2).setColor(red1, grn1, blu1, alpha);


            if (ythick)
            {
                builder.addVertex(x1, y2, z1).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y2, z1).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x2, y2, z2).setColor(red1, grn1, blu1, alpha);
                builder.addVertex(x1, y2, z2).setColor(red1, grn1, blu1, alpha);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void drawCylinderWireframe(Tesselator tesselator,
                                             float cx, float cy, float cz,
                                             float r, float h, Direction.Axis axis, int subd, boolean isFlat,
                                             float red, float grn, float blu, float alpha)
    {
        float step = (float) Math.PI / (subd / 2);
        int num_steps180 = (int) (Math.PI / step) + 1;
        int num_steps360 = (int) (2 * Math.PI / step);
        int hsteps = 1;
        float hstep = 1.0f;
        if (!isFlat)
        {
            hsteps = (int) Math.ceil(Mth.abs(h) / (step * r)) + 1;
            hstep = h / (hsteps - 1);
        }// draw base

        if (axis == Direction.Axis.Y)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);  // line loop to line strip
                for (int i = 0; i <= num_steps360 + 1; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = hh;
                    float z = r * Mth.sin(theta);
                    builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float x = r * Mth.cos(theta);

                    float z = r * Mth.sin(theta);

                    builder.addVertex(cx - x, cy + 0, cz + z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + x, cy + 0, cz - z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + x, cy + h, cz - z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx - x, cy + h, cz + z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx - x, cy + 0, cz + z).setColor(red, grn, blu, alpha);
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }
            }
            else
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.addVertex(cx - x, cy, cz + z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + x, cy, cz - z).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

        }
        else if (axis == Direction.Axis.X)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float z = r * Mth.cos(theta);
                    float x = hh;
                    float y = r * Mth.sin(theta);
                    builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float y = r * Mth.cos(theta);

                    float z = r * Mth.sin(theta);

                    builder.addVertex(cx + 0, cy - y, cz + z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + 0, cy + y, cz - z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + h, cy + y, cz - z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + h, cy - y, cz + z).setColor(red, grn, blu, alpha);
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }
            }
            else
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.addVertex(cx, cy - y, cz + z).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx, cy + y, cz - z).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = hh;
                    float x = r * Mth.sin(theta);
                    builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float x = r * Mth.cos(theta);

                    float y = r * Mth.sin(theta);

                    builder.addVertex(cx + x, cy - y, cz + 0).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx - x, cy + y, cz + 0).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx - x, cy + y, cz + h).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx + x, cy - y, cz + h).setColor(red, grn, blu, alpha);
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }
            }
            else
            {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builder.addVertex(cx + x, cy - y, cz).setColor(red, grn, blu, alpha);
                    builder.addVertex(cx - x, cy + y, cz).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

        }
    }

    public static void drawCylinderFaces(Tesselator tesselator,
                                         float cx, float cy, float cz,
                                         float r, float h, Direction.Axis axis, int subd, boolean isFlat,
                                         float red, float grn, float blu, float alpha)
    {
        float step = (float) Math.PI / (subd / 2);
        //final int num_steps180 = (int) (Math.PI / step) + 1;
        int num_steps360 = (int) (2 * Math.PI / step) + 1;

        if (axis == Direction.Axis.Y)
        {

            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.addVertex(cx, cy, cz).setColor(red, grn, blu, alpha);
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.cos(theta);
                float z = r * Mth.sin(theta);
                builder.addVertex(x + cx, cy, z + cz).setColor(red, grn, blu, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
            if (!isFlat)
            {
                BufferBuilder builderr = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builderr.addVertex(cx, cy + h, cz).setColor(red, grn, blu, alpha);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builderr.addVertex(x + cx, cy + h, z + cz).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builderr.buildOrThrow());

                BufferBuilder builderrr = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float xp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builderrr.addVertex(cx + xp, cy + 0, cz + zp).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + xp, cy + h, cz + zp).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + x, cy + h, cz + z).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + x, cy + 0, cz + z).setColor(red, grn, blu, alpha);
                    xp = x;
                    zp = z;
                }
                BufferUploader.drawWithShader(builderrr.buildOrThrow());
            }

        }
        else if (axis == Direction.Axis.X)
        {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.addVertex(cx, cy, cz).setColor(red, grn, blu, alpha);
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float y = r * Mth.cos(theta);
                float z = r * Mth.sin(theta);
                builder.addVertex(cx, cy + y, z + cz).setColor(red, grn, blu, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
            if (!isFlat)
            {
                BufferBuilder builderr = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builderr.addVertex(cx + h, cy, cz).setColor(red, grn, blu, alpha);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builderr.addVertex(cx + h, cy + y, cz + z).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builderr.buildOrThrow());

                BufferBuilder builderrr = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float yp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builderrr.addVertex(cx + 0, cy + yp, cz + zp).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + h, cy + yp, cz + zp).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + h, cy + y, cz + z).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + 0, cy + y, cz + z).setColor(red, grn, blu, alpha);
                    yp = y;
                    zp = z;
                }
                BufferUploader.drawWithShader(builderrr.buildOrThrow());
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.addVertex(cx, cy, cz).setColor(red, grn, blu, alpha);
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.cos(theta);
                float y = r * Mth.sin(theta);
                builder.addVertex(x + cx, cy + y, cz).setColor(red, grn, blu, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
            if (!isFlat)
            {
                BufferBuilder builderr = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builderr.addVertex(cx, cy, cz + h).setColor(red, grn, blu, alpha);
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builderr.addVertex(x + cx, cy + y, cz + h).setColor(red, grn, blu, alpha);
                }
                BufferUploader.drawWithShader(builderr.buildOrThrow());

                BufferBuilder builderrr = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float xp = r;
                float yp = 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builderrr.addVertex(cx + xp, cy + yp, cz + 0).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + xp, cy + yp, cz + h).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + x, cy + y, cz + h).setColor(red, grn, blu, alpha);
                    builderrr.addVertex(cx + x, cy + y, cz + 0).setColor(red, grn, blu, alpha);
                    xp = x;
                    yp = y;
                }
                BufferUploader.drawWithShader(builderrr.buildOrThrow());
            }
        }
    }

    public static void drawSphereWireframe(Tesselator tesselator,
                                           float cx, float cy, float cz,
                                           float r, int subd,
                                           float red, float grn, float blu, float alpha)
    {
        float step = (float) Math.PI / (subd / 2);
        int num_steps180 = (int) (Math.PI / step) + 1;
        int num_steps360 = (int) (2 * Math.PI / step) + 1;
        for (int i = 0; i <= num_steps360; i++)
        {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            float theta = step * i;
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = step * j;
                float x = r * Mth.sin(phi) * Mth.cos(theta);
                float z = r * Mth.sin(phi) * Mth.sin(theta);
                float y = r * Mth.cos(phi);
                builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
        for (int j = 0; j <= num_steps180; j++)
        {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
            float phi = step * j;

            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.sin(phi) * Mth.cos(theta);
                float z = r * Mth.sin(phi) * Mth.sin(theta);
                float y = r * Mth.cos(phi);
                builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }

    }

    public static void drawSphereFaces(Tesselator tesselator,
                                       float cx, float cy, float cz,
                                       float r, int subd,
                                       float red, float grn, float blu, float alpha)
    {

        float step = (float) Math.PI / (subd / 2);
        int num_steps180 = (int) (Math.PI / step) + 1;
        int num_steps360 = (int) (2 * Math.PI / step);
        for (int i = 0; i <= num_steps360; i++)
        {
            float theta = i * step;
            float thetaprime = theta + step;
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
            float xb = 0;
            float zb = 0;
            float xbp = 0;
            float zbp = 0;
            float yp = r;
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = j * step;
                float x = r * Mth.sin(phi) * Mth.cos(theta);
                float z = r * Mth.sin(phi) * Mth.sin(theta);
                float y = r * Mth.cos(phi);
                float xp = r * Mth.sin(phi) * Mth.cos(thetaprime);
                float zp = r * Mth.sin(phi) * Mth.sin(thetaprime);
                builder.addVertex(xb + cx, yp + cy, zb + cz).setColor(red, grn, blu, alpha);
                builder.addVertex(xbp + cx, yp + cy, zbp + cz).setColor(red, grn, blu, alpha);
                builder.addVertex(xp + cx, y + cy, zp + cz).setColor(red, grn, blu, alpha);
                builder.addVertex(x + cx, y + cy, z + cz).setColor(red, grn, blu, alpha);
                xb = x;
                zb = z;
                xbp = xp;
                zbp = zp;
                yp = y;
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
    }
}
