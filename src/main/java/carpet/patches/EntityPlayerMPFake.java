package carpet.patches;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import carpet.fakes.ServerPlayerEntityInterface;
import carpet.utils.Messenger;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerMPFake extends ServerPlayer
{
    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static EntityPlayerMPFake createFake(String username, MinecraftServer server, double d0, double d1, double d2, double yaw, double pitch, ResourceKey<Level> dimensionId, GameType gamemode, boolean flying)
    {
        //prolly half of that crap is not necessary, but it works
        ServerLevel worldIn = server.getLevel(dimensionId);
        GameProfileCache.setUsesAuthentication(false);
        GameProfile gameprofile;
        try {
            gameprofile = server.getProfileCache().get(username).orElse(null); //findByName  .orElse(null)
        }
        finally {
            GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (gameprofile == null)
        {
            if (!CarpetSettings.allowSpawningOfflinePlayers)
            {
                return null;
            } else {
                gameprofile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
            }
        }
        if (gameprofile.getProperties().containsKey("textures"))
        {
            AtomicReference<GameProfile> result = new AtomicReference<>();
            SkullBlockEntity.updateGameprofile(gameprofile, result::set);
            gameprofile = result.get();
        }
        EntityPlayerMPFake instance = new EntityPlayerMPFake(server, worldIn, gameprofile, false);
        instance.fixStartingPosition = () -> instance.moveTo(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance);
        instance.teleportTo(worldIn, d0, d1, d2, (float)yaw, (float)pitch);
        instance.setHealth(20.0F);
        instance.unsetRemoved();
        instance.maxUpStep = 0.6F;
        instance.gameMode.changeGameModeForPlayer(gamemode);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);//instance.dimension);
        server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(instance), dimensionId);//instance.dimension);
        //instance.world.getChunkManager(). updatePosition(instance);
        instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show all model layers (incl. capes)
        instance.getAbilities().flying = flying;
        return instance;
    }

    public static EntityPlayerMPFake createShadow(MinecraftServer server, ServerPlayer player)
    {
        player.getServer().getPlayerList().remove(player);
        player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        ServerLevel worldIn = player.getLevel();//.getWorld(player.dimension);
        GameProfile gameprofile = player.getGameProfile();
        EntityPlayerMPFake playerShadow = new EntityPlayerMPFake(server, worldIn, gameprofile, true);
        playerShadow.setChatSession(player.getChatSession());
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerShadow);

        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        playerShadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
        ((ServerPlayerEntityInterface) playerShadow).getActionPack().copyFrom(((ServerPlayerEntityInterface) player).getActionPack());
        playerShadow.maxUpStep = 0.6F;
        playerShadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));


        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (player.yHeadRot * 256 / 360)), playerShadow.level.dimension());
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, playerShadow));
        //player.world.getChunkManager().updatePosition(playerShadow);
        playerShadow.getAbilities().flying = player.getAbilities().flying;
        return playerShadow;
    }

    private EntityPlayerMPFake(MinecraftServer server, ServerLevel worldIn, GameProfile profile, boolean shadow)
    {
        super(server, worldIn, profile);
        isAShadow = shadow;
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack)
    {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill()
    {
        kill(Messenger.s("Killed"));
    }

    public void kill(Component reason)
    {
        shakeOff();
        this.server.tell(new TickTask(this.server.getTickCount(), () -> {
            this.connection.onDisconnect(reason);
        }));
    }

    @Override
    public void tick()
    {
        if (this.getServer().getTickCount() % 10 == 0)
        {
            this.connection.resetPosition();
            this.getLevel().getChunkSource().move(this);
            hasChangedDimension(); //<- causes hard crash but would need to be done to enable portals // not as of 1.17
        }
        try
        {
            super.tick();
            this.doTick();
        }
        catch (NullPointerException ignored)
        {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game not gonna crash violently.
        }


    }

    private void shakeOff()
    {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers())
        {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(DamageSource cause)
    {
        shakeOff();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress()
    {
        return "127.0.0.1";
    }
}
