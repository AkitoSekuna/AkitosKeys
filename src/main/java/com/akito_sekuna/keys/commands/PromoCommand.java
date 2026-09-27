package com.akito_sekuna.keys.commands;

import com.akito_sekuna.keys.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class PromoCommand implements CommandExecutor {

    private final Main plugin;

    public PromoCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPromoMessage("players-only", "&cOnly players can use this command."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("usage", "&cUsage: /promo <code>"));
            return true;
        }

        String code = args[0].toUpperCase();
        String path = "promos." + code;
        var config = plugin.getConfigManager().getRaw();

        if (!config.contains(path)) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("invalid", "&cThat promo code does not exist!"));
            return true;
        }

        if (!config.getBoolean(path + ".enabled", true)) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("disabled", "&cThis promo code is no longer active!"));
            return true;
        }

        String expires = config.getString(path + ".expires", "");
        if (expires != null && !expires.isBlank()) {
            try {
                LocalDate expirationDate = LocalDate.parse(expires);
                if (LocalDate.now().isAfter(expirationDate)) {
                    player.sendMessage(plugin.getConfigManager().getPromoMessage("expired", "&cThis promo code has expired!"));
                    return true;
                }
            } catch (DateTimeParseException exception) {
                plugin.getLogger().warning("Invalid expiration date for promo " + code + ": " + expires);
            }
        }

        int maxUses = config.getInt(path + ".max-uses", 0);
        int currentUses = config.getInt(path + ".uses", 0);
        if (maxUses > 0 && currentUses >= maxUses) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("no-uses", "&cThis promo code has already been fully used!"));
            return true;
        }

        if (plugin.getDataManager().hasClaimedPromo(player.getUniqueId(), code)) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("already-claimed", "&cYou already used this promo code!"));
            return true;
        }

        String rewardType = config.getString(path + ".reward-type", "").toUpperCase();
        boolean success = false;

        switch (rewardType) {
            case "PX" -> {
                double amount = config.getDouble(path + ".amount", 0);
                plugin.getCoreAPI().getEconomy().give(player.getUniqueId(), amount);
                success = true;
            }
            case "COMMAND" -> {
                String rewardCommand = config.getString(path + ".command", "");
                if (!rewardCommand.isBlank()) {
                    success = plugin.getKeyManager().giveKey(player, rewardCommand);
                }
            }
            default -> plugin.getLogger().warning("Invalid reward type for promo " + code + ": " + rewardType);
        }

        if (!success) {
            player.sendMessage(plugin.getConfigManager().getPromoMessage("error", "&cAn error occurred using this promo code!"));
            return true;
        }

        plugin.getDataManager().addPromoClaim(player.getUniqueId(), code);

        config.set(path + ".uses", currentUses + 1);
        plugin.getConfigManager().save();

        String successMessage = config.getString("promo-messages.success", "&aPromo code &e{code} &asuccessfully activated!");
        successMessage = successMessage.replace("{code}", code);

        if (rewardType.equals("PX")) {
            double amount = config.getDouble(path + ".amount", 0);
            successMessage = successMessage.replace("{amount}", String.valueOf(amount));
        }

        player.sendMessage(plugin.getConfigManager().legacy(successMessage));
        return true;
    }
}
