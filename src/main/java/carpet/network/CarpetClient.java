package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.utils.ShapesRenderer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class CarpetClient
{
    private static boolean fabricApiPayloadsRegistered = false;

    public static void registerPayloads()
    {
        if (fabricApiPayloadsRegistered)
        {
            return;
        }
        try
        {
            // Fabric API checks its dynamic registry before vanilla's fallback codec.
            Class<?> registryClass = Class.forName("net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry");
            registerWithFabricApi(invokePayloadRegistryFactory(registryClass, "clientboundPlay", "playS2C"));
            registerWithFabricApi(invokePayloadRegistryFactory(registryClass, "serverboundPlay", "playC2S"));
            fabricApiPayloadsRegistered = true;
        }
        catch (ClassNotFoundException ignored)
        {
            fabricApiPayloadsRegistered = true;
        }
        catch (ReflectiveOperationException | LinkageError exception)
        {
            CarpetSettings.LOG.warn("Failed to register Carpet custom payload with Fabric API", exception);
        }
    }

    private static Object invokePayloadRegistryFactory(Class<?> registryClass, String... methodNames) throws ReflectiveOperationException
    {
        for (String methodName : methodNames)
        {
            try
            {
                return registryClass.getMethod(methodName).invoke(null);
            }
            catch (NoSuchMethodException ignored)
            {
            }
        }
        throw new NoSuchMethodException(String.join("/", methodNames));
    }

    private static void registerWithFabricApi(Object registry) throws ReflectiveOperationException
    {
        Method registerMethod = registry.getClass().getMethod("register", CustomPacketPayload.Type.class, StreamCodec.class);
        try
        {
            registerMethod.invoke(registry, CarpetPayload.TYPE, CarpetPayload.STREAM_CODEC);
        }
        catch (InvocationTargetException exception)
        {
            if (exception.getCause() instanceof IllegalArgumentException cause && cause.getMessage() != null && cause.getMessage().contains("already registered"))
            {
                return;
            }
            throw exception;
        }
    }

    public record CarpetPayload(CompoundTag data) implements CustomPacketPayload
    {
        public static final StreamCodec<FriendlyByteBuf, CarpetPayload> STREAM_CODEC = CustomPacketPayload.codec(CarpetPayload::write, CarpetPayload::new);

        public static final Type<CarpetPayload> TYPE = new CustomPacketPayload.Type<>(CARPET_CHANNEL);

        public CarpetPayload(FriendlyByteBuf input)
        {
            this(input.readNbt());
        }

        public void write(FriendlyByteBuf output)
        {
            output.writeNbt(data);
        }

        @Override public Type<CarpetPayload> type()
        {
            return TYPE;
        }
    }

    public static final String HI = "69";
    public static final String HELLO = "420";

    public static ShapesRenderer shapes = null;

    private static LocalPlayer clientPlayer = null;
    private static boolean isServerCarpet = false;
    public static String serverCarpetVersion;
    public static final Identifier CARPET_CHANNEL = Identifier.fromNamespaceAndPath("carpet", "hello");

    public static void gameJoined(LocalPlayer player)
    {
        clientPlayer = player;
    }

    public static void disconnect()
    {
        if (isServerCarpet) // multiplayer connection
        {
            isServerCarpet = false;
            clientPlayer = null;
            CarpetServer.onServerClosed(null);
            CarpetServer.onServerDoneClosing(null);
        }
        else // singleplayer disconnect
        {
            CarpetServer.clientPreClosing();
        }
    }

    public static void setCarpet()
    {
        isServerCarpet = true;
    }

    public static LocalPlayer getPlayer()
    {
        return clientPlayer;
    }

    public static boolean isCarpet()
    {
        return isServerCarpet;
    }

    public static boolean sendClientCommand(String command)
    {
        if (!isServerCarpet && CarpetServer.minecraft_server == null)
        {
            return false;
        }
        ClientNetworkHandler.clientCommand(command);
        return true;
    }

    public static void onClientCommand(Tag t)
    {
        CarpetSettings.LOG.info("Server Response:");
        CompoundTag tag = (CompoundTag) t;
        CarpetSettings.LOG.info(" - id: " + tag.getString("id"));
        if (tag.contains("error"))
        {
            CarpetSettings.LOG.warn(" - error: " + tag.getString("error"));
        }
        if (tag.contains("output"))
        {
            ListTag outputTag = (ListTag) tag.get("output");
            for (int i = 0; i < outputTag.size(); i++)
            {
                CarpetSettings.LOG.info(" - response: " + outputTag.getString(i).orElseThrow());
            }
        }
    }
}
