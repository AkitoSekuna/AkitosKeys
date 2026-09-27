package com.akito_sekuna.keys.managers;

import com.akito_sekuna.keys.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the fixed/structural config keys (reward command, messages, shop
 * item slots/prices/lore, shop and promo messages). Dynamic per-key sections
 * (promos.*, collections.*) are intentionally NOT individually wrapped here,
 * consuming code reads them directly via {@link #getRaw()}, matching
 * AkitosDrugs' SettingsManager precedent for the same kind of admin-defined,
 * open-ended config shape.
 *
 * Reads from plugin.getCustomDataFolder(), not plugin.getConfig(), which is
 * permanently tied to Bukkit's standard getDataFolder() and can't be
 * redirected, matching AkitosLobby's ConfigManager pattern.
 */
public class ConfigManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getCustomDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getRaw() {
        return config;
    }

    // ========================================
    // WEEKLY KEY
    // ========================================

    public String getRewardCommand() {
        return config.getString("reward-command", "dc givekey {player} weekly 1");
    }

    public Component getReceivedMessage() {
        return legacy(config.getString("received", "&aYou received your free &eWeekly Key&a!"));
    }

    public Component getCooldownMessage(String timeLeft) {
        String raw = config.getString("cooldown",
                "&cYou already claimed your weekly key! Come back in &e{time}&c.");
        return legacy(raw.replace("{time}", timeLeft));
    }

    public Component getPlayersOnlyMessage() {
        return legacy(config.getString("players-only", "&cOnly players can use this command."));
    }

    public Component getErrorMessage() {
        return legacy(config.getString("error", "&cAn error occurred. Contact staff."));
    }

    // ========================================
    // KEY SHOP
    // ========================================

    public Component getShopTitle() {
        return legacy(config.getString("shop.title", "&8&lKey Shop"));
    }

    public int getShopItemSlot(String key, int def) {
        return config.getInt("shop." + key + ".slot", def);
    }

    public String getShopItemMaterial(String key, String def) {
        return config.getString("shop." + key + ".material", def);
    }

    public double getShopItemPrice(String key, double def) {
        return config.getDouble("shop." + key + ".price", def);
    }

    public String getShopItemCommand(String key, String def) {
        return config.getString("shop." + key + ".command", def);
    }

    public Component getShopItemDisplayName(String key, String def) {
        return legacy(config.getString("shop." + key + ".display-name", def));
    }

    public List<Component> getShopItemLore(String key, String priceFormatted) {
        List<Component> lore = new ArrayList<>();
        for (String line : config.getStringList("shop." + key + ".lore")) {
            lore.add(legacy(line.replace("{price}", priceFormatted)));
        }
        return lore;
    }

    public Component getShopMessage(String key, String def) {
        return legacy(config.getString("messages." + key, def));
    }

    // ========================================
    // PROMO
    // ========================================

    public Component getPromoMessage(String key, String def) {
        return legacy(config.getString("promo-messages." + key, def));
    }

    // ========================================
    // TEXT HELPERS
    // ========================================

    public Component legacy(String raw) {
        if (raw == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }
}
