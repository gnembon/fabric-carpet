package carpet.mixins;

import carpet.fakes.BlockEntityRenderDispatcherInterface;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcher_movableTEMixin implements BlockEntityRenderDispatcherInterface
{
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
    
    @Shadow
    public abstract void renderEntity(BlockEntity blockEntity_1, double double_1, double double_2, double double_3,
            float float_1, int int_1, boolean boolean_1);
    
    /**
     * @author 2No2Name
     */
    //Renders the BlockEntity offset by the amount specified in the arguments xOffset yOffset zOffset (the moving block moved in the animation by this)
    public void renderBlockEntityOffset(BlockEntity blockEntity_1, float partialTicks, int destroyStage, double xOffset,
            double yOffset, double zOffset)
    {
        if (blockEntity_1.getSquaredDistance(this.cameraEntity.getPos().x - xOffset, this.cameraEntity.getPos().y - yOffset,
                this.cameraEntity.getPos().z - zOffset) < blockEntity_1.getSquaredRenderDistance())
        {
            GuiLighting.enable();
            int i = this.world.getLightmapIndex(blockEntity_1.getPos(), 0);
            int j = i % 65536;
            int k = i / 65536;
            GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, (float) j, (float) k);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            BlockPos blockpos = blockEntity_1.getPos();
            this.renderEntity(blockEntity_1, (double) blockpos.getX() - renderOffsetX + xOffset, (double) blockpos.getY() - renderOffsetY + yOffset, (double) blockpos.getZ() - renderOffsetZ + zOffset, partialTicks, destroyStage, false);
        }
    }
}
