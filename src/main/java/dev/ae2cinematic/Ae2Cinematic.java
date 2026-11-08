package dev.ae2cinematic;

import dev.ae2cinematic.client.Ae2CinematicClient;
import dev.dyslexic.cinematic.api.AddonStations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Ae2Cinematic.MODID)
public final class Ae2Cinematic {
    public static final String MODID = "ae2cinematic";

    public Ae2Cinematic(IEventBus modBus, Dist dist) {
        ChargerMenu.MENUS.register(modBus);
        for (Stations.Entry e : Stations.ALL) {
            AddonStations.register(new AddonStations.Definition(e.id(), e.blockId(), e.menu(), e.sides()));
        }
        if (dist.isClient()) {
            Ae2CinematicClient.init(modBus);
        }
    }
}
