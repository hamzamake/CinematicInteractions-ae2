package dev.ae2cinematic.client;

import appeng.menu.SlotSemantics;
import appeng.menu.implementations.IOPortMenu;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.implementations.VibrationChamberMenu;
import dev.ae2cinematic.Ae2Cinematic;
import dev.ae2cinematic.Stations;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.NeoForge;

public final class Ae2CinematicClient {
    private Ae2CinematicClient() {
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Ae2Cinematic.MODID, name);
    }

    public static void init() {
        AddonScenes.register(id("drive"), (pos, state, player, side) -> new DriveStation(pos, side, state));
        AddonScenes.register(id("wireless_access_point"), (pos, state, player, side) -> new WapStation(pos, side, state));
        // face the terminal that was actually clicked, not just the nearest side of the cable bus
        AddonScenes.register(id("pattern_encoding_terminal"),
                (pos, state, player, side) -> new EncoderStation(pos, Stations.ENCODER_SIDE.getOrDefault(pos, side)));

        AddonScenes.register(id("inscriber"), (pos, state, player, side) -> new SimpleStation<>(pos, side, InscriberMenu.class, "inscriber",
                (s, menu) -> {
                    List<SlotStation.Spot> spots = new ArrayList<>();
                    add(spots, s, SlotStation.first(menu, SlotSemantics.INSCRIBER_PLATE_TOP), -0.16, 0.22, false, "top_press");
                    add(spots, s, SlotStation.first(menu, SlotSemantics.MACHINE_INPUT), -0.16, -0.03, false, "input");
                    add(spots, s, SlotStation.first(menu, SlotSemantics.INSCRIBER_PLATE_BOTTOM), -0.16, -0.28, false, "bottom_press");
                    add(spots, s, SlotStation.first(menu, SlotSemantics.MACHINE_OUTPUT), 0.2, -0.03, false, "output");
                    return spots;
                }));
        AddonScenes.register(id("io_port"), (pos, state, player, side) -> new SimpleStation<>(pos, side, IOPortMenu.class, "io_port",
                (s, menu) -> {
                    List<SlotStation.Spot> spots = new ArrayList<>();
                    s.grid(spots, menu.getSlots(SlotSemantics.MACHINE_INPUT), 6, 2, -0.36, 0.2, 0.14, 0.055, true, "input");
                    s.grid(spots, menu.getSlots(SlotSemantics.MACHINE_OUTPUT), 6, 2, 0.04, 0.2, 0.14, 0.055, false, "output");
                    return spots;
                }));
        AddonScenes.register(id("vibration_chamber"), (pos, state, player, side) -> new SimpleStation<>(pos, side, VibrationChamberMenu.class,
                "vibration_chamber", (s, menu) -> {
                    List<SlotStation.Spot> spots = new ArrayList<>();
                    add(spots, s, SlotStation.first(menu, SlotSemantics.MACHINE_INPUT), 0, -0.12, false, "fuel");
                    return spots;
                }));
        AddonScenes.register(id("condenser"), (pos, state, player, side) -> new CondenserStation(pos, side));
        AddonScenes.register(id("molecular_assembler"), (pos, state, player, side) -> new SimpleStation<>(pos, side,
                MolecularAssemblerMenu.class, "molecular_assembler", (s, menu) -> {
                    List<SlotStation.Spot> spots = new ArrayList<>();
                    add(spots, s, SlotStation.first(menu, SlotSemantics.ENCODED_PATTERN), 0, 0, true, "pattern");
                    return spots;
                }));
        AddonScenes.register(id("pattern_provider"), (pos, state, player, side) -> new SimpleStation<>(pos, side, PatternProviderMenu.class,
                "pattern_provider", (s, menu) -> {
                    List<SlotStation.Spot> spots = new ArrayList<>();
                    s.grid(spots, menu.getSlots(SlotSemantics.ENCODED_PATTERN), 9, 3, -0.22, 0.2, 0.22, 0.07, false, "patterns");
                    return spots;
                }));

        NeoForge.EVENT_BUS.register(ChargerHand.class);
    }

    static void add(List<SlotStation.Spot> spots, SimpleStation<?> s, Slot slot, double u, double v, boolean drag, String label) {
        if (slot != null) {
            spots.add(SlotStation.Spot.labeled(slot, s.point(u, v), 0.07, 0.07, drag, label));
        }
    }
}
