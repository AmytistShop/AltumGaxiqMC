package ru.altum.altumgaxiqmc.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;
import ru.altum.altumgaxiqmc.util.Msg;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TimeCommandListener implements Listener {

    private final AltumGaxiqMC plugin;
    private final Msg msg;

    public TimeCommandListener(AltumGaxiqMC plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    private String stripNamespace(String cmd) {
        int idx = cmd.indexOf(':');
        if (idx >= 0 && idx + 1 < cmd.length()) return cmd.substring(idx + 1);
        return cmd;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTimeCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("messages.time.enabled", true)) return;

        Player p = e.getPlayer();
        if (!p.hasPermission("altumgaxiq.time")) return;

        String full = e.getMessage().trim();
        if (!full.startsWith("/")) return;

        String[] parts = full.substring(1).split("\\s+");
        if (parts.length == 0) return;

        String base = stripNamespace(parts[0].toLowerCase(Locale.ROOT));
        String time = null;

        if (base.equals("day")) time = "day";
        else if (base.equals("night")) time = "night";
        else if (base.equals("time")) {
            if (parts.length >= 2) {
                String p1 = parts[1].toLowerCase(Locale.ROOT);
                if (p1.equals("day")) time = "day";
                else if (p1.equals("night")) time = "night";
                else if (p1.equals("set") && parts.length >= 3) {
                    String p2 = parts[2].toLowerCase(Locale.ROOT);
                    if (p2.equals("day")) time = "day";
                    else if (p2.equals("night")) time = "night";
                }
            }
        }

        if (time == null) return;

        // cancel to remove vanilla feedback
        e.setCancelled(true);

        World w = p.getWorld();
        if (time.equals("day")) w.setTime(1000L);
        else w.setTime(13000L);

        String raw = plugin.getConfig().getString("messages.time.set-" + time, "");
        if (raw == null || raw.isEmpty()) return;

        Map<String, String> v = new HashMap<>();
        v.put("player_name", p.getName());
        v.put("world_name", w.getName());
        v.put("time", time);

        Bukkit.broadcast(msg.parse(p, raw, v));
    }
}
