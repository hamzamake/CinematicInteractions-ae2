package dev.ae2cinematic.client;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.WirelessAccessPointMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wireless Access Point: touch the antenna tip with a wireless terminal to link it (it comes straight back to your hand),
 * slot boosters into the chassis like a game cartridge, click the one sticking out to take it back.
 */
final class WapStation extends SlotStation<WirelessAccessPointMenu> {
    private static final double TOUCH = 0.3;

    private final Vec3 tip;
    /** Where the booster slot opens on the chassis; boosters stand up out of it. */
    private final Vec3 mouth;
    private double touchedAt = -10;
    private boolean linking;
    private double linkedAt = -10;

    WapStation(BlockPos pos, Direction side, BlockState state) {
        super(pos, side, WirelessAccessPointMenu.class, "wireless_access_point", UP);
        // AE2 15's model: the antenna runs along the facing from the back plate to 1 px past the block's centre.
        Direction facing = BlockOrientation.get(state).getSide(RelativeSide.FRONT);
        Vec3 f = Vec3.atLowerCornerOf(facing.getNormal()), center = Vec3.atCenterOf(pos);
        tip = center.add(f.scale(1 / 16.0));
        // The chassis' back plate is 6 px square and 2 px thick, flush with the back of the block, with a cross of arms in
        // front of it. On a sideways antenna the slot is in the plate's top edge; on one pointing up or down it's in the
        // plate's rim on the side facing the player, off to the right of the arm in the middle.
        mouth = facing.getAxis().isHorizontal() ? center.subtract(f.scale(7 / 16.0)).add(0, 3 / 16.0, 0)
                : center.add(0, facing == Direction.UP ? -6 / 16.0 : 8 / 16.0, 0).add(normal.scale(2.5 / 16.0)).add(across.scale(2.5 / 16.0));
    }

    @Override
    protected double viewDistance() {
        return 1.25; // the antenna is small: stand back far enough to see all of it
    }

    @Override
    protected double viewHeight() {
        return 0.4;
    }

    @Override
    protected Vec3 lookAt() {
        return center();
    }

    @Override
    protected List<Spot> layout(WirelessAccessPointMenu menu) {
        Slot in = first(menu, SlotSemantics.MACHINE_INPUT), out = first(menu, SlotSemantics.MACHINE_OUTPUT);
        Slot booster = first(menu, SlotSemantics.STORAGE);
        List<Spot> spots = new ArrayList<>(2);
        // A linked terminal still waiting in the output is offered at the tip; otherwise the tip takes terminals to link.
        Slot tipSlot = out != null && out.hasItem() ? out : in;
        if (tipSlot != null) {
            spots.add(Spot.of(tipSlot, tip, 0.06, 0.06, false));
        }
        if (booster != null) {
            spots.add(Spot.of(booster, mouth.add(0, 0.04, 0), 0.06, 0.06, true));
        }
        return spots;
    }

    @Override
    public void beforeFrame() {
        super.beforeFrame();
        WirelessAccessPointMenu menu = menu();
        Slot out = menu == null ? null : first(menu, SlotSemantics.MACHINE_OUTPUT);
        // AE2 links the terminal the moment it lands and moves it to the output: hand it right back.
        if (linking && out != null && out.hasItem() && menu.getCarried().isEmpty() && Scene.clock() - touchedAt > TOUCH) {
            linking = false;
            linkedAt = Scene.clock();
            slotClick(out.index, 0, false);
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
            kick(0.3);
        }
    }

    @Override
    protected void place(Spot s, int button) {
        WirelessAccessPointMenu menu = menu();
        if (s.slot() == first(menu, SlotSemantics.MACHINE_INPUT)) {
            linking = true;
            touchedAt = Scene.clock();
            slotClick(s.slot().index, 0, false);
            sound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.6f, 1.8f);
            return;
        }
        super.place(s, button);
    }

    @Override
    protected Vec3 animatedAim() {
        double t = Scene.clock() - touchedAt;
        if (t >= 0 && t < TOUCH) {
            return tip.add(normal.scale(0.015));
        }
        return super.animatedAim();
    }

    @Override
    public boolean busy() {
        return super.busy() || Scene.clock() - touchedAt < TOUCH;
    }

    @Override
    public double flash() {
        double t = Scene.clock() - linkedAt;
        return t >= 0 && t < 1 ? 0.3 * Math.exp(-8 * t) : 0;
    }

    @Override
    public int flashColor() {
        return 0x9FE8FF;
    }

    @Override
    protected void drawExtras(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
        // the slot's dark mouth on the chassis, then the booster half in it (the chassis hides the half that's inside)
        Vec3 m = mouth.add(0, 0.002, 0);
        Vec3 w = across.scale(0.06), d = normal.scale(0.014);
        quad(ps, buffers, cam, m.subtract(w).subtract(d), m.add(w).subtract(d), m.add(w).add(d), m.subtract(w).add(d), 0xFF101216);
        Slot booster = first(menu(), SlotSemantics.STORAGE);
        if (booster != null && booster.hasItem()) {
            ItemPlacer.drawOriented(ps, buffers, cam, booster.getItem(), mouth.add(0, 0.035, 0), itemFacing, 0.16f, light);
        }
    }

    @Override
    public void renderScreen(GuiGraphics gui, Font font, float partial) {
        WirelessAccessPointMenu menu = menu();
        if (menu == null) {
            return;
        }
        Slot booster = first(menu, SlotSemantics.STORAGE);
        int boosters = booster == null ? 0 : booster.getItem().getCount();
        // AE2 syncs the range in tenths of a block
        var text = tr("wap.status", String.format("%.1f", menu.getRange() / 10.0), boosters);
        gui.drawCenteredString(font, text, gui.guiWidth() / 2, 42, 0xFFBFEFFF); // below Cinematic's hotbar strip
    }
}
