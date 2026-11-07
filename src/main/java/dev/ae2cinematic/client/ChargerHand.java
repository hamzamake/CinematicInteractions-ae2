package dev.ae2cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ae2cinematic.mixin.ItemInHandRendererAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The charger has no window: right-clicking it reaches the hand in to set the item down (or pick it up) and back.
 * The hand is drawn here for the whole reach, because vanilla's re-equip dip would otherwise pull it off screen the
 * moment the item leaves (or lands in) the hand.
 */
public final class ChargerHand {
    private static final ResourceLocation CHARGER = ResourceLocation.fromNamespaceAndPath("ae2", "charger");
    private static final double LENGTH_MS = 550;
    private static long startedAt = -1;
    private static ItemStack heldAtStart = ItemStack.EMPTY;

    private ChargerHand() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide && event.getHand() == InteractionHand.MAIN_HAND
                && BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).equals(CHARGER)) {
            startedAt = Util.getMillis();
            heldAtStart = event.getEntity().getMainHandItem().copy();
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (startedAt < 0 || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        double t = (Util.getMillis() - startedAt) / LENGTH_MS;
        Minecraft mc = Minecraft.getInstance();
        if (t >= 1 || mc.player == null) {
            startedAt = -1;
            return;
        }
        event.setCanceled(true);
        // reach out, hold a beat at the charger, come back; the item swaps at the charger
        float reach = (float) Math.sin(Math.PI * Math.min(1, t * 1.1));
        ItemStack shown = t < 0.5 ? heldAtStart : mc.player.getMainHandItem();
        float side = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-0.15f * side * reach, -0.18f * reach, -0.55f * reach);
        ps.mulPose(Axis.XP.rotationDegrees(-40f * reach));
        ((ItemInHandRendererAccessor) mc.getEntityRenderDispatcher().getItemInHandRenderer()).ae2cinematic$renderArmWithItem(mc.player,
                event.getPartialTick(), event.getInterpolatedPitch(), InteractionHand.MAIN_HAND, 0, shown, 0, ps,
                event.getMultiBufferSource(), event.getPackedLight());
        ps.popPose();
    }
}
