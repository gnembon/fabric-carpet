package carpet.patches;

import carpet.fakes.ServerPlayerEntityInterface;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Objects;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerMPFake extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static void createFake(String playerName, MinecraftServer server,
                                  double d0, double d1, double d2, double yaw, double pitch,
                                  RegistryKey<World> dimensionId, GameMode gamemode,
                                  boolean isSpawnerPrivileged
                                                )
            throws FakePlayerSpawnException {
        Objects.requireNonNull(playerName);
        Objects.requireNonNull(server);
        Objects.requireNonNull(dimensionId);
        Objects.requireNonNull(gamemode);

        PlayerManager playerManager = server.getPlayerManager();

        // if this player already logged in
        if (playerManager.getPlayer(playerName) != null)
        {
            throw new FakePlayerSpawnException("r Player ", "rb " + playerName, "r  is already logged on");
        }

        // get player profile from auth server
        GameProfile profile = server.getUserCache().findByName(playerName);

        // if the player with this playerName does not exist
        if (profile == null)
        {
            throw new FakePlayerSpawnException("r Player " + playerName + " is either banned by Mojang, or " +
                    "auth servers are down. Banned players can only be summoned in Singleplayer " +
                    "and in servers in off-line mode.");
        }

        // if this player is banned locally
        if (playerManager.getUserBanList().contains(profile))
        {
            throw new FakePlayerSpawnException("r Player ", "rb " + playerName, "r  is banned on this server");
        }

        // if the player is whitelisted and the command runner is not privileged
        if (playerManager.isWhitelistEnabled() && playerManager.isWhitelisted(profile) && !isSpawnerPrivileged)
        {
            throw new FakePlayerSpawnException("r Whitelisted players can only be spawned by operators");
        }

        // get necessary server managers
        ServerWorld world = server.getWorld(dimensionId);
        ServerPlayerInteractionManager playerInteractionManager = new ServerPlayerInteractionManager(world);

        // load player texture
        if (profile.getProperties().containsKey("textures"))
        {
            profile = SkullBlockEntity.loadProperties(profile);
        }

        // initiate player instance
        EntityPlayerMPFake player = new EntityPlayerMPFake(server, world, profile, playerInteractionManager, false);
        player.fixStartingPosition = () -> player.refreshPositionAndAngles(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerManager().onPlayerConnect(new NetworkManagerFake(NetworkSide.SERVERBOUND), player);
        player.teleport(world, d0, d1, d2, (float)yaw, (float)pitch);
        player.setHealth(20.0F);
        player.removed = false;
        player.stepHeight = 0.6F;
        playerInteractionManager.setGameMode(gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(player, (byte) (player.headYaw * 256 / 360)), dimensionId);//instance.dimension);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(player), dimensionId);//instance.dimension);
        player.getServerWorld().getChunkManager().updateCameraPosition(player);
        player.dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); // show all model layers (incl. capes)

    }

    public static EntityPlayerMPFake createShadow(MinecraftServer server, ServerPlayerEntity player)
    {
        player.getServer().getPlayerManager().remove(player);
        player.networkHandler.disconnect(new TranslatableText("multiplayer.disconnect.duplicate_login"));
        ServerWorld worldIn = player.getServerWorld();//.getWorld(player.dimension);
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


        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(playerShadow, (byte) (player.headYaw * 256 / 360)), playerShadow.world.getRegistryKey());
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
    protected void onEquipStack(ItemStack stack)
    {
        if (!isUsingItem()) super.onEquipStack(stack);
    }

    @Override
    public void kill()
    {
        kill(Messenger.s("Killed"));
    }

    public void kill(Text reason)
    {
        shakeOff();
        this.server.send(new ServerTask(this.server.getTicks(), () -> {
            this.networkHandler.onDisconnected(reason);
        }));
    }

    @Override
    public void tick()
    {
        if (this.getServer().getTicks() % 10 == 0)
        {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().getChunkManager().updateCameraPosition(this);
            //if (netherPortalCooldown==10) onTeleportationDone(); <- causes hard crash but would need to be done to enable portals
        }
        super.tick();
        this.playerTick();
    }

    private void shakeOff()
    {
        if (getVehicle() instanceof PlayerEntity) stopRiding();
        for (Entity passenger : getPassengersDeep())
        {
            if (passenger instanceof PlayerEntity) passenger.stopRiding();
        }
    }

    @Override
    public void onDeath(DamageSource cause)
    {
        shakeOff();
        super.onDeath(cause);
        setHealth(20);
        this.hungerManager = new HungerManager();
        kill(this.getDamageTracker().getDeathMessage());
    }

    @Override
    public String getIp()
    {
        return "127.0.0.1";
    }

    public static class FakePlayerSpawnException extends Exception {
        private final Object[] message;

        /**
         * Create an exception indicating a failure when trying to spawn a fake player.
         * @param message the message to be passed to {@link Messenger::m} method.
         */
        public FakePlayerSpawnException(Object ... message) {
            this.message = message;
        }

        /**
         * Get the detailed error message which should be sent to user via {@link Messenger::m} method.
         */
        public Object[] getMessengerMessage() {
            return message;
        }
    }
}
