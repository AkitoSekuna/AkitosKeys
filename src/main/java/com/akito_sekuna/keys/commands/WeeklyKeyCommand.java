package com.akito_sekuna.keys.commands;

import com.akito_sekuna.keys.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WeeklyKeyCommand implements CommandExecutor {

    private static final long WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final Main plugin;

    public WeeklyKeyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPlayersOnlyMessage());
            return true;
        }

        long lastClaim = plugin.getDataManager().getCooldown(player.getUniqueId());
        long now = System.currentTimeMillis();

        if (lastClaim > 0L) {
            long elapsed = now - lastClaim;
            if (elapsed < WEEK_MILLIS) {
                long remaining = WEEK_MILLIS - elapsed;
                player.sendMessage(plugin.getConfigManager().getCooldownMessage(formatTime(remaining)));
                return true;
            }
        }

        boolean success = plugin.getKeyManager().giveKey(player, plugin.getConfigManager().getRewardCommand());
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getErrorMessage());
            return true;
        }

        plugin.getDataManager().setCooldown(player.getUniqueId(), now);
        player.sendMessage(plugin.getConfigManager().getReceivedMessage());
        return true;
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        return days + "d " + hours + "h " + minutes + "m";
    }
}
