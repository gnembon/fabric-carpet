package carpet.mixins;

import carpet.fakes.BlockEntityRenderDispatcherInterface;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcher_movableTEMixin implements BlockEntityRenderDispatcherInterface
{/*
    @Shadow
    public static double renderOffsetX;
    @Shadow
    public static double renderOffsetY;
    @Shadow
    public static double renderOffsetZ;
    @Shadow
    public Camera cameraEntity;
    @Shadow
    public World world;
    
    //@Shadow //Shadow doesn't work due to private+static+mixins requires abstract, therefore copied the whole method. //Todo fix this
    private static void renderEntity(BlockEntity blockEntity_1, Runnable runnable_1){
        try {
            runnable_1.run();
        } catch (Throwable var5) {
            CrashReport crashReport_1 = CrashReport.create(var5, "Rendering Block Entity");
            CrashReportSection crashReportSection_1 = crashReport_1.addElement("Block Entity Details");
            blockEntity_1.populateCrashReport(crashReportSection_1);
            throw new CrashException(crashReport_1);
        }
    }
    @Shadow
    public abstract <T extends BlockEntity> BlockEntityRenderer<T> get(/@Nullable/ BlockEntity blockEntity_1);
    
    /
      @author 2No2Name
     /
    //Renders the BlockEntity offset by the amount specified in the arguments xOffset yOffset zOffset (the moving block moved in the animation by this)
    //Code copied and modified from BlockEntityRenderDispatcher::render(BlockEntity blockEntity_1, float float_1, int int_1, BlockRenderLayer blockRenderLayer_1, BufferBuilder bufferBuilder_1)
    public void renderBlockEntityOffset(BlockEntity blockEntity_1, float partialTicks, int destroyStage, BlockRenderLayer blockRenderLayer_1, BufferBuilder bufferBuilder_1, double xOffset,
                                        double yOffset, double zOffset){
        if (blockEntity_1.getSquaredDistance(this.cameraEntity.getPos().x - xOffset, this.cameraEntity.getPos().y - yOffset, this.cameraEntity.getPos().z - zOffset) < blockEntity_1.getSquaredRenderDistance()) {
            BlockEntityRenderer<BlockEntity> blockEntityRenderer_1 = this.get(blockEntity_1);
            if (blockEntityRenderer_1 != null) {
                if (blockEntity_1.hasWorld() && blockEntity_1.getType().supports(blockEntity_1.getCachedState().getBlock())) {
                    BlockPos blockPos_1 = blockEntity_1.getPos();
                    renderEntity(blockEntity_1, () ->
                    {
                        bufferBuilder_1.method_22629(); //add layer to stack
                        bufferBuilder_1.method_22626(xOffset,yOffset,zOffset); //add offset for the Blockentity
                        blockEntityRenderer_1.method_22747(blockEntity_1, (double)blockPos_1.getX() - renderOffsetX, (double)blockPos_1.getY() - renderOffsetY, (double)blockPos_1.getZ() - renderOffsetZ, partialTicks, destroyStage, bufferBuilder_1, blockRenderLayer_1, blockPos_1);
                        //bufferBuilder_1.method_22626(-xOffset,-yOffset,-zOffset); //remove offset
                        bufferBuilder_1.method_22630(); //remove layer from stack
                    }
                    );
                }
            }
        }
    }*/
}
