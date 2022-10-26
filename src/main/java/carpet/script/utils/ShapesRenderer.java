package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.mixins.ShulkerBoxAccessMixin;
import carpet.script.utils.shapes.ShapeDirection;
import carpet.utils.CarpetProfiler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ShapesRenderer
{
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> shapes;
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> labels;
    private Minecraft client;

    private Map<String, BiFunction<Minecraft, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >>> renderedShapes
            = new HashMap<>() {{
        put("line", RenderedLine::new);
        put("box", RenderedBox::new);
        put("sphere", RenderedSphere::new);
        put("cylinder", RenderedCylinder::new);
        put("label", RenderedText::new);
        put("polygon", RenderedPolyface::new);
        put("block", (c, s) -> new RenderedSprite(c, s, false));
        put("item", (c, s) -> new RenderedSprite(c, s, true));
    }};

    public ShapesRenderer(Minecraft minecraftClient)
    {
        this.client = minecraftClient;
        shapes = new HashMap<>();
        labels = new HashMap<>();
    }

    public void render(PoseStack matrices, Camera camera, float partialTick)
    {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        //Camera camera = this.client.gameRenderer.getCamera();
        ClientLevel iWorld = this.client.level;
        ResourceKey<Level> dimensionType = iWorld.dimension();
        if ((shapes.get(dimensionType) == null || shapes.get(dimensionType).isEmpty()) &&
                (labels.get(dimensionType) == null || labels.get(dimensionType).isEmpty())) return;
        long currentTime = client.level.getGameTime();
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

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();

        // render
        double cameraX = camera.getPosition().x;
        double cameraY = camera.getPosition().y;
        double cameraZ = camera.getPosition().z;
        boolean entityBoxes = client.getEntityRenderDispatcher().shouldRenderHitBoxes();

        if (shapes.size() != 0) {
            shapes.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            PoseStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.pushPose();
            matrixStack.mulPoseMatrix(matrices.last().pose());
            RenderSystem.applyModelViewMatrix();

            // lines
            RenderSystem.lineWidth(0.5F);
            shapes.get(dimensionType).values().forEach( s -> {
                if ( (!s.shape.debug ||entityBoxes ) && s.shouldRender(dimensionType)) s.renderLines(matrices, tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
            // faces
            RenderSystem.lineWidth(0.1F);
            shapes.get(dimensionType).values().forEach(s -> {
                        if ( (!s.shape.debug ||entityBoxes ) && s.shouldRender(dimensionType)) s.renderFaces(tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
            RenderSystem.lineWidth(1.0F);
            matrixStack.popPose();
            RenderSystem.applyModelViewMatrix();

        }
        if (labels.size() != 0) {
            labels.get(dimensionType).long2ObjectEntrySet().removeIf(
                    entry -> entry.getValue().isExpired(currentTime)
            );
            labels.get(dimensionType).values().forEach(s -> {
                if ( (!s.shape.debug || entityBoxes) && s.shouldRender(dimensionType)) s.renderLines(matrices, tessellator, bufferBuilder, cameraX, cameraY, cameraZ, partialTick);
            });
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        CarpetProfiler.end_current_section(token);
    }

    public void addShapes(ListTag tag)
    {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        for (int i=0, count = tag.size(); i < count; i++)
        {
            addShape(tag.getCompound(i));
        }
        CarpetProfiler.end_current_section(token);
    }

    public void addShape(CompoundTag tag)
    {
        ShapeDispatcher.ExpiringShape shape = ShapeDispatcher.fromTag(tag, client.level);
        if (shape == null) return;
        BiFunction<Minecraft, ShapeDispatcher.ExpiringShape, RenderedShape<? extends ShapeDispatcher.ExpiringShape >> shapeFactory;
        shapeFactory = renderedShapes.get(tag.getString("shape"));
        if (shapeFactory == null)
        {
            CarpetSettings.LOG.info("Unrecognized shape: "+tag.getString("shape"));
        }
        else
        {
            RenderedShape<?> rshape = shapeFactory.apply(client, shape);
            ResourceKey<Level> dim = shape.shapeDimension;
            long key = rshape.key();
            Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RenderedShape<? extends ShapeDispatcher.ExpiringShape>>> container =
                    rshape.stageDeux()?labels:shapes;
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
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Scarpet client", CarpetProfiler.TYPE.GENERAL);
        shapes.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));
        labels.values().forEach(el -> el.values().forEach(shape -> shape.expiryTick++));

        CarpetProfiler.end_current_section(token);
    }

    public abstract static class RenderedShape<T extends ShapeDispatcher.ExpiringShape>
    {
        protected T shape;
        protected Minecraft client;
        long expiryTick;
        double renderEpsilon = 0;
        public abstract void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick );
        public void renderFaces(Tesselator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick ) {}
        protected RenderedShape(Minecraft client, T shape)
        {
            this.shape = shape;
            expiryTick = client.level.getGameTime()+shape.getExpiry();
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
        public boolean shouldRender(ResourceKey<Level> dim)
        {
            if (shape.followEntity <=0 ) return true;
            if (client.level == null) return false;
            if (client.level.dimension() != dim) return false;
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

    public static class RenderedSprite extends RenderedShape<ShapeDispatcher.DisplayedSprite> {

        private final boolean isitem;
        private ItemTransforms.TransformType transformType = ItemTransforms.TransformType.NONE;

        private BlockPos blockPos;
        private BlockState blockState;
        private BlockEntity BlockEntity = null;

        protected RenderedSprite(Minecraft client, ShapeDispatcher.ExpiringShape shape, boolean isitem) {
            super(client, (ShapeDispatcher.DisplayedSprite) shape);
            this.isitem = isitem;
            if (isitem) {
                this.transformType = ItemTransforms.TransformType.valueOf(((ShapeDispatcher.DisplayedSprite) shape).itemTransformType.toUpperCase(Locale.ROOT));
            }
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder builder, double cx, double cy,
                double cz, float partialTick)
        {
            if (shape.a == 0.0)
                return;

            Vec3 v1 = shape.relativiseRender(client.level, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getMainCamera();

            matrices.pushPose();
            if (!isitem)// blocks should use its center as the origin
            {
                matrices.translate(0.5, 0.5, 0.5);
            }

            matrices.translate(v1.x - cx, v1.y - cy, v1.z - cz);
            ShapeDirection.rotatePoseStackByShapeDirection(matrices, shape.facing, camera1, isitem ? v1 : v1.add(0.5, 0.5, 0.5));
            if (shape.tilt != 0.0f) matrices.mulPose(Axis.ZP.rotationDegrees(-shape.tilt));
            if (shape.lean != 0.0f) matrices.mulPose(Axis.XP.rotationDegrees(-shape.lean));
            if (shape.turn != 0.0f) matrices.mulPose(Axis.YP.rotationDegrees( shape.turn));
            matrices.scale(shape.scaleX, shape.scaleY, shape.scaleZ);

            if (!isitem)
            {
                // blocks should use its center as the origin
                matrices.translate(-0.5, -0.5, -0.5);
            }
            else {
                // items seems to be flipped by default
                matrices.mulPose(Axis.YP.rotationDegrees(180));
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();

            blockPos = new BlockPos(v1);
            int light = 0;
            if (client.level != null) {
                light = LightTexture.pack(
                        shape.blockLight < 0 ? client.level.getBrightness(LightLayer.BLOCK, blockPos) : shape.blockLight,
                        shape.skyLight < 0 ? client.level.getBrightness(LightLayer.SKY, blockPos) : shape.skyLight
                );
            }

            blockState = shape.blockState;

            MultiBufferSource.BufferSource immediate = client.renderBuffers().bufferSource();
            if (!isitem) {
                // draw the block itself
                if (blockState.getRenderShape() == RenderShape.MODEL) {

                    var bakedModel = client.getBlockRenderer().getBlockModel(blockState);
                    int color = client.getBlockColors().getColor(blockState,client.level,blockPos,0);
                    //dont know why there is a 0. 
                    //see https://github.com/senseiwells/EssentialClient/blob/4db1f291936f502304791ee323f369c206b3021d/src/main/java/me/senseiwells/essentialclient/utils/render/RenderHelper.java#L464
                    float red = (color >> 16 & 0xFF) / 255.0F;
			    	float green = (color >> 8 & 0xFF) / 255.0F;
			    	float blue = (color & 0xFF) / 255.0F;
                    client.getBlockRenderer().getModelRenderer().renderModel(matrices.last(), immediate.getBuffer(ItemBlockRenderTypes.getRenderType(blockState, false)), blockState, bakedModel, red, green, blue, light, OverlayTexture.NO_OVERLAY);
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
                            if (shape.blockEntity != null) BlockEntity.load(shape.blockEntity);
                        }
                    }
                }
                if (BlockEntity instanceof ShulkerBoxBlockEntity sbBlockEntity)
                {
                    sbrender(sbBlockEntity, partialTick,
                    matrices, immediate, light, OverlayTexture.NO_OVERLAY);
                }
                else
                {
                    if (BlockEntity != null)
                    {
                        final BlockEntityRenderer<BlockEntity> blockEntityRenderer = client.getBlockEntityRenderDispatcher().getRenderer(BlockEntity);
                        if (blockEntityRenderer != null) {
                            blockEntityRenderer.render(BlockEntity, partialTick,
                                    matrices, immediate, light, OverlayTexture.NO_OVERLAY);

                        }
                    }
                }
            }
            else
            {
                if (shape.item != null)
                {
                    // draw the item
                    client.getItemRenderer().renderStatic(shape.item, transformType, light,
                            OverlayTexture.NO_OVERLAY, matrices, immediate, (int) shape.key());
                }
            }
            matrices.popPose();
            immediate.endBatch();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

        }

        @Override
        public boolean stageDeux() {
            return true;
        }

        // copy and modifiy a bit from net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer.render
        public void sbrender(ShulkerBoxBlockEntity shulkerBoxBlockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j)
        {
            Direction direction = Direction.UP;
            if (shulkerBoxBlockEntity.hasLevel())
            {
                BlockState blockState = shulkerBoxBlockEntity.getBlockState();
                if (blockState.getBlock() instanceof ShulkerBoxBlock)
                {
                    direction = blockState.getValue(ShulkerBoxBlock.FACING);
                }
            }
            DyeColor dyeColor = shulkerBoxBlockEntity.getColor();
            Material material;
            if (dyeColor == null)
            {
                material = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
            }
            else
            {
                material = Sheets.SHULKER_TEXTURE_LOCATION.get(dyeColor.getId());
            }
    
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.9995F, 0.9995F, 0.9995F);
            poseStack.mulPose(direction.getRotation());
            poseStack.scale(1.0F, -1.0F, -1.0F);
            poseStack.translate(0.0, -1.0, 0.0);
            ShulkerModel<?> model = ((ShulkerBoxAccessMixin) client.getBlockEntityRenderDispatcher().getRenderer(BlockEntity)).getModel();
            ModelPart modelPart = model.getLid();
            modelPart.setPos(0.0F, 24.0F - shulkerBoxBlockEntity.getProgress(f) * 0.5F * 16.0F, 0.0F);
            modelPart.yRot = 270.0F * shulkerBoxBlockEntity.getProgress(f) * (float) (Math.PI / 180.0);
            VertexConsumer vertexConsumer = material.buffer(multiBufferSource, RenderType::entityCutoutNoCull);
            model.renderToBuffer(poseStack, vertexConsumer, i, j, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }


    public static class RenderedText extends RenderedShape<ShapeDispatcher.DisplayedText>
    {

        protected RenderedText(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.DisplayedText)shape);
        }

        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder builder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3 v1 = shape.relativiseRender(client.level, shape.pos, partialTick);
            Camera camera1 = client.gameRenderer.getMainCamera();
            Font textRenderer = client.font;
            if (shape.doublesided)
                RenderSystem.disableCull();
            else
                RenderSystem.enableCull();
            matrices.pushPose();
            matrices.translate(v1.x - cx,v1.y - cy,v1.z - cz);

            ShapeDirection.rotatePoseStackByShapeDirection(matrices, shape.facing, camera1, v1);

            matrices.scale(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            //RenderSystem.scalef(shape.size* 0.0025f, -shape.size*0.0025f, shape.size*0.0025f);
            if (shape.tilt!=0.0f) matrices.mulPose(Axis.ZP.rotationDegrees(shape.tilt));
            if (shape.lean!=0.0f) matrices.mulPose(Axis.XP.rotationDegrees(shape.lean));
            if (shape.turn!=0.0f) matrices.mulPose(Axis.YP.rotationDegrees(shape.turn));
            matrices.translate(-10*shape.indent, -10*shape.height-9,  (-10*renderEpsilon)-10*shape.raise);
            //if (visibleThroughWalls) RenderSystem.disableDepthTest();
            matrices.scale(-1, 1, 1);
            //RenderSystem.applyModelViewMatrix(); // passed matrix directly to textRenderer.draw, not AffineTransformation.identity().getMatrix(),

            float text_x = 0;
            if (shape.align == 0)
            {
                text_x = (float)(-textRenderer.width(shape.value.getString())) / 2.0F;
            }
            else if (shape.align == 1)
            {
                text_x = (float)(-textRenderer.width(shape.value.getString()));
            }
            MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(builder);
            textRenderer.drawInBatch(shape.value, text_x, 0.0F, shape.textcolor, false, matrices.last().pose(), immediate, false, shape.textbck, 15728880);
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
                CarpetSettings.LOG.error("shape "+rshape.shape.getClass()+" cannot cast to a Label");
            }
        }
    }

    public static class RenderedBox extends RenderedShape<ShapeDispatcher.Box>
    {

        private RenderedBox(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Box)shape);

        }
        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
            drawBoxWireGLLines(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
                    v1.x!=v2.x, v1.y!=v2.y, v1.z!=v2.z,
                    shape.r, shape.g, shape.b, shape.a, shape.r, shape.g, shape.b
            );
        }
        @Override
        public void renderFaces(Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
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
        public RenderedLine(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Line)shape);
        }
        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            Vec3 v1 = shape.relativiseRender(client.level, shape.from, partialTick);
            Vec3 v2 = shape.relativiseRender(client.level, shape.to, partialTick);
            drawLine(tessellator, bufferBuilder,
                    (float)(v1.x-cx-renderEpsilon), (float)(v1.y-cy-renderEpsilon), (float)(v1.z-cz-renderEpsilon),
                    (float)(v2.x-cx+renderEpsilon), (float)(v2.y-cy+renderEpsilon), (float)(v2.z-cz+renderEpsilon),
                    shape.r, shape.g, shape.b, shape.a
            );
        }
    }
    public static class RenderedPolyface extends RenderedShape<ShapeDispatcher.Polyface>
    {
        public RenderedPolyface(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Polyface)shape);
        }
        @Override
        public void renderFaces(Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {       
            if(shape.fa==0){return;}

            if (shape.doublesided)
                RenderSystem.disableCull();
            else
                RenderSystem.enableCull();
            
            bufferBuilder.begin(shape.mode, DefaultVertexFormat.POSITION_COLOR);
                for(int i=0;i<shape.vertex_list.size();i++){
                    Vec3 vec=shape.vertex_list.get(i);
                    if(shape.relative.get(i)){
                        vec=shape.relativiseRender(client.level, vec, partialTick);
                    }
                    bufferBuilder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.fr, shape.fg, shape.fb, shape.fa).endVertex();
                }
                tessellator.end();

            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            //RenderSystem.enableDepthTest();
        
            
                
        }
        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder builder, double cx, double cy,
                double cz, float partialTick) {
                    if(shape.a==0){return;}

                    if (shape.mode==Mode.TRIANGLE_FAN){
                        builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                        Vec3 vec0=null;
                        for(int i=0;i<shape.vertex_list.size();i++){
                            Vec3 vec=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vec=shape.relativiseRender(client.level, vec, partialTick);
                            }
                            if(i==0)vec0=vec;
                            builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        }
                        builder.vertex(vec0.x()-cx, vec0.y()-cy, vec0.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        tessellator.end();
                        if(shape.inneredges){
                            builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                            for(int i=1;i<shape.vertex_list.size()-1;i++){
                                Vec3 vec = shape.vertex_list.get(i);
                                if(shape.relative.get(i)){
                                    vec=shape.relativiseRender(client.level, vec, partialTick);
                                }
                                
                                builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                                builder.vertex(vec0.x()-cx, vec0.y()-cy, vec0.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                            }
                            tessellator.end();
                        }
                        return;
                    }
                    if (shape.mode==Mode.TRIANGLE_STRIP){
                        builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                        Vec3 vec=shape.vertex_list.get(1);
                        if(shape.relative.get(1)){
                            vec=shape.relativiseRender(client.level, vec, partialTick);
                        }
                        builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        int i;
                        for(i=0;i<shape.vertex_list.size();i+=2){
                            vec=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vec=shape.relativiseRender(client.level, vec, partialTick);
                            }
                            builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        }
                        i=shape.vertex_list.size()-1;
                        for(i-=1-i%2;i>0;i-=2){
                            vec=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vec=shape.relativiseRender(client.level, vec, partialTick);
                            }
                            builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        }
                        if(shape.inneredges){
                            for(i=2;i<shape.vertex_list.size()-1;i++){
                                vec=shape.vertex_list.get(i);
                                if(shape.relative.get(i)){
                                    vec=shape.relativiseRender(client.level, vec, partialTick);
                                }
                                builder.vertex(vec.x()-cx, vec.y()-cy, vec.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                            }
                        }
                        tessellator.end();
                        return;
                    }
                    if (shape.mode==Mode.TRIANGLES){
                        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                        for (int i=0;i<shape.vertex_list.size();i++){
                            Vec3 vecA=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vecA=shape.relativiseRender(client.level, vecA, partialTick);
                            }
                            i++;
                            Vec3 vecB=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vecB=shape.relativiseRender(client.level, vecB, partialTick);
                            }
                            i++;
                            Vec3 vecC=shape.vertex_list.get(i);
                            if(shape.relative.get(i)){
                                vecC=shape.relativiseRender(client.level, vecC, partialTick);
                            }
                            builder.vertex(vecA.x()-cx, vecA.y()-cy, vecA.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                            builder.vertex(vecB.x()-cx, vecB.y()-cy, vecB.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();

                            builder.vertex(vecB.x()-cx, vecB.y()-cy, vecB.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                            builder.vertex(vecC.x()-cx, vecC.y()-cy, vecC.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();

                            builder.vertex(vecC.x()-cx, vecC.y()-cy, vecC.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                            builder.vertex(vecA.x()-cx, vecA.y()-cy, vecA.z()-cz).color(shape.r, shape.g, shape.b, shape.a).endVertex();
                        }
                        tessellator.end();
                        return;
                    }
                }
    }
    public static class RenderedSphere extends RenderedShape<ShapeDispatcher.Sphere>
    {
        public RenderedSphere(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Sphere)shape);
        }
        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            drawSphereWireframe(tessellator, bufferBuilder,
                    (float)(vc.x-cx), (float)(vc.y-cy), (float)(vc.z-cz),
                    (float)(shape.radius+renderEpsilon), shape.subdivisions,
                    shape.r, shape.g, shape.b, shape.a);
        }
        @Override
        public void renderFaces(Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            drawSphereFaces(tessellator, bufferBuilder,
                    (float)(vc.x-cx), (float)(vc.y-cy), (float)(vc.z-cz),
                    (float)(shape.radius+renderEpsilon), shape.subdivisions,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    public static class RenderedCylinder extends RenderedShape<ShapeDispatcher.Cylinder>
    {
        public RenderedCylinder(Minecraft client, ShapeDispatcher.ExpiringShape shape)
        {
            super(client, (ShapeDispatcher.Cylinder)shape);
        }
        @Override
        public void renderLines(PoseStack matrices, Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.a == 0.0) return;
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            double dir = Mth.sign(shape.height);
            drawCylinderWireframe(tessellator, bufferBuilder,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2*dir*renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
                    shape.r, shape.g, shape.b, shape.a);

        }
        @Override
        public void renderFaces(Tesselator tessellator, BufferBuilder bufferBuilder, double cx, double cy, double cz, float partialTick)
        {
            if (shape.fa == 0.0) return;
            Vec3 vc = shape.relativiseRender(client.level, shape.center, partialTick);
            double dir = Mth.sign(shape.height);
            drawCylinderFaces(tessellator, bufferBuilder,
                    (float) (vc.x - cx - dir * renderEpsilon), (float) (vc.y - cy - dir * renderEpsilon), (float) (vc.z - cz - dir * renderEpsilon),
                    (float) (shape.radius + renderEpsilon), (float) (shape.height + 2*dir*renderEpsilon), shape.axis,
                    shape.subdivisions, shape.radius == 0,
                    shape.fr, shape.fg, shape.fb, shape.fa);
        }
    }

    // some raw shit

    public static void drawLine(Tesselator tessellator, BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float red1, float grn1, float blu1, float alpha) {
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
        builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
        tessellator.end();
    }

    public static void drawBoxWireGLLines(
            Tesselator tessellator, BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha, float red2, float grn2, float blu2)
    {
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        if (xthick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn2, blu2, alpha).endVertex();
            builder.vertex(x2, y1, z1).color(red1, grn2, blu2, alpha).endVertex();

            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
        }
        if (ythick)
        {
            builder.vertex(x1, y1, z1).color(red2, grn1, blu2, alpha).endVertex();
            builder.vertex(x1, y2, z1).color(red2, grn1, blu2, alpha).endVertex();

            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
        }
        if (zthick)
        {
            builder.vertex(x1, y1, z1).color(red2, grn2, blu1, alpha).endVertex();
            builder.vertex(x1, y1, z2).color(red2, grn2, blu1, alpha).endVertex();

            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).endVertex();

            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
        }
        tessellator.end();
    }

    public static void drawBoxFaces(
            Tesselator tessellator, BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            boolean xthick, boolean ythick, boolean zthick,
            float red1, float grn1, float blu1, float alpha)
    {
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (xthick && ythick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            if (zthick)
            {
                builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
            }
        }


        if (zthick && ythick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).endVertex();

            if (xthick)
            {
                builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
            }
        }

        // now at least drawing one
        if (zthick && xthick)
        {
            builder.vertex(x1, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y1, z1).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x2, y1, z2).color(red1, grn1, blu1, alpha).endVertex();
            builder.vertex(x1, y1, z2).color(red1, grn1, blu1, alpha).endVertex();


            if (ythick)
            {
                builder.vertex(x1, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y2, z1).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x2, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
                builder.vertex(x1, y2, z2).color(red1, grn1, blu1, alpha).endVertex();
            }
        }
        tessellator.end();
    }

    public static void drawCylinderWireframe(Tesselator tessellator, BufferBuilder builder,
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
            hsteps = (int) Math.ceil(Mth.abs(h) / (step * r)) + 1;
            hstep = h / (hsteps - 1);
        }// draw base

        if (axis == Direction.Axis.Y)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh*hstep;
                builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);  // line loop to line strip
                for (int i = 0; i <= num_steps360+1; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = hh;
                    float z = r * Mth.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float x = r * Mth.cos(theta);

                    float z = r * Mth.sin(theta);

                    builder.vertex(cx - x, cy + 0, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + 0, cz - z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + h, cz - z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx - x, cy + h, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx - x, cy + 0, cz + z).color(red, grn, blu, alpha).endVertex();
                    tessellator.end();
                }
            }
            else
            {
                builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(cx - x, cy, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy, cz - z).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }

        }
        else if (axis == Direction.Axis.X)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float z = r * Mth.cos(theta);
                    float x = hh;
                    float y = r * Mth.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }

            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float y = r * Mth.cos(theta);

                    float z = r * Mth.sin(theta);

                    builder.vertex(cx + 0, cy - y, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + 0, cy + y, cz - z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + h, cy + y, cz - z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + h, cy - y, cz + z).color(red, grn, blu, alpha).endVertex();
                    tessellator.end();
                }
            }
            else
            {
                builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(cx, cy - y, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx, cy + y, cz - z).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            for (int dh = 0; dh < hsteps; dh++)
            {
                float hh = dh * hstep;
                builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = hh;
                    float x = r * Mth.sin(theta);
                    builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }
            if (!isFlat)
            {
                for (int i = 0; i <= num_steps180; i++)
                {
                    builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
                    float theta = step * i;
                    float x = r * Mth.cos(theta);

                    float y = r * Mth.sin(theta);

                    builder.vertex(cx + x, cy - y, cz + 0).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx - x, cy + y, cz + 0).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx - x, cy + y, cz + h).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy - y, cz + h).color(red, grn, blu, alpha).endVertex();
                    tessellator.end();
                }
            }
            else
            {
                builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= num_steps180; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builder.vertex(cx + x, cy - y, cz).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx - x, cy + y, cz).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();
            }

        }
    }

    public static void drawCylinderFaces(Tesselator tessellator, BufferBuilder builder,
                                             float cx, float cy, float cz,
                                             float r, float h, Direction.Axis axis,  int subd, boolean isFlat,
                                             float red, float grn, float blu, float alpha)
    {
        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step)+1;

        if (axis == Direction.Axis.Y)
        {

            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).endVertex();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.cos(theta);
                float z = r * Mth.sin(theta);
                builder.vertex(x + cx, cy, z + cz).color(red, grn, blu, alpha).endVertex();
            }
            tessellator.end();
            if (!isFlat)
            {
                builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(cx, cy+h, cz).color(red, grn, blu, alpha).endVertex();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(x + cx, cy+h, z + cz).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();

                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float xp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(cx + xp, cy + 0, cz + zp).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + xp, cy + h, cz + zp).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + h, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + 0, cz + z).color(red, grn, blu, alpha).endVertex();
                    xp = x;
                    zp = z;
                }
                tessellator.end();
            }

        }
        else if (axis == Direction.Axis.X)
        {
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).endVertex();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float y = r * Mth.cos(theta);
                float z = r * Mth.sin(theta);
                builder.vertex(cx, cy + y, z + cz).color(red, grn, blu, alpha).endVertex();
            }
            tessellator.end();
            if (!isFlat)
            {
                builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(cx+h, cy, cz).color(red, grn, blu, alpha).endVertex();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(cx+h, cy + y, cz + z).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();

                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float yp = r * 1;
                float zp = r * 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float y = r * Mth.cos(theta);
                    float z = r * Mth.sin(theta);
                    builder.vertex(cx + 0, cy + yp, cz + zp).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + h, cy + yp, cz + zp).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + h, cy + y, cz + z).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + 0, cy + y, cz + z).color(red, grn, blu, alpha).endVertex();
                    yp = y;
                    zp = z;
                }
                tessellator.end();
            }
        }
        else if (axis == Direction.Axis.Z)
        {
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(cx, cy, cz).color(red, grn, blu, alpha).endVertex();
            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.cos(theta);
                float y = r * Mth.sin(theta);
                builder.vertex(x + cx, cy+y, cz).color(red, grn, blu, alpha).endVertex();
            }
            tessellator.end();
            if (!isFlat)
            {
                builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(cx, cy, cz+h).color(red, grn, blu, alpha).endVertex();
                for (int i = 0; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builder.vertex(x + cx, cy+y, cz+h).color(red, grn, blu, alpha).endVertex();
                }
                tessellator.end();

                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
                float xp = r;
                float yp = 0;
                for (int i = 1; i <= num_steps360; i++)
                {
                    float theta = step * i;
                    float x = r * Mth.cos(theta);
                    float y = r * Mth.sin(theta);
                    builder.vertex(cx + xp, cy + yp, cz + 0).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + xp, cy + yp, cz + h).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + y, cz + h).color(red, grn, blu, alpha).endVertex();
                    builder.vertex(cx + x, cy + y, cz + 0).color(red, grn, blu, alpha).endVertex();
                    xp = x;
                    yp = y;
                }
                tessellator.end();
            }
        }
    }

    public static void drawSphereWireframe(Tesselator tessellator, BufferBuilder builder,
                                           float cx, float cy, float cz,
                                           float r, int subd,
                                           float red, float grn, float blu, float alpha)
    {
        float step = (float)Math.PI / (subd/2);
        int num_steps180 = (int)(Math.PI / step)+1;
        int num_steps360 = (int)(2*Math.PI / step)+1;
        for (int i = 0; i <= num_steps360; i++)
        {
            builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            float theta = step * i ;
            for (int j = 0; j <= num_steps180; j++)
            {
                float phi = step * j ;
                float x = r * Mth.sin(phi) * Mth.cos(theta);
                float z = r * Mth.sin(phi) * Mth.sin(theta);
                float y = r * Mth.cos(phi);
                builder.vertex(x+cx, y+cy, z+cz).color(red, grn, blu, alpha).endVertex();
            }
            tessellator.end();
        }
        for (int j = 0; j <= num_steps180; j++)
        {
            builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); // line loop to line strip
            float phi = step * j ;

            for (int i = 0; i <= num_steps360; i++)
            {
                float theta = step * i;
                float x = r * Mth.sin(phi) * Mth.cos(theta);
                float z = r * Mth.sin(phi) * Mth.sin(theta);
                float y = r * Mth.cos(phi);
                builder.vertex(x+cx, y+cy, z+cz).color(red, grn, blu, alpha).endVertex();
            }
            tessellator.end();
        }

    }

    public static void drawSphereFaces(Tesselator tessellator, BufferBuilder builder,
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
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);  // quad strip to quads
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
                builder.vertex(xb + cx, yp + cy, zb + cz).color(red, grn, blu, alpha).endVertex();
                builder.vertex(xbp + cx, yp + cy, zbp + cz).color(red, grn, blu, alpha).endVertex();
                builder.vertex(xp + cx, y + cy, zp + cz).color(red, grn, blu, alpha).endVertex();
                builder.vertex(x + cx, y + cy, z + cz).color(red, grn, blu, alpha).endVertex();
                xb = x;
                zb = z;
                xbp = xp;
                zbp = zp;
                yp = y;
            }
            tessellator.end();
        }
    }
}
