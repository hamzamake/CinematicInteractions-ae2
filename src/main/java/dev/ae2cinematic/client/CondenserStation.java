package dev.ae2cinematic.client;

import appeng.api.config.CondenserOutput;
import appeng.api.config.Settings;
import appeng.core.sync.packets.ConfigButtonPacket;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.CondenserMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import appeng.core.sync.network.NetworkHandler;

/**
 * Matter Condenser: items in the top socket are destroyed for power, the button picks what that power makes (AE2 starts on
 * Trash, which makes nothing), and a storage component in the bay is what lets it hold power at all.
 */
final class CondenserStation extends SimpleStation<CondenserMenu> {
    private static final double MODE_V = -0.38;

    CondenserStation(BlockPos pos, Direction side) {
        super(pos, side, CondenserMenu.class, "condenser", (s, menu) -> {
            List<Spot> spots = new ArrayList<>();
            Ae2CinematicClient.add(spots, s, first(menu, SlotSemantics.MACHINE_INPUT), 0, 0.18, false, "input");
            Ae2CinematicClient.add(spots, s, first(menu, SlotSemantics.STORAGE_CELL), -0.22, -0.2, true, "component");
            Ae2CinematicClient.add(spots, s, first(menu, SlotSemantics.MACHINE_OUTPUT), 0.22, -0.2, false, "output");
            spots.add(Spot.button("mode", s.point(0, MODE_V), 0.12, 0.034));
            return spots;
        });
    }

    private static Component mode(CondenserOutput output) {
        return tr("condenser.mode." + output.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void press(String button) {
        NetworkHandler.instance().sendToServer(new ConfigButtonPacket(Settings.CONDENSER_OUTPUT, false));
        kick(0.15);
        sound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.5f, 1.2f);
    }

    @Override
    protected List<Component> buttonTooltip(String button) {
        return List.of(tr("condenser.making", mode(menu().getOutput())), tr("button.mode").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void drawExtras(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
        super.drawExtras(ps, buffers, cam, light);
        CondenserOutput output = menu().getOutput();
        int color = switch (output) {
            case TRASH -> 0xFF8A4A4A;
            case MATTER_BALLS -> 0xFF3FA9C9;
            case SINGULARITY -> 0xFF8E5BD6;
        };
        panel(ps, buffers, cam, 0, MODE_V, 0.12, 0.034, 0.008, color);
        label(ps, buffers, cam, mode(output), at(0, MODE_V, 0.01), 0xFFFFFFFF, 0xF000F0);
    }

    @Override
    public void renderScreen(GuiGraphics gui, Font font, float partial) {
        CondenserMenu menu = menu();
        if (menu == null) {
            return;
        }
        Slot component = first(menu, SlotSemantics.STORAGE_CELL);
        // AE2 can't hold any power without a component, so everything put in is destroyed for nothing
        Component text = menu.getOutput() == CondenserOutput.TRASH ? tr("condenser.voiding")
                : component != null && !component.hasItem() ? tr("condenser.no_component")
                : tr("condenser.status", menu.getCurrentProgress(), menu.getMaxProgress());
        gui.drawCenteredString(font, text, gui.guiWidth() / 2, 42, 0xFFBFEFFF); // below Cinematic's hotbar strip
    }
}
