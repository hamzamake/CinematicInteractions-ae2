package dev.ae2cinematic;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * What Cinematic gets in place of a block menu provider for AE2 blocks. AE2 menus need host data sent along with them,
 * which a plain {@code openMenu(provider)} can't write, so {@code ServerPlayerMixin} routes the open to AE2 instead.
 */
public record Ae2Provider(Stations.Entry entry, BlockPos pos) implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    public OptionalInt open(ServerPlayer player) {
        return entry.opener().test(player, pos) ? OptionalInt.of(player.containerMenu.containerId) : OptionalInt.empty();
    }
}
