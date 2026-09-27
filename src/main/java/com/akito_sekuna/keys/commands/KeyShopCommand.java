package com.akito_sekuna.keys.commands;

import com.akito_sekuna.keys.Main;
import com.akito_sekuna.keys.listeners.KeyShopListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KeyShopCommand implements CommandExecutor {

    private final Main plugin;

    public KeyShopCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPlayersOnlyMessage());
            return true;
        }

        KeyShopListener.open(plugin, player);
        return true;
    }
}
