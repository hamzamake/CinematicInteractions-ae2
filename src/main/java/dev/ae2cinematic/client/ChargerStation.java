package dev.ae2cinematic.client;

import appeng.menu.SlotSemantics;
import dev.ae2cinematic.ChargerMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;

/**
 * Charger: the hand reaches into the frame and grabs what's charging (it comes out and drops into your inventory), or
 * carries a held item in and pushes it into place. AE2's own renderer keeps drawing the item while it sits there.
 */
final class ChargerStation extends SlotStation<ChargerMenu> {
    private final Vec3 item;

    ChargerStation(BlockPos pos, Direction side) {
        super(pos, side, ChargerMenu.class, "charger", UP);
        // where AE2's ChargerBlockEntityRenderer floats the item: the middle of the block, 0.35 up from its bottom
        item = Vec3.atLowerCornerOf(pos).add(0.5, 0.35, 0.5);
    }

    @Override
    protected List<Spot> layout(ChargerMenu menu) {
        Slot slot = first(menu, SlotSemantics.MACHINE_INPUT);
        return slot == null ? List.of() : List.of(Spot.of(slot, item, 0.12, 0.12, true));
    }

    @Override
    protected Vec3 lookAt() {
        return item;
    }

    @Override
    protected double viewDistance() {
        return 1.35; // from the item in the middle, so about 0.85 in front of the frame
    }
}
