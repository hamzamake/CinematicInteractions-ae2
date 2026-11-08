package dev.ae2cinematic.client;

import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.slot.FakeSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import appeng.core.sync.network.NetworkHandler;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * A scene at one face of an AE2 block: the camera settles on the face, the hand follows the cursor between the block's
 * slots, a click pulls an item out (it slides out of the block and drops into your inventory) and a held item is put in -
 * for "drag" slots by pushing it in with the mouse.
 */
public abstract class SlotStation<M extends AEBaseMenu> extends AddonMenuStation<M> {
    /** A place on the block the hand can reach: a menu slot, or a named button when {@code slot} is null. */
    protected record Spot(Slot slot, Vec3 point, double w, double h, boolean drag, String button, String label) {
        static Spot of(Slot slot, Vec3 point, double w, double h, boolean drag) {
            return new Spot(slot, point, w, h, drag, null, null);
        }

        static Spot labeled(Slot slot, Vec3 point, double w, double h, boolean drag, String label) {
            return new Spot(slot, point, w, h, drag, null, label);
        }

        static Spot button(String name, Vec3 point, double w, double h) {
            return new Spot(null, point, w, h, false, name, null);
        }
    }

    static final Vec3 UP = new Vec3(0, 1, 0);
    private static final double PULL_OUT = 0.3;
    private static final double PULL_LENGTH = 0.75;
    private static final double DRAG_PIXELS = 45;
    private static final double DRAG_DEPTH = 0.24;

    protected final String name;
    protected final Vec3 normal;
    protected final Vec3 face;
    protected final Vec3 up;
    protected final Vec3 across;
    protected final Quaternionf facing;
    /** Items are modelled facing away from {@code facing}: turned round so they read the right way, not mirrored. */
    protected final Quaternionf itemFacing;
    protected List<Spot> spots = List.of();
    protected Spot hover;

    private ItemStack pulled = ItemStack.EMPTY;
    private Vec3 pulledFrom = Vec3.ZERO;
    private double pulledAt = -10;
    private Spot dragging;
    private double dragStartX;
    private double dragStartY;
    private double dragged;
    private double kickAt = -10;
    private double kick;
    private boolean tookHeld;

    protected SlotStation(BlockPos pos, Direction side, Class<M> type, String name, Vec3 up) {
        super(pos, side, type, 0);
        this.name = name;
        this.normal = Vec3.atLowerCornerOf(side.getNormal());
        this.face = Vec3.atCenterOf(pos).add(normal.scale(0.5));
        this.up = up;
        this.across = up.cross(normal);
        this.facing = new Quaternionf().rotationY((float) Math.toRadians(-side.toYRot()));
        this.itemFacing = new Quaternionf(facing).rotateY((float) Math.PI);
    }

    /** The spots the block has right now (rebuilt every frame: slots fill, empty and appear). */
    protected abstract List<Spot> layout(M menu);

    /** A point on the worked face: {@code u} towards the player's right, {@code v} up, {@code out} towards the player. */
    protected Vec3 at(double u, double v, double out) {
        return face.add(across.scale(u)).add(up.scale(v)).add(normal.scale(out));
    }

    protected static Slot first(AEBaseMenu menu, SlotSemantic semantic) {
        List<Slot> slots = menu.getSlots(semantic);
        return slots.isEmpty() ? null : slots.get(0);
    }

    // ---- camera and body -------------------------------------------------------------------------------------------

    protected double viewDistance() {
        return 0.8;
    }

    protected double viewHeight() {
        return 0.35;
    }

    protected Vec3 lookAt() {
        return face;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(pos.getX() + 0.5 + side.getStepX(), player.getY(), pos.getZ() + 0.5 + side.getStepZ());
        return Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06 ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = lookAt().add(normal.scale(viewDistance())).add(0, viewHeight(), 0);
        Vec3 look = lookAt().subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        return new CameraRig.Pose(eye, yaw, pitch, 0);
    }

