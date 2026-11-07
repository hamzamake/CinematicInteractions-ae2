package dev.ae2cinematic.client;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;

/** A block whose scene is just its slots laid out on a face, each showing what it holds. */
class SimpleStation<M extends AEBaseMenu> extends SlotStation<M> {
    interface Layout<M extends AEBaseMenu> {
        List<Spot> spots(SimpleStation<M> station, M menu);
    }

    private final Layout<M> layout;

    SimpleStation(BlockPos pos, Direction side, Class<M> type, String name, Layout<M> layout) {
        super(pos, side, type, name, UP);
        this.layout = layout;
    }

    @Override
    protected List<Spot> layout(M menu) {
        List<Spot> spots = new ArrayList<>(layout.spots(this, menu));
        // upgrade cards go in a column down the right edge, on every block that takes them
        grid(spots, menu.getSlots(SlotSemantics.UPGRADE), 6, 1, 0.39, 0.28, 0.12, 0.04, true, "upgrades");
        return spots;
    }

    Vec3 point(double u, double v) {
        return at(u, v, 0.02);
    }

    /** {@code slots} in rows of {@code columns}, the first centred on (u, v), {@code step} apart; the first one labelled. */
    void grid(List<Spot> out, List<Slot> slots, int max, int columns, double u, double v, double step, double size, boolean drag, String label) {
        for (int i = 0; i < slots.size() && i < max; i++) {
            out.add(Spot.labeled(slots.get(i), point(u + (i % columns) * step, v - (i / columns) * step), size, size, drag,
                    i == 0 ? label : null));
        }
    }

    @Override
    protected void drawExtras(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
        for (Spot s : spots) {
            if (s.slot() == null) {
                continue;
            }
            // a framed socket for every slot, so it's clear where things go
            Vec3 c = s.point().subtract(normal.scale(0.015));
            double w = s.w() + 0.012, h = s.h() + 0.012;
            quad(ps, buffers, cam, c.add(across.scale(-w)).add(up.scale(-h)), c.add(across.scale(w)).add(up.scale(-h)),
                    c.add(across.scale(w)).add(up.scale(h)), c.add(across.scale(-w)).add(up.scale(h)), 0xFF8A93A6);
            Vec3 in = c.add(normal.scale(0.002));
            quad(ps, buffers, cam, in.add(across.scale(-s.w())).add(up.scale(-s.h())), in.add(across.scale(s.w())).add(up.scale(-s.h())),
                    in.add(across.scale(s.w())).add(up.scale(s.h())), in.add(across.scale(-s.w())).add(up.scale(s.h())), 0xFF15171C);
            drawItem(ps, buffers, cam, s.slot().getItem(), s.point().add(normal.scale(0.02)), (float) (s.w() * 1.7), light);
            if (s.label() != null) {
                label(ps, buffers, cam, tr("slot." + s.label()), c.add(up.scale(h + 0.03)).add(normal.scale(0.004)), 0xFFE8F6FF, 0xF000F0);
            }
        }
    }
}
