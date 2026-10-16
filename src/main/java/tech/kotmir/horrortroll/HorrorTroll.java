package tech.kotmir.horrortroll;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import tech.kotmir.horrortroll.managers.PluginManager;

public final class HorrorTroll extends JavaPlugin {

    private static HorrorTroll instance;
    private PluginManager pluginManager;
    
    // Список замороженных игроков для команд троллинга и фейк-ошибок
    public final Set<UUID> frozenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        this.pluginManager = new PluginManager(this);
        this.pluginManager.init();
        getLogger().info("HorrorTroll v2.0 успешно запущен на 1.21.1!");
    }

    @Override
    public void onDisable() {
        if (this.pluginManager != null) {
            this.pluginManager.cleanup();
        }
        frozenPlayers.clear();
        getLogger().info("HorrorTroll выключен.");
    }

    public static HorrorTroll getInstance() {
        return instance;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
}