package dev.ae2cinematic;

import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * AE2's charger has no window, but a Cinematic scene needs a menu to work through: this one holds just the charger's slot
 * (and the player's inventory, for taking things in and out). It is only ever opened by the scene.
 */
public final class ChargerMenu extends AEBaseMenu {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Ae2Cinematic.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ChargerMenu>> TYPE = MENUS.register("charger",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new ChargerMenu(id, inventory, charger(inventory, buf))));

    public ChargerMenu(int id, Inventory inventory, ChargerBlockEntity charger) {
        super(TYPE.get(), id, inventory, charger);
        addSlot(new AppEngSlot(charger.getInternalInventory(), 0), SlotSemantics.MACHINE_INPUT);
        createPlayerInventorySlots(inventory);
    }

    private static ChargerBlockEntity charger(Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            return charger;
        }
        throw new IllegalStateException("No AE2 charger at " + pos);
    }
}
