package com.akito_sekuna.keys.managers;

import com.akito_sekuna.keys.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Centralizes the "{player}-template console command" dispatch that used to
 * be duplicated across the weekly key handler, the shop, and promo command
 * rewards. Kept flexible, takes a resolved command template string rather
 * than hardcoding the DonateCase command format, so admins stay free to
 * change reward-command/shop item commands/promo commands in config.yml
 * without needing a code change.
 */
public class KeyManager {

    private final Main plugin;

    public KeyManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean giveKey(Player player, String commandTemplate) {
        if (commandTemplate == null || commandTemplate.isBlank()) {
            plugin.getLogger().warning("Attempted to dispatch a blank command template for " + player.getName());
            return false;
        }

        String command = commandTemplate.replace("{player}", player.getName());
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
