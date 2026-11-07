package dev.ae2cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderArmWithItem")
    void ae2cinematic$renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
                                        float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
                                        MultiBufferSource buffer, int combinedLight);
}
