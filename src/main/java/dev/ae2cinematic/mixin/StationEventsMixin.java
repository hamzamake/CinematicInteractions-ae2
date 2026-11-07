package dev.ae2cinematic.mixin;

import dev.ae2cinematic.Stations;
import dev.dyslexic.cinematic.station.StationEvents;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cinematic stations are keyed by block; on a cable bus only the Pattern Encoding Terminal part should start one. */
@Mixin(value = StationEvents.class, remap = false)
abstract class StationEventsMixin {
    @Inject(method = "onRightClickBlock", at = @At("HEAD"), cancellable = true)
    private static void ae2cinematic$onlyEncoderParts(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        if (!Stations.allowsScene(event.getLevel(), event.getPos(), event.getHitVec().getLocation())) {
            ci.cancel();
        }
    }
}
