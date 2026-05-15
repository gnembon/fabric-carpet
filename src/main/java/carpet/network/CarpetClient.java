package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.utils.ShapesRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CarpetClient
{
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
    private static boolean payloadRegistered = false;
    public static String serverCarpetVersion;
    public static final ResourceLocation CARPET_CHANNEL = ResourceLocation.fromNamespaceAndPath("carpet", "hello");

    public static void registerPayloadType()
    {
        if (payloadRegistered) return;
        try
        {
            Class<?> payloadTypeRegistry = Class.forName("net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry");
            registerPayloadType(payloadTypeRegistry, "playS2C");
            registerPayloadType(payloadTypeRegistry, "playC2S");
            payloadRegistered = true;
        }
        catch (ClassNotFoundException ignored)
        {
            // Fabric API is optional. The CustomPacketPayload mixin still handles vanilla registration.
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            CarpetSettings.LOG.warn("Failed to register Carpet custom payload type with Fabric API", e);
        }
    }

    private static void registerPayloadType(Class<?> payloadTypeRegistry, String registryGetter) throws ReflectiveOperationException
    {
        Object registry = payloadTypeRegistry.getMethod(registryGetter).invoke(null);
        Method register = payloadTypeRegistry.getMethod("register", CustomPacketPayload.Type.class, StreamCodec.class);
        try
        {
            register.invoke(registry, CarpetPayload.TYPE, CarpetPayload.STREAM_CODEC);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException && cause.getMessage() != null && cause.getMessage().contains("already registered")) return;
            throw e;
        }
    }

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
