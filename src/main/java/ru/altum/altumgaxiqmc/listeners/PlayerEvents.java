package ru.altum.altumgaxiqmc.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.TimeSkipEvent;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;
import ru.altum.altumgaxiqmc.util.Msg;

import java.util.HashMap;
import java.util.Map;

public class PlayerEvents implements Listener {

    private final AltumGaxiqMC plugin;
    private final Msg msg;

    public PlayerEvents(AltumGaxiqMC plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (!plugin.getConfig().getBoolean("messages.teleport.enabled", true)) return;

        Player p = e.getPlayer();
        if (e.getFrom() == null || e.getTo() == null) return;

        String fromWorld = e.getFrom().getWorld() != null ? e.getFrom().getWorld().getName() : "";
        String toWorld = e.getTo().getWorld() != null ? e.getTo().getWorld().getName() : "";

        Map<String, String> v = msg.baseVars(p);
        v.put("from_world", fromWorld);
        v.put("to_world", toWorld);

        String toPlayer = plugin.getConfig().getString("messages.teleport.to-player", "");
        if (toPlayer != null && !toPlayer.isEmpty()) {
            p.sendMessage(msg.parse(p, toPlayer, v));
        }

        boolean bEnabled = plugin.getConfig().getBoolean("messages.teleport.broadcast.enabled", false);
        if (bEnabled) {
            String fmt = plugin.getConfig().getString("messages.teleport.broadcast.format", "");
            msg.broadcast(fmt, p, v);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("messages.death.enabled", true)) return;

        Player p = e.getEntity();
        String fmt = plugin.getConfig().getString("messages.death.format", "");
        if (fmt == null || fmt.isEmpty()) return;

        String vanilla = e.getDeathMessage() == null ? "" : e.getDeathMessage();

        Map<String, String> v = msg.baseVars(p);
        v.put("death_message", vanilla);

        e.deathMessage(msg.parse(p, fmt, v));
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        if (!plugin.getConfig().getBoolean("messages.time.enabled", true)) return;

        World w = e.getWorld();
        if (w == null) return;

        long newTime = (w.getTime() + e.getSkipAmount()) % 24000L;

        boolean isDay = newTime < 12300L;
        String path = isDay ? "messages.time.day" : "messages.time.night";
        String raw = plugin.getConfig().getString(path, "");
        if (raw == null || raw.isEmpty()) return;

        for (Player p : w.getPlayers()) {
            Map<String, String> v = new HashMap<>();
            v.put("player_name", p.getName());
            v.put("world_name", w.getName());
            p.sendMessage(msg.parse(p, raw, v));
        }
    }
}
