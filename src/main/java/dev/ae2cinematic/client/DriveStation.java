package dev.ae2cinematic.client;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.DriveMenu;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** ME Drive: ten bays, hover one to read its cell, click to pull it out, push a held cell in. */
final class DriveStation extends SlotStation<DriveMenu> {
    private static final int SHOWN_CONTENTS = 6;

    DriveStation(BlockPos pos, Direction side, BlockState state) {
        super(pos, side, DriveMenu.class, "drive", topOf(state, side));
    }

    /** A drive can be spun on its front; its bays turn with it. */
    private static Vec3 topOf(BlockState state, Direction front) {
        Direction top = BlockOrientation.get(state).getSide(RelativeSide.TOP);
        return top.getAxis() == front.getAxis() ? UP : Vec3.atLowerCornerOf(top.getNormal());
    }

    @Override
    protected List<Spot> layout(DriveMenu menu) {
        // Bay i sits where AE2's DriveBakedModel puts it: row i/2, column i%2, 6x2 px, 1 px deep in the front.
        List<Slot> cells = menu.getSlots(SlotSemantics.STORAGE_CELL);
        List<Spot> spots = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            int row = i / 2, col = i % 2;
            spots.add(Spot.of(cells.get(i), at((8 * col - 4) / 16.0, (6 - 3 * row) / 16.0, 0), 3.4 / 16, 1.3 / 16, true));
        }
        return spots;
    }

    @Override
    protected List<Component> describe(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
        if (stack.getItem() instanceof IBasicCellItem cellItem) {
            lines.add(tr("cell.type", cellItem.getKeyType().getDescription()).withStyle(ChatFormatting.GRAY));
        }
        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (cell == null) {
            return lines;
        }
        if (cell instanceof BasicCellInventory basic) {
            long used = basic.getUsedBytes(), total = basic.getTotalBytes();
            lines.add(tr("cell.bytes", bar(used, total), used, total).withStyle(ChatFormatting.WHITE));
            lines.add(tr("cell.types", basic.getStoredItemTypes(), basic.getTotalItemTypes()).withStyle(ChatFormatting.WHITE));
        }
        KeyCounter contents = new KeyCounter();
        cell.getAvailableStacks(contents);
        if (contents.isEmpty()) {
            lines.add(tr("cell.empty").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        List<Object2LongMap.Entry<AEKey>> sorted = new ArrayList<>();
        contents.forEach(sorted::add);
        sorted.sort(Comparator.comparingLong(Object2LongMap.Entry<AEKey>::getLongValue).reversed());
        for (Object2LongMap.Entry<AEKey> e : sorted.subList(0, Math.min(SHOWN_CONTENTS, sorted.size()))) {
            lines.add(Component.literal("  " + e.getKey().formatAmount(e.getLongValue(), AmountFormat.FULL) + " x ")
                    .append(e.getKey().getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        if (sorted.size() > SHOWN_CONTENTS) {
            lines.add(tr("cell.more", sorted.size() - SHOWN_CONTENTS).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    /** A ten-block fill bar, green to red. */
    private static Component bar(long used, long total) {
        double fill = total <= 0 ? 0 : (double) used / total;
        int lit = (int) Math.round(fill * 10);
        ChatFormatting color = fill < 0.75 ? ChatFormatting.GREEN : fill < 1 ? ChatFormatting.GOLD : ChatFormatting.RED;
        return Component.literal("|".repeat(lit)).withStyle(color)
                .append(Component.literal("|".repeat(10 - lit)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
