package carpet.script.utils.shapes;

import org.jspecify.annotations.Nullable;
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
}
