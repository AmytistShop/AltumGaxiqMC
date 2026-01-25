package ru.altum.altumgaxiqmc.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;
import ru.altum.altumgaxiqmc.util.Msg;

import java.util.HashMap;
import java.util.Map;

public class TimeCommandListener implements Listener {

    private final AltumGaxiqMC plugin;
    private final Msg msg;

    public TimeCommandListener(AltumGaxiqMC plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    @EventHandler
    public void onTimeCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("messages.time.enabled", true)) return;

        Player p = e.getPlayer();
        String cmd = e.getMessage().trim().toLowerCase();

        // Supports:
        // /time set day|night
        // /time day|night
        // /day, /night (if present)
        String time = null;

        if (cmd.equals("/day") || cmd.startsWith("/time set day") || cmd.equals("/time day") || cmd.startsWith("/time day")) {
            time = "day";
        } else if (cmd.equals("/night") || cmd.startsWith("/time set night") || cmd.equals("/time night") || cmd.startsWith("/time night")) {
            time = "night";
        }

        if (time == null) return;

        String path = "messages.time.set-" + time;
        String raw = plugin.getConfig().getString(path, "");
        if (raw == null || raw.isEmpty()) return;

        Map<String, String> v = new HashMap<>();
        v.put("player_name", p.getName());
        v.put("world_name", p.getWorld().getName());
        v.put("time", time);

        Bukkit.broadcast(msg.parse(p, raw, v));
    }
}
