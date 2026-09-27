package com.akito_sekuna.keys.listeners;

import com.akito_sekuna.keys.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyShopListener implements Listener {

    private static final long PURCHASE_DELAY = 1000L;

    private static final Map<UUID, Long> purchaseCooldowns =
            new ConcurrentHashMap<>();

    private final Main plugin;

    public KeyShopListener(Main plugin) {
        this.plugin = plugin;
    }


    // ========================================
    // OPEN SHOP
    // ========================================

    public static void open(Main plugin, Player player) {

        ConfigurationSection shop =
                plugin.getConfigManager().getRaw().getConfigurationSection("shop");

        if (shop == null) {
            player.sendMessage(
                    plugin.getConfigManager().legacy(
                            "&cОшибка: секция магазина не найдена!"
                    )
            );
            return;
        }

        int size = shop.getInt("size", 27);

        // Inventory size must be divisible by 9
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 27;
        }

        String title = shop.getString(
                "title",
                "&8Магазин ключей"
        );

        Inventory inventory = Bukkit.createInventory(
                (InventoryHolder) null,
                size,
                plugin.getConfigManager().legacy(title)
        );


        // ========================================
        // DECORATIONS (non-purchasable filler items)
        // ========================================

        ConfigurationSection decorationsSection =
                shop.getConfigurationSection("decorations");

        if (decorationsSection != null) {

            for (String decorId : decorationsSection.getKeys(false)) {

                ConfigurationSection decor =
                        decorationsSection.getConfigurationSection(decorId);

                if (decor == null) {
                    continue;
                }

                List<Integer> slots = readSlots(decor);

                ItemStack stack = buildDecorItem(decor, decorId, plugin);

                if (stack == null) {
                    continue;
                }

                for (int slot : slots) {

                    if (slot < 0 || slot >= size) {
                        plugin.getLogger().warning(
                                "Invalid slot for shop decoration '" +
                                        decorId + "': " + slot
                        );
                        continue;
                    }

                    inventory.setItem(slot, stack.clone());
                }
            }
        }


        // ========================================
        // LOAD ALL SHOP ITEMS
        // ========================================

        ConfigurationSection itemsSection =
                shop.getConfigurationSection("items");

        if (itemsSection != null) {

            for (String itemId : itemsSection.getKeys(false)) {

                ConfigurationSection item =
                        itemsSection.getConfigurationSection(itemId);

                if (item == null) {
                    continue;
                }

                int slot = item.getInt("slot", -1);

                if (slot < 0 || slot >= size) {
                    plugin.getLogger().warning(
                            "Invalid slot for shop item '" +
                                    itemId + "': " + slot
                    );
                    continue;
                }

                String materialName = item.getString(
                        "material",
                        "STONE"
                );

                Material material;

                try {
                    material = Material.valueOf(
                            materialName.toUpperCase()
                    );
                } catch (IllegalArgumentException exception) {

                    plugin.getLogger().warning(
                            "Invalid material '" +
                                    materialName +
                                    "' for shop item '" +
                                    itemId + "'"
                    );

                    material = Material.STONE;
                }


                String displayName = item.getString(
                        "display-name",
                        "&f" + itemId
                );

                double price = item.getDouble(
                        "price",
                        0.0
                );

                List<String> loreLines =
                        item.getStringList("lore");

                List<Component> lore = new ArrayList<>();

                for (String line : loreLines) {

                    line = line.replace(
                            "{price}",
                            formatMoney(price)
                    );

                    lore.add(
                            plugin.getConfigManager().legacy(line)
                    );
                }


                ItemStack stack =
                        new ItemStack(material);

                ItemMeta meta =
                        stack.getItemMeta();

                if (meta != null) {

                    meta.displayName(
                            plugin.getConfigManager().legacy(
                                    displayName
                            )
                    );

                    meta.lore(lore);

                    stack.setItemMeta(meta);
                }

                inventory.setItem(slot, stack);
            }
        }

        player.openInventory(inventory);
    }

    private static List<Integer> readSlots(ConfigurationSection decor) {

        List<Integer> slots = new ArrayList<>();

        if (decor.contains("slots")) {

            for (Object entry : decor.getList("slots")) {

                if (entry instanceof Number number) {
                    slots.add(number.intValue());
                    continue;
                }

                if (entry instanceof String string) {
                    try {
                        slots.add(Integer.parseInt(string));
                    } catch (NumberFormatException ignored) {
                        // skip malformed entry
                    }
                }
            }

        } else if (decor.contains("slot")) {
            slots.add(decor.getInt("slot", -1));
        }

        return slots;
    }

    private static ItemStack buildDecorItem(ConfigurationSection decor, String decorId, Main plugin) {

        String materialName = decor.getString("material", "STONE");
        Material material;

        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning(
                    "Invalid material '" + materialName + "' for shop decoration '" + decorId + "'"
            );
            material = Material.STONE;
        }

        String displayName = decor.getString("display-name", "&f" + decorId);

        List<Component> lore = new ArrayList<>();
        for (String line : decor.getStringList("lore")) {
            lore.add(plugin.getConfigManager().legacy(line));
        }

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.displayName(plugin.getConfigManager().legacy(displayName));

        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        stack.setItemMeta(meta);
        return stack;
    }


    // ========================================
    // HANDLE PURCHASE
    // ========================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Component configuredTitle =
                plugin.getConfigManager().legacy(
                        plugin.getConfigManager().getRaw().getString(
                                "shop.title",
                                "&8Магазин ключей"
                        )
                );

        if (!event.getView().title().equals(configuredTitle)) {
            return;
        }

        // Cancel unconditionally the moment we know this is our GUI, before
        // looking at which specific inventory was clicked. Gating the
        // cancellation itself behind getClickedInventory() (as the previous
        // version did) leaves double-click "collect to cursor" unblocked,
        // since that action's originating click reports the player's own
        // inventory as clicked even though it pulls matching items from the
        // shop GUI too. Which inventory was clicked still matters below, but
        // only for deciding whether to treat this as a purchase attempt, not
        // for deciding whether to cancel.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (!event.getClickedInventory().equals(
                event.getView().getTopInventory()
        )) {
            return;
        }


        // ========================================
        // FIND CONFIG ITEM BY SLOT
        // ========================================

        int clickedSlot = event.getRawSlot();

        ConfigurationSection itemsSection =
                plugin.getConfigManager().getRaw().getConfigurationSection(
                        "shop.items"
                );

        if (itemsSection == null) {
            return;
        }

        String selectedItemId = null;
        ConfigurationSection selectedItem = null;


        for (String itemId : itemsSection.getKeys(false)) {

            ConfigurationSection item =
                    itemsSection.getConfigurationSection(itemId);

            if (item == null) {
                continue;
            }

            int configuredSlot =
                    item.getInt("slot", -1);

            if (configuredSlot == clickedSlot) {

                selectedItemId = itemId;
                selectedItem = item;
                break;
            }
        }


        // Clicked a decoration slot, or an empty slot, not a purchasable item
        if (selectedItemId == null || selectedItem == null) {
            return;
        }


        // ========================================
        // PURCHASE COOLDOWN
        // ========================================

        UUID uuid = player.getUniqueId();

        long now = System.currentTimeMillis();

        Long lastPurchase =
                purchaseCooldowns.get(uuid);

        if (lastPurchase != null &&
                now - lastPurchase < PURCHASE_DELAY) {
            return;
        }

        purchaseCooldowns.put(uuid, now);


        // ========================================
        // PRICE
        // ========================================

        double price =
                selectedItem.getDouble("price", 0.0);

        if (price < 0) {
            price = 0;
        }


        // ========================================
        // CHECK MONEY
        // ========================================

        if (!plugin.getCoreAPI()
                .getEconomy()
                .has(uuid, price)) {

            String message =
                    plugin.getConfigManager().getRaw().getString(
                            "messages.not-enough-money",
                            "&cВам нужно &e{price} PX &cдля покупки!"
                    );

            message = message.replace(
                    "{price}",
                    formatMoney(price)
            );

            player.sendMessage(
                    plugin.getConfigManager().legacy(message)
            );

            return;
        }


        // ========================================
        // TAKE MONEY
        // ========================================

        boolean taken =
                plugin.getCoreAPI()
                        .getEconomy()
                        .take(uuid, price);

        if (!taken) {

            player.sendMessage(
                    plugin.getConfigManager().getShopMessage(
                            "error",
                            "&cПроизошла ошибка при покупке!"
                    )
            );

            return;
        }


        // ========================================
        // EXECUTE CONFIG COMMAND
        // ========================================

        String command =
                selectedItem.getString("command", "");

        if (command.isBlank()) {

            // Refund if configuration is broken
            plugin.getCoreAPI()
                    .getEconomy()
                    .give(uuid, price);

            player.sendMessage(
                    plugin.getConfigManager().getShopMessage(
                            "error",
                            "&cПроизошла ошибка при покупке!"
                    )
            );

            return;
        }


        command = command.replace(
                "{player}",
                player.getName()
        );


        boolean success =
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command
                );


        // ========================================
        // REFUND ON FAILURE
        // ========================================

        if (!success) {

            plugin.getCoreAPI()
                    .getEconomy()
                    .give(uuid, price);

            player.sendMessage(
                    plugin.getConfigManager().getShopMessage(
                            "error",
                            "&cПроизошла ошибка при выдаче ключа!"
                    )
            );

            return;
        }


        // ========================================
        // SUCCESS MESSAGE
        // ========================================

        String message =
                plugin.getConfigManager().getRaw().getString(
                        "messages.purchased",
                        "&aВы купили ключ за &e{price} PX&a!"
                );

        message = message.replace(
                "{price}",
                formatMoney(price)
        );

        player.sendMessage(
                plugin.getConfigManager().legacy(message)
        );
    }


    // ========================================
    // BLOCK DRAGGING ITEMS IN/OUT OF THE SHOP
    // ========================================

    // InventoryClickEvent and InventoryDragEvent are separate events. Now that
    // decorations can fill many real, non-air slots in this GUI, dragging
    // items into or out of it is a second way to move things around unless
    // drags are also cancelled whenever the shop is the top inventory, same
    // class of hole the click-cancellation fix above closes for clicks.
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        Component configuredTitle =
                plugin.getConfigManager().legacy(
                        plugin.getConfigManager().getRaw().getString(
                                "shop.title",
                                "&8Магазин ключей"
                        )
                );

        if (!event.getView().title().equals(configuredTitle)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }


    // ========================================
    // FORMAT MONEY
    // ========================================

    private static String formatMoney(double amount) {

        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }
}
