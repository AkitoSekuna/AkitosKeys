package com.akito_sekuna.keys.managers;

import com.akito_sekuna.keys.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class CollectionManager {

    private final Main plugin;

    public CollectionManager(Main plugin) {
        this.plugin = plugin;
    }

    public void checkPlayer(Player player) {
        ConfigurationSection collections = plugin.getConfigManager().getRaw().getConfigurationSection("collections");
        if (collections == null) return;

        for (String collectionId : collections.getKeys(false)) {
            checkCollection(player, collectionId);
        }
    }

    private void checkCollection(Player player, String collectionId) {
        String path = "collections." + collectionId;
        var config = plugin.getConfigManager().getRaw();

        if (!config.getBoolean(path + ".enabled", true)) return;

        List<String> heads = config.getStringList(path + ".heads");
        if (heads.isEmpty()) return;

        for (String permission : heads) {
            if (!player.hasPermission(permission)) return;
        }

        boolean alreadyCompleted = plugin.getDataManager().isCollectionCompleted(player.getUniqueId(), collectionId);
        boolean repeatable = config.getBoolean(path + ".repeatable", false);

        if (alreadyCompleted && !repeatable) return;

        if (!giveReward(player, path)) return;

        if (!repeatable) {
            plugin.getDataManager().setCollectionCompleted(player.getUniqueId(), collectionId);
        }

        String rawMessage = config.getString(path + ".completion-message", "&aYou completed the &e{collection}&a collection!");
        String displayName = config.getString(path + ".display-name", collectionId);

        String resolved = rawMessage
                .replace("{collection}", displayName)
                .replace("{player}", player.getName());

        player.sendMessage(plugin.getConfigManager().legacy(resolved));
    }

    private boolean giveReward(Player player, String path) {
        var config = plugin.getConfigManager().getRaw();
        String rewardType = config.getString(path + ".reward.type", "").toUpperCase();

        switch (rewardType) {
            case "PX" -> {
                double amount = config.getDouble(path + ".reward.amount", 0.0);
                if (amount <= 0) {
                    plugin.getLogger().warning("Collection reward amount must be greater than 0: " + path);
                    return false;
                }
                plugin.getCoreAPI().getEconomy().give(player.getUniqueId(), amount);
                return true;
            }
            case "COMMAND" -> {
                String command = config.getString(path + ".reward.command", "");
                if (command.isBlank()) {
                    plugin.getLogger().warning("Collection command reward is empty: " + path);
                    return false;
                }
                return plugin.getKeyManager().giveKey(player, command);
            }
            default -> {
                plugin.getLogger().warning("Unknown collection reward type '" + rewardType + "' in " + path);
                return false;
            }
        }
    }

    public int getCollected(Player player, String collectionId) {
        List<String> heads = plugin.getConfigManager().getRaw().getStringList("collections." + collectionId + ".heads");
        int collected = 0;
        for (String permission : heads) {
            if (player.hasPermission(permission)) collected++;
        }
        return collected;
    }

    public int getTotal(String collectionId) {
        return plugin.getConfigManager().getRaw().getStringList("collections." + collectionId + ".heads").size();
    }
}
