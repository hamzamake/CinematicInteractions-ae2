package dev.ae2cinematic.mixin;

import dev.ae2cinematic.Stations;
import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cinematic asks the block state for a menu provider; AE2 blocks don't have one, so hand it ours. */
@Mixin(value = MenuActions.class, remap = false)
abstract class MenuActionsMixin {
    @Inject(method = "provider", at = @At("HEAD"), cancellable = true)
    private static void ae2cinematic$provider(ServerPlayer player, BlockPos pos, StationKind kind,
                                              CallbackInfoReturnable<MenuProvider> cir) {
        MenuProvider ours = Stations.provider(player.level().getBlockState(pos), pos);
        if (ours != null) {
            cir.setReturnValue(ours);
        }
    }
}
