package dev.ae2cinematic;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.CondenserMenu;
import appeng.menu.implementations.DriveMenu;
import appeng.menu.implementations.IOPortMenu;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.implementations.VibrationChamberMenu;
import appeng.menu.implementations.WirelessAccessPointMenu;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.AEBasePart;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Every AE2 block that gets a Cinematic scene, and how the server opens its AE2 menu. */
public final class Stations {
    public record Entry(String name, String block, Class<? extends AbstractContainerMenu> menu,
                        Function<BlockState, List<Direction>> sides, BiPredicate<ServerPlayer, BlockPos> opener) {
        public ResourceLocation id() {
            return new ResourceLocation(Ae2Cinematic.MODID, name);
        }

        public ResourceLocation blockId() {
            return new ResourceLocation("ae2", block);
        }
    }

    static final List<Direction> AROUND = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    static final ResourceLocation CABLE_BUS = new ResourceLocation("ae2", "cable_bus");

    /** The side of the Pattern Encoding Terminal last clicked on each cable bus (client and server each fill their own clicks). */
    public static final Map<BlockPos, Direction> ENCODER_SIDE = new ConcurrentHashMap<>();

    // The menu types are read lazily (Supplier) so AE2's menu classes are not initialised while mods are still constructing.
    public static final List<Entry> ALL = List.of(
            new Entry("drive", "drive", DriveMenu.class, Stations::front, block(() -> DriveMenu.TYPE)),
            new Entry("wireless_access_point", "wireless_access_point", WirelessAccessPointMenu.class, s -> AROUND,
                    block(() -> WirelessAccessPointMenu.TYPE)),
            new Entry("pattern_encoding_terminal", "cable_bus", PatternEncodingTermMenu.class, s -> AROUND, Stations::openEncoder),
            new Entry("inscriber", "inscriber", InscriberMenu.class, Stations::front, block(() -> InscriberMenu.TYPE)),
            new Entry("io_port", "io_port", IOPortMenu.class, s -> AROUND, block(() -> IOPortMenu.TYPE)),
            new Entry("vibration_chamber", "vibration_chamber", VibrationChamberMenu.class, Stations::front,
                    block(() -> VibrationChamberMenu.TYPE)),
            new Entry("condenser", "condenser", CondenserMenu.class, s -> AROUND, block(() -> CondenserMenu.TYPE)),
            new Entry("molecular_assembler", "molecular_assembler", MolecularAssemblerMenu.class, s -> AROUND,
                    block(() -> MolecularAssemblerMenu.TYPE)),
            new Entry("pattern_provider", "pattern_provider", PatternProviderMenu.class, s -> AROUND,
                    block(() -> PatternProviderMenu.TYPE)),
            new Entry("charger", "charger", ChargerMenu.class, Stations::front, Stations::openCharger));

    private static final Map<ResourceLocation, Entry> BY_BLOCK = ALL.stream().collect(Collectors.toMap(Entry::blockId, e -> e));

    private Stations() {
    }

    /** Blocks worked from their front only; one turned to face up or down falls back to AE2's own window. */
    static List<Direction> front(BlockState state) {
        Direction front = BlockOrientation.get(state).getSide(RelativeSide.FRONT);
        return front.getAxis().isHorizontal() ? List.of(front) : List.of();
    }

    private static BiPredicate<ServerPlayer, BlockPos> block(Supplier<? extends MenuType<? extends AEBaseMenu>> type) {
        return (player, pos) -> {
            BlockEntity be = player.level().getBlockEntity(pos);
            return be != null && MenuOpener.open(type.get(), player, MenuLocators.forBlockEntity(be));
        };
    }

    private static boolean openCharger(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof ChargerBlockEntity charger)) {
            return false;
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new ChargerMenu(id, inventory, charger), Component.empty()),
                buf -> buf.writeBlockPos(pos));
        return player.containerMenu instanceof ChargerMenu;
    }

    private static boolean openEncoder(ServerPlayer player, BlockPos pos) {
        IPart part = player.level().getBlockEntity(pos) instanceof IPartHost host ? encoderPart(host, ENCODER_SIDE.get(pos)) : null;
        return part instanceof AEBasePart base && MenuOpener.open(PatternEncodingTermMenu.TYPE, player, MenuLocators.forPart(base));
    }

    /** The terminal on {@code prefer} if there is one there, else any terminal facing sideways. */
    public static IPart encoderPart(IPartHost host, Direction prefer) {
        if (prefer != null && host.getPart(prefer) instanceof PatternEncodingTerminalPart part) {
            return part;
        }
        for (Direction d : AROUND) {
            if (host.getPart(d) instanceof PatternEncodingTerminalPart part) {
                return part;
            }
        }
        return null;
    }

    /** Server side: an AE2-aware stand-in for the block's menu provider, or null for blocks that aren't ours. */
    public static MenuProvider provider(BlockState state, BlockPos pos) {
        Entry entry = BY_BLOCK.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return entry == null ? null : new Ae2Provider(entry, pos.immutable());
    }

    /**
     * A cable bus holds many parts, but only a sideways Pattern Encoding Terminal is a station: a click on anything else
     * on the bus is left to AE2.
     */
    public static boolean allowsScene(Level level, BlockPos pos, Vec3 hit) {
        if (!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(CABLE_BUS)) {
            return true;
        }
        if (!(level.getBlockEntity(pos) instanceof IPartHost host)) {
            return false;
        }
        SelectedPart selected = host.selectPartWorld(hit);
        if (!(selected.part instanceof PatternEncodingTerminalPart) || selected.side == null
                || !selected.side.getAxis().isHorizontal()) {
            return false;
        }
        ENCODER_SIDE.put(pos.immutable(), selected.side);
        return true;
    }
}
