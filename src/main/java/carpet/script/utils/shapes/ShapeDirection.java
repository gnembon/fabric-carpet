package carpet.script.utils.shapes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;

public enum ShapeDirection
{
    NORTH,
    SOUTH,
    EAST,
    WEST,
    UP,
    DOWN,
    CAMERA,
    PLAYER;

    @Nullable
    public static ShapeDirection fromString(String direction)
    {
        return switch (direction.toLowerCase(Locale.ROOT))
        {
            case "north" -> NORTH;
            case "south" -> SOUTH;
            case "east" -> EAST;
            case "west" -> WEST;
            case "up" -> UP;
            case "down" -> DOWN;
            case "camera" -> CAMERA;
            case "player" -> PLAYER;
            default -> null;
        };
    }

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
}
