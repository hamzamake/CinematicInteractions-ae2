package dev.ae2cinematic.mixin;

import dev.ae2cinematic.Ae2Provider;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("HEAD"), cancellable = true)
    private void ae2cinematic$openAe2(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (provider instanceof Ae2Provider ae2) {
            cir.setReturnValue(ae2.open((ServerPlayer) (Object) this));
        }
    }
}
