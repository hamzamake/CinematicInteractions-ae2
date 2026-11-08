package dev.ae2cinematic;

import dev.ae2cinematic.client.Ae2CinematicClient;
import dev.dyslexic.cinematic.api.AddonStations;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Ae2Cinematic.MODID)
public final class Ae2Cinematic {
    public static final String MODID = "ae2cinematic";

    public Ae2Cinematic() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ChargerMenu.MENUS.register(modBus);
        for (Stations.Entry e : Stations.ALL) {
            AddonStations.register(new AddonStations.Definition(e.id(), e.blockId(), e.menu(), e.sides()));
        }
        if (FMLEnvironment.dist.isClient()) {
            Ae2CinematicClient.init(modBus);
        }
    }
}
