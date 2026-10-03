package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Sleep-only adaptation of Carpet AS Addition's fake-player exclusion.
 * Portions Copyright (c) 2026 AstraSolis. MIT licensed; see LICENSE.
 */
@Mixin(SleepStatus.class)
public abstract class SleepStatus_fakePlayerSleepIgnoreMixin
{
    @Unique
    private static final Predicate<ServerPlayer> REAL_DEEP_SLEEPER =
            player -> !(player instanceof EntityPlayerMPFake) && player.isSleepingLongEnough();

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean ignoreFakePlayer(boolean spectator, @Local ServerPlayer player)
    {
        // Exclude bots from both active and sleeping counts without changing their game mode.
        return spectator || (CarpetSettings.fakePlayerSleepIgnore && player instanceof EntityPlayerMPFake);
    }

    @ModifyArg(method = "areEnoughDeepSleeping", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"), index = 0)
    private Predicate<ServerPlayer> ignoreDeepSleepingFakePlayer(Predicate<ServerPlayer> original)
    {
        // Vanilla filters with ServerPlayer::isSleepingLongEnough. Keep that check, but
        // prevent sleeping bots from satisfying a real player's remaining sleep delay.
        return CarpetSettings.fakePlayerSleepIgnore ? REAL_DEEP_SLEEPER : original;
    }
}