    @Override
    public boolean crouches() {
        return true;
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public float bodyPitch() {
        return 32;
    }

    @Override
    public BlockPos lightPos() {
        return pos.relative(side);
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.METAL_STEP;
    }

    /** Where an input slot's leftovers fly back from on leaving; AE2 blocks keep everything (no inputs), so unused. */
    @Override
    protected Vec3 restPoint(int slot) {
        return face;
    }

    @Override
    public Vec3 rest(boolean right) {
        return face.add(normal.scale(0.45)).add(across.scale(right ? 0.28 : -0.28)).add(0, -0.3, 0);
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 from, boolean right, double press) {
        Vec3 dir = new Vec3(aim.x - from.x, 0, aim.z - from.z).normalize();
        return aim.add(0, -0.02 - 0.03 * press, 0).subtract(dir.scale(0.06));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 from) {
        Vec3 dir = new Vec3(hand.x - from.x, 0, hand.z - from.z).normalize();
        return hand.add(dir.scale(0.07)).add(0, -0.043, 0);
    }

    // ---- cursor ----------------------------------------------------------------------------------------------------

    /** Where an animation holds the hand, or null to let it follow the cursor. */
    protected Vec3 animatedAim() {
        double t = Scene.clock() - pulledAt;
        if (t >= 0 && t < PULL_OUT) {
            return pulledFrom.add(normal.scale(0.04 + 0.22 * Anim.outCubic(t / PULL_OUT)));
        }
        if (dragging != null) {
            return dragging.point().add(normal.scale(0.03 + DRAG_DEPTH * (1 - dragged)));
        }
        return null;
    }

    @Override
    public void updateCursor(boolean off) {
        clearCursor();
        Vec3 held = animatedAim();
        if (held != null) {
            aim = held;
            return;
        }
        if (off || menu() == null) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (mouse == null) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (Spot s : spots) {
            double[] at = Picking.toGui(s.point());
            double[] edge = Picking.toGui(s.point().add(across.scale(Math.max(s.w(), s.h()))));
            if (at == null || edge == null) {
                continue;
            }
            double reach = Math.max(6, Math.hypot(edge[0] - at[0], edge[1] - at[1]));
            double d = Math.hypot(mouse[0] - at[0], mouse[1] - at[1]);
            if (d < reach && d < best) {
                best = d;
                hover = s;
            }
        }
        if (hover != null) {
            aim = hover.point().add(normal.scale(0.04));
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        hover = null;
    }

    // ---- actions ---------------------------------------------------------------------------------------------------

    @Override
    public void beforeFrame() {
        M menu = menu();
        spots = menu == null ? List.of() : layout(menu);
        if (menu != null && !tookHeld) {
            tookHeld = true;
            takeHeld(menu);
        }
        if (dragging != null) {
            // Dragging the mouse (any way) or scrolling pushes it in; let go and it slides the rest of the way itself.
            double[] mouse = Picking.mouseGui();
            if (mouse != null) {
                dragged = Math.max(dragged, Anim.clamp01(Math.hypot(mouse[0] - dragStartX, mouse[1] - dragStartY) / DRAG_PIXELS));
            }
            if (!mouseDown()) {
                dragged = Math.min(1, dragged + Scene.frameDt * 3.5);
            }
            if (dragged >= 1) {
                Spot s = dragging;
                dragging = null;
                slotClick(s.slot().index, 0, false);
                kick(0.45);
                sound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.6f);
            }
        }
    }

    /** Walked up holding something the block takes (a cell to a drive, say): it starts in the hand, ready to go in. */
    private void takeHeld(M menu) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        if (held.isEmpty() || !menu.getCarried().isEmpty()) {
            return;
        }
        boolean wanted = spots.stream().anyMatch(s -> s.slot() != null && !(s.slot() instanceof FakeSlot) && s.slot().mayPlace(held));
        if (!wanted) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() == player.getInventory().selected) {
                slotClick(slot.index, 0, false);
                return;
            }
        }
    }

    private static boolean mouseDown() {
        var mouse = Minecraft.getInstance().mouseHandler;
        return mouse.isLeftPressed() || mouse.isRightPressed();
    }

    @Override
    protected void act(int button, boolean shift) {
        Spot s = hover;
        if (s == null || pulling()) {
            return;
        }
        if (s.button() != null) {
            press(s.button());
            return;
        }
        Slot slot = s.slot();
        if (slot instanceof FakeSlot) {
            // ghost slots: AE2 copies whatever is held (or clears the slot) itself
            NetworkHandler.instance().sendToServer(new InventoryActionPacket(InventoryAction.PICKUP_OR_SET_DOWN, slot.index, 0));
            sound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.4f, 1.4f);
            return;
        }
        ItemStack carried = menu().getCarried();
        if (!carried.isEmpty()) {
            if (!slot.mayPlace(carried)) {
                deny();
            } else if (s.drag() && !slot.hasItem()) {
                dragging = s;
                dragged = 0;
                double[] mouse = Picking.mouseGui();
                dragStartX = mouse == null ? 0 : mouse[0];
                dragStartY = mouse == null ? 0 : mouse[1];
            } else {
                place(s, button);
            }
            return;
        }
        if (!slot.hasItem() || !slot.mayPickup(Minecraft.getInstance().player)) {
            deny();
            return;
        }
        pulled = slot.getItem().copy();
        pulledFrom = s.point();
        pulledAt = Scene.clock();
        slotClick(slot.index, 0, true);
        sound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.5f, 1.1f);
    }

    /** Puts the held item straight into the spot's slot. */
    protected void place(Spot s, int button) {
        slotClick(s.slot().index, button, false);
        kick(0.2);
        sound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.5f, 1.0f);
    }

    /** A button spot was clicked. */
    protected void press(String button) {
    }

    @Override
    public void scroll(double amount) {
        if (dragging != null) {
            dragged = Anim.clamp01(dragged + Math.abs(amount) * 0.25); // scrolling pushes too
        }
    }

    protected boolean pulling() {
        return Scene.clock() - pulledAt < PULL_LENGTH;
    }

    @Override
    public boolean busy() {
        return super.busy() || dragging != null || pulling();
    }

    protected void kick(double amount) {
        kick = amount;
        kickAt = Scene.clock();
    }

    @Override
    public double cameraKick() {
        double t = Scene.clock() - kickAt;
        return t >= 0 && t < 0.6 ? kick * Math.exp(-13 * t) : 0;
    }

    protected void sound(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                new SimpleSoundInstance(sound, SoundSource.BLOCKS, volume, pitch, RandomSource.create(), pos));
    }

    // ---- text ------------------------------------------------------------------------------------------------------

    protected static MutableComponent tr(String key, Object... args) {
        return Component.translatable("ae2cinematic." + key, args);
    }

    @Override
    public Component hint() {
        return tr("hint." + name);
    }

    @Override
    public List<Component> tooltip(ItemStack carried) {
        if (dragging != null) {
            return List.of(tr("drag").withStyle(ChatFormatting.AQUA));
        }
        if (hover == null) {
            return null;
        }
        if (hover.button() != null) {
            return buttonTooltip(hover.button());
        }
        Slot slot = hover.slot();
        ItemStack in = slot.getItem();
        if (slot instanceof FakeSlot) {
            List<Component> lines = new ArrayList<>();
            if (!in.isEmpty()) {
                lines.add(in.getHoverName());
            }
            lines.add(tr(carried.isEmpty() ? (in.isEmpty() ? "ghost_empty" : "ghost_clear") : "ghost_set").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (!carried.isEmpty()) {
            if (!slot.mayPlace(carried)) {
                return List.of(tr("wont_fit").withStyle(ChatFormatting.RED));
            }
            return List.of(tr(hover.drag() && in.isEmpty() ? "drag_in" : "put_in").withStyle(ChatFormatting.GRAY));
        }
        if (in.isEmpty()) {
            return List.of(tr("empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        List<Component> lines = new ArrayList<>(describe(in));
        lines.add(tr("take").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    protected List<Component> describe(ItemStack stack) {
        return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    protected List<Component> buttonTooltip(String button) {
        return List.of(tr("button." + button));
    }

    // ---- drawing ---------------------------------------------------------------------------------------------------

    /** Anything the block's own model doesn't show (slot contents, panels). */
    protected void drawExtras(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
    }

    @Override
    public void renderScene(PoseStack ps, MultiBufferSource buffers, Camera camera, int light) {
        if (outro() || menu() == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        drawExtras(ps, buffers, cam, light);
        double t = Scene.clock() - pulledAt;
        if (t >= 0 && t < PULL_LENGTH && !pulled.isEmpty()) {
            Vec3 out = pulledFrom.add(normal.scale(0.24));
            Vec3 at;
            float size = 0.2f;
            if (t < PULL_OUT) {
                at = pulledFrom.add(normal.scale(0.24 * Anim.outCubic(t / PULL_OUT)));
            } else {
                // then it drops down past the camera into the inventory
                double k = Anim.inCubic((t - PULL_OUT) / (PULL_LENGTH - PULL_OUT));
                Vec3 pocket = cam.add(out.subtract(cam).normalize().scale(0.35)).add(0, -0.45, 0);
                at = Anim.lerp(out, pocket, k).add(0, 0.08 * Anim.hump(k), 0);
                size *= (float) (1 - 0.5 * k);
            }
            ItemPlacer.drawOriented(ps, buffers, cam, pulled, at, itemFacing, size, light);
        }
    }

    @Override
    public void renderWorld(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
        if (dragging != null) {
            outline(ps, buffers, cam, dragging, 0.3f, 0.9f, 1f);
        } else if (hover != null && menu() != null) {
            ItemStack carried = menu().getCarried();
            boolean refused = hover.slot() != null && !(hover.slot() instanceof FakeSlot) && !carried.isEmpty()
                    && !hover.slot().mayPlace(carried);
            outline(ps, buffers, cam, hover, refused ? 1f : 0.55f, refused ? 0.3f : 0.95f, refused ? 0.3f : 1f);
        }
    }

    protected void outline(PoseStack ps, MultiBufferSource buffers, Vec3 cam, Spot s, float r, float g, float b) {
        Vec3 a = s.point().add(across.scale(-s.w())).add(up.scale(-s.h())).add(normal.scale(-0.01));
        Vec3 c = s.point().add(across.scale(s.w())).add(up.scale(s.h())).add(normal.scale(0.02));
        LevelRenderer.renderLineBox(ps, buffers.getBuffer(RenderType.lines()), new AABB(a, c).move(-cam.x, -cam.y, -cam.z), r, g, b, 1f);
    }

    /** An item standing on the face, turned towards the player. */
    protected void drawItem(PoseStack ps, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, float size, int light) {
        if (!stack.isEmpty()) {
            ItemPlacer.drawOriented(ps, buffers, cam, stack, at, itemFacing, size, light);
        }
    }

    /** A flat coloured quad (both windings, so it shows whichever way it's culled). */
    protected static void quad(PoseStack ps, MultiBufferSource buffers, Vec3 cam, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = ps.last().pose();
        for (Vec3 v : new Vec3[] {a, b, c, d, d, c, b, a}) {
            vc.vertex(m, (float) (v.x - cam.x), (float) (v.y - cam.y), (float) (v.z - cam.z))
                    .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24).endVertex();
        }
    }

    /** A line of text standing on the face, centred on {@code at}. */
    protected void label(PoseStack ps, MultiBufferSource buffers, Vec3 cam, Component text, Vec3 at, int argb, int light) {
        var font = Minecraft.getInstance().font;
        Vec3 p = at.subtract(cam);
        ps.pushPose();
        ps.translate(p.x, p.y, p.z);
        ps.mulPose(facing);
        ps.scale(0.0045f, -0.0045f, 0.0045f);
        font.drawInBatch(text, -font.width(text) / 2f, -font.lineHeight / 2f, argb, false, ps.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    /** A rectangle on the face, {@code out} in front of it. */
    protected void panel(PoseStack ps, MultiBufferSource buffers, Vec3 cam, double u, double v, double w, double h, double out, int argb) {
        quad(ps, buffers, cam, at(u - w, v - h, out), at(u + w, v - h, out), at(u + w, v + h, out), at(u - w, v + h, out), argb);
    }
}
