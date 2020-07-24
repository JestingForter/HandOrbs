package me.qKing12.HandyOrbs.AnvilGUI;

import me.qKing12.HandyOrbs.utils.utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;


/**
 * An anvil gui, used for gathering a user's input
 * @author Wesley Smith
 * @since 1.0
 */
public class AnvilGUI {

    /**
     * The player who has the GUI open
     */
    private final Player holder;
    /**
     * The ItemStack that is in the {@link Slot#INPUT_LEFT} slot.
     */
    private final ItemStack insert;
    /**
     * Called when the player clicks the {@link Slot#OUTPUT} slot
     */
    private final ClickHandler clickHandler;

    /**
     * The {@link NmsHelper} for this server
     */
    private final NmsHelper nms;
    /**
     * The container id of the inventory, used for NMS methods
     */
    private final int containerId;
    /**
     * The inventory that is used on the Bukkit side of things
     */
    private final Inventory inventory;
    /**
     * The listener holder class
     */
    private final ListenUp listener = new ListenUp();

    private final ArmorStand am;

    /**
     * Represents the state of the inventory being open
     */
    private boolean open = false;

    public AnvilGUI(ArmorStand am, Plugin plugin, Player holder, ItemStack slot, ClickHandler clickHandler) {
        this.am=am;
        this.holder = holder;
        this.clickHandler = clickHandler;

        this.insert = slot;

        this.plugin=plugin;

        this.nms = NmsHelper.INSTANCE;

        nms.handleInventoryCloseEvent(holder);
        nms.setActiveContainerDefault(holder);

        Bukkit.getPluginManager().registerEvents(listener, plugin);

        containerId = nms.getNextContainerId(holder);
        final Object container = nms.newContainerAnvil(holder, containerId);

        inventory = nms.toBukkitInventory(container);
        inventory.setItem(Slot.INPUT_LEFT, this.insert);

        nms.sendPacketOpenWindow(holder, containerId, container);
        nms.setActiveContainer(holder, container);
        nms.setActiveContainerId(container, containerId);
        nms.addActiveContainerSlotListener(container, holder);

        open = true;
    }


    /**
     * Closes the inventory if it's open.
     * @throws IllegalArgumentException If the inventory isn't open
     */
    public void closeInventory(boolean fromEvent) {
        if(!open)
            throw new IllegalArgumentException("You can't close an inventory that isn't open!");
        open = false;

        if(!fromEvent)
            nms.handleInventoryCloseEvent(holder);
        nms.setActiveContainerDefault(holder);
        nms.sendPacketCloseWindow(holder, containerId);

        HandlerList.unregisterAll(listener);
    }

    private Plugin plugin;

    /**
     * Simply holds the listeners for the GUI
     */
    private class ListenUp implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(e.getInventory().equals(inventory)) {
                e.setCancelled(true);
                final Player clicker = (Player) e.getWhoClicked();
                if(e.getRawSlot() == Slot.OUTPUT) {
                    final ItemStack clicked = inventory.getItem(e.getRawSlot());
                    if(clicked == null || clicked.getType() == Material.AIR) return;
                    String reply = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : clicked.getType().toString();
                    if(reply!=null && reply.startsWith(" "))
                        reply=utils.chat(reply.substring(1));
                    final String ret = clickHandler.onClick(clicker, reply);
                    if(ret != null) {
                        final ItemMeta meta = clicked.getItemMeta();
                        meta.setDisplayName(utils.chat(ret));
                        clicked.setItemMeta(meta);
                        inventory.setItem(Slot.OUTPUT, clicked);
                        clicker.updateInventory();
                    } else closeInventory(false);
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            if(e.getInventory().equals(inventory)) {
                Player p = (Player)e.getPlayer();
                e.getInventory().clear();
                if(open) {
                    closeInventory(true);
                }
            }
        }
    }

    /**
     * Handles the click of the output slot
     */
    public interface ClickHandler{

        /**
         * Is called when a {@link Player} clicks on the output in the GUI
         * @param clicker The {@link Player} who clicked the output
         * @param input What the item was renamed to
         * @return What to replace the text with, or null to close the inventory
         */
        String onClick(Player clicker, String input);
    }

    /**
     * Class wrapping the magic constants of slot numbers in an anvil GUI
     */
    public static class Slot {

        /**
         * The slot on the far left, where the first input is inserted. An {@link ItemStack} is always inserted
         * here to be renamed
         */
        public static final int INPUT_LEFT = 0;
        /**
         * Not used, but in a real anvil you are able to put the second item you want to combine here
         */
        public static final int INPUT_RIGHT = 1;
        /**
         * The output slot, where an item is put when two items are combined from {@link #INPUT_LEFT} and
         * {@link #INPUT_RIGHT} or {@link #INPUT_LEFT} is renamed
         */
        public static final int OUTPUT = 2;

    }

}
