package com.akito_sekuna.keys.managers;

import com.akito_sekuna.keys.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Wraps data.yml: per-player weekly key cooldowns, promo claims, and
 * completed collections. Cooldowns live here rather than in config.yml,
 * config is admin-editable settings, data.yml is the plugin's own runtime
 * state, mixing the two (as the old flat-package version did) risks an
 * admin's config edit wiping player progress.
 */
public class DataManager {

    private final Main plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getCustomDataFolder(), "data.yml");
        load();
    }

    private void load() {
        // Main.loadCustomConfig() already copies the bundled data.yml into the custom
        // folder before any manager is constructed, so no saveResource() call here,
        // that would target Bukkit's standard getDataFolder() instead.
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (!dataConfig.contains("cooldowns")) dataConfig.createSection("cooldowns");
        if (!dataConfig.contains("promo-claims")) dataConfig.createSection("promo-claims");
        if (!dataConfig.contains("collections-completed")) dataConfig.createSection("collections-completed");

        save();
    }

    public void save() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    // ========================================
    // WEEKLY KEY COOLDOWN
    // ========================================

    public long getCooldown(UUID uuid) {
        return dataConfig.getLong("cooldowns." + uuid, 0L);
    }

    public void setCooldown(UUID uuid, long timestamp) {
        dataConfig.set("cooldowns." + uuid, timestamp);
        save();
    }

    // ========================================
    // PROMO CLAIMS
    // ========================================

    public List<String> getPromoClaims(UUID uuid) {
        return dataConfig.getStringList("promo-claims." + uuid);
    }

    public boolean hasClaimedPromo(UUID uuid, String code) {
        return getPromoClaims(uuid).contains(code);
    }

    public void addPromoClaim(UUID uuid, String code) {
        List<String> claims = getPromoClaims(uuid);
        claims.add(code);
        dataConfig.set("promo-claims." + uuid, claims);
        save();
    }

    // ========================================
    // COLLECTIONS
    // ========================================

    public boolean isCollectionCompleted(UUID uuid, String collectionId) {
        return dataConfig.getBoolean("collections-completed." + uuid + "." + collectionId, false);
    }

    public void setCollectionCompleted(UUID uuid, String collectionId) {
        dataConfig.set("collections-completed." + uuid + "." + collectionId, true);
        save();
    }
}
