package com.akito_sekuna.keys.commands;

import com.akito_sekuna.keys.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class WeeklyKeyAdminCommand implements CommandExecutor {

    private final Main plugin;

    public WeeklyKeyAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weeklykey.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reload();
            sender.sendMessage(Component.text("WeeklyKey configuration reloaded!").color(NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Usage: /weeklykey reload").color(NamedTextColor.YELLOW));
        return true;
    }
}
