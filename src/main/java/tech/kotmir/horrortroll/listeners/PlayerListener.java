package tech.kotmir.horrortroll.listeners;

import org.bukkit.event.Listener;
import tech.kotmir.horrortroll.HorrorTroll;

public class PlayerListener implements Listener {
    private final HorrorTroll plugin;

    public PlayerListener(HorrorTroll plugin) {
        this.plugin = plugin;
    }
}