package carpet.patches;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.network.NetworkSide;
import net.minecraft.text.TranslatableText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import carpet.fakes.ServerPlayerEntityInterface;
import carpet.utils.Messenger;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerMPFake extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static EntityPlayerMPFake createFake(String username, MinecraftServer server, double d0, double d1, double d2, double yaw, double pitch, DimensionType dimension, GameMode gamemode)
    {
        //prolly half of that crap is not necessary, but it works
        ServerWorld worldIn = server.getWorld(dimension);
        ServerPlayerInteractionManager interactionManagerIn = new ServerPlayerInteractionManager(worldIn);
        GameProfile gameprofile = server.getUserCache().findByName(username);
        if (gameprofile == null)
        {
            return null;
        }
        if (gameprofile.getProperties().containsKey("textures"))
        {
            gameprofile = SkullBlockEntity.loadProperties(gameprofile);
        }
        EntityPlayerMPFake instance = new EntityPlayerMPFake(server, worldIn, gameprofile, interactionManagerIn, false);
        instance.fixStartingPosition = () -> instance.refreshPositionAndAngles(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerManager().onPlayerConnect(new NetworkManagerFake(NetworkSide.SERVERBOUND), instance);
        if (instance.dimension != dimension) //player was logged in in a different dimension
        {
            ServerWorld old_world = server.getWorld(instance.dimension);
            instance.dimension = dimension;
            old_world.removePlayer(instance);
            instance.removed = false;
            //worldIn.spawnEntity(instance);
            instance.setWorld(worldIn);
            server.getPlayerManager().sendWorldInfo(instance, worldIn);
            instance.networkHandler.requestTeleport(d0, d1, d2, (float) yaw, (float) pitch);
            instance.interactionManager.setWorld(worldIn);
            worldIn.onPlayerChangeDimension(instance);
        }
        instance.setHealth(20.0F);
        instance.removed = false;
        instance.networkHandler.requestTeleport(d0, d1, d2, (float) yaw, (float) pitch);
        instance.stepHeight = 0.6F;
        interactionManagerIn.setGameMode(gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(instance, (byte) (instance.headYaw * 256 / 360)), instance.dimension);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(instance), instance.dimension);
        instance.getServerWorld().getChunkManager().updateCameraPosition(instance);
        instance.dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); // show all model layers (incl. capes)
        return instance;
    }

    public static EntityPlayerMPFake createShadow(MinecraftServer server, ServerPlayerEntity player)
    {
        player.getServer().getPlayerManager().remove(player);
        player.networkHandler.disconnect(new TranslatableText("multiplayer.disconnect.duplicate_login"));
        ServerWorld worldIn = server.getWorld(player.dimension);
        ServerPlayerInteractionManager interactionManagerIn = new ServerPlayerInteractionManager(worldIn);
        GameProfile gameprofile = player.getGameProfile();
        EntityPlayerMPFake playerShadow = new EntityPlayerMPFake(server, worldIn, gameprofile, interactionManagerIn, true);
        server.getPlayerManager().onPlayerConnect(new NetworkManagerFake(NetworkSide.SERVERBOUND), playerShadow);

        playerShadow.setHealth(player.getHealth());
        playerShadow.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.yaw, player.pitch);
        interactionManagerIn.setGameMode(player.interactionManager.getGameMode());
        ((ServerPlayerEntityInterface) playerShadow).getActionPack().copyFrom(((ServerPlayerEntityInterface) player).getActionPack());
        playerShadow.stepHeight = 0.6F;
        playerShadow.dataTracker.set(PLAYER_MODEL_PARTS, player.getDataTracker().get(PLAYER_MODEL_PARTS));


        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(playerShadow, (byte) (player.headYaw * 256 / 360)), playerShadow.dimension);
        server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, playerShadow));
        player.getServerWorld().getChunkManager().updateCameraPosition(playerShadow);
        return playerShadow;
    }

    private EntityPlayerMPFake(MinecraftServer server, ServerWorld worldIn, GameProfile profile, ServerPlayerInteractionManager interactionManagerIn, boolean shadow)
    {
        super(server, worldIn, profile, interactionManagerIn);
        isAShadow = shadow;
    }


    @Override
    public void kill()
    {
        this.server.send(new ServerTask(this.server.getTicks(), () -> {
            this.networkHandler.onDisconnected(Messenger.s("Killed"));
        }));
    }

    @Override
    public void tick()
    {
        if (this.getServer().getTicks() % 10 == 0)
        {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().getChunkManager().updateCameraPosition(this);
        }
        super.tick();
        this.playerTick();
    }

    @Override
    public void onDeath(DamageSource cause)
    {
        super.onDeath(cause);
        setHealth(20);
        this.hungerManager = new HungerManager();
        kill();
    }
}
