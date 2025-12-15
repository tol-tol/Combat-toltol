package com.toltol.combattoltol;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CombatToltolPlugin extends JavaPlugin {

    private CombatManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("Enabled Combat-toltol v" + getDescription().getVersion());

        File dataFolder = getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        getLogger().info("Data folder: " + dataFolder.getAbsolutePath());
        getLogger().info("Config path: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            try {
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                saveResource("config.yml", false);
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.combatManager = new CombatManager(this);

        Bukkit.getPluginManager().registerEvents(new CombatListener(this, combatManager), this);

        if (getCommand("전투모드") != null) {
            getCommand("전투모드").setExecutor(new CombatModeCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.shutdown();
        }
    }

    public int getCombatSeconds() {
        return Math.max(1, getConfig().getInt("combatSeconds", 15));
    }
}
