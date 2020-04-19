package carpet.patches;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BlazeRenderer extends MobEntityRenderer<BlazeEntity, BlazeEntityModel<BlazeEntity>>
{
    private static final Identifier TEXTURE = new Identifier("textures/entity/blaze.png");

    public BlazeRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        super(entityRenderDispatcher, new BlazeEntityModel(), 0.25F);

        //this.addFeature(new BlazeOverlayRenderer<>(this));
    }

    protected void scale(BlazeEntity phantomEntity, MatrixStack matrixStack, float f) {
        float g = 0.5f;
        matrixStack.scale(g, g, g);
        //matrixStack.translate(0.0D, 1.3125D, 0.1875D);
    }

    protected int getBlockLight(BlazeEntity blazeEntity, float f) {
        return 15;
    }

    public Identifier getTexture(BlazeEntity blazeEntity) {
        return TEXTURE;
    }
}
