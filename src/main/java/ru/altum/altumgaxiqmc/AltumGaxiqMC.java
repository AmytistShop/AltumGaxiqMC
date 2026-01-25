package ru.altum.altumgaxiqmc;

import org.bukkit.plugin.java.JavaPlugin;
import ru.altum.altumgaxiqmc.commands.GmCommand;
import ru.altum.altumgaxiqmc.listeners.PlayerEvents;
import ru.altum.altumgaxiqmc.listeners.TimeCommandListener;
import ru.altum.altumgaxiqmc.util.Msg;

public final class AltumGaxiqMC extends JavaPlugin {

    private Msg msg;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.msg = new Msg(this);

        // Commands
        if (getCommand("gm") != null) {
            GmCommand gm = new GmCommand(this, msg);
            getCommand("gm").setExecutor(gm);
            getCommand("gm").setTabCompleter(gm);
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerEvents(this, msg), this);
        getServer().getPluginManager().registerEvents(new TimeCommandListener(this, msg), this);

        getLogger().info("AltumGaxiqMC enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AltumGaxiqMC disabled.");
    }

    public Msg msg() {
        return msg;
    }
}
