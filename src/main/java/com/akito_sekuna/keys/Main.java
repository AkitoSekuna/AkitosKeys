package com.akito_sekuna.keys;

import com.akito_sekuna.core.AkitosAddon;
import com.akito_sekuna.core.ReloadReason;
import com.akito_sekuna.core.api.ICoreAPI;
import com.akito_sekuna.keys.commands.KeyShopCommand;
import com.akito_sekuna.keys.commands.PromoCommand;
import com.akito_sekuna.keys.commands.WeeklyKeyAdminCommand;
import com.akito_sekuna.keys.commands.WeeklyKeyCommand;
import com.akito_sekuna.keys.listeners.KeyShopListener;
import com.akito_sekuna.keys.managers.CollectionManager;
import com.akito_sekuna.keys.managers.ConfigManager;
import com.akito_sekuna.keys.managers.DataManager;
import com.akito_sekuna.keys.managers.KeyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class Main extends JavaPlugin implements AkitosAddon {

    private ICoreAPI coreAPI;

    private ConfigManager configManager;
    private DataManager dataManager;
    private KeyManager keyManager;
    private CollectionManager collectionManager;
    private File customFolder;

    // Custom getter for your AkitosPlugins directory, matching AkitosLobby's pattern
    public File getCustomDataFolder() {
        if (customFolder == null) {
            customFolder = new File(getServer().getPluginsFolder(), "AkitosPlugins/AkitosKeys");
        }
        return customFolder;
    }

    // Ensures the custom folder exists and copies config.yml/data.yml into it directly.
    // Deliberately avoids saveDefaultConfig()/saveResource(), which always write to
    // Bukkit's standard getDataFolder() first and would leave an empty plugins/AkitosKeys/
    // folder behind after the files are relocated.
    private void loadCustomConfig() {
        File folder = getCustomDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        copyIfMissing(folder, "config.yml");
        copyIfMissing(folder, "data.yml");
    }

    private void copyIfMissing(File folder, String resourceName) {
        File target = new File(folder, resourceName);
        if (target.exists()) return;

        try (InputStream in = getResource(resourceName)) {
            if (in != null) {
                Files.copy(in, target.toPath());
            }
        } catch (Exception e) {
            getLogger().severe("Failed to copy default " + resourceName + ": " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        loadCustomConfig();

        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        keyManager = new KeyManager(this);
        collectionManager = new CollectionManager(this);

        com.akito_sekuna.core.Main.registerAddon(this);

        getCommand("wkey").setExecutor(new WeeklyKeyCommand(this));
        getCommand("weeklykey").setExecutor(new WeeklyKeyAdminCommand(this));

        KeyShopListener keyShopListener = new KeyShopListener(this);
        getCommand("kshop").setExecutor(new KeyShopCommand(this));
        getServer().getPluginManager().registerEvents(keyShopListener, this);

        getCommand("promo").setExecutor(new PromoCommand(this));

        // Check collections every 5 seconds. This means the collection system
        // doesn't need to know which plugin granted the player a head permission.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                collectionManager.checkPlayer(player);
            }
        }, 100L, 100L);

        getLogger().info("AkitosKeys enabled.");
    }

    @Override
    public String getAddonName() {
        return "AkitosKeys";
    }

    @Override
    public String getAddonVersion() {
        return getPluginMeta().getVersion();
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        getLogger().info("AkitosKeys disabled.");
    }

    @Override
    public void onCoreReady(ICoreAPI api) {
        this.coreAPI = api;
    }

    @Override
    public void onCoreReload(ICoreAPI api, ReloadReason reason) {
        this.coreAPI = api;
        configManager.reload();
    }

    @Override
    public void onCoreShutdown() {
        if (dataManager != null) {
            dataManager.save();
        }
    }

    public ICoreAPI getCoreAPI() {
        return coreAPI;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }
}
