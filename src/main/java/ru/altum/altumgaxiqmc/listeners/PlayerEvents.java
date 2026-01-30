package ru.altum.altumgaxiqmc.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;
import ru.altum.altumgaxiqmc.util.Msg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvents implements Listener {

    private final AltumGaxiqMC plugin;
    private final Msg msg;

    // anti-spam for teleport messages (per player)
    private final Map<UUID, Long> lastTpMsg = new ConcurrentHashMap<>();

    public PlayerEvents(AltumGaxiqMC plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (!plugin.getConfig().getBoolean("messages.teleport.enabled", true)) return;

        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        // Ignore causes (e.g. auth plugins)
        try {
            java.util.List<String> ignore = plugin.getConfig().getStringList("messages.teleport.ignore-causes");
            if (ignore != null && !ignore.isEmpty()) {
                String cause = e.getCause().name();
                for (String s : ignore) {
                    if (s != null && !s.isEmpty() && cause.equalsIgnoreCase(s)) return;
                }
            }
        } catch (Throwable ignored) {}

        // Option: only message when world changes
        boolean onlyWorldChange = plugin.getConfig().getBoolean("messages.teleport.only-world-change", false);
        String fromWorld = from.getWorld() != null ? from.getWorld().getName() : "";
        String toWorld = to.getWorld() != null ? to.getWorld().getName() : "";

        if (onlyWorldChange && fromWorld.equalsIgnoreCase(toWorld)) return;

        // Ignore teleports within same block (common with auth plugins)
        if (from.getWorld() != null && to.getWorld() != null
                && from.getWorld().equals(to.getWorld())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Cooldown (ms)
        long cooldown = plugin.getConfig().getLong("messages.teleport.cooldown-ms", 1500L);
        long now = System.currentTimeMillis();
        Long last = lastTpMsg.get(p.getUniqueId());
        if (last != null && (now - last) < cooldown) return;
        lastTpMsg.put(p.getUniqueId(), now);

        Map<String, String> v = msg.baseVars(p);
        v.put("from_world", fromWorld);
        v.put("to_world", toWorld);

        // to player
        String toPlayer = plugin.getConfig().getString("messages.teleport.to-player", "");
        if (toPlayer != null && !toPlayer.isEmpty()) {
            p.sendMessage(msg.parse(p, toPlayer, v));
        }

        // broadcast optional
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

        String vanilla = e.getDeathMessage() == null ? "" : e.getDeathMessage();

        Map<String, String> v = msg.baseVars(p);
        v.put("death_message", vanilla);

        // Broadcast (replace server death message)
        String fmt = plugin.getConfig().getString("messages.death.format", "");
        if (fmt != null && !fmt.isEmpty()) {
            e.deathMessage(msg.parse(p, fmt, v));
        }

        // Send to player chat
        String toPlayer = plugin.getConfig().getString("messages.death.to-player", "");
        if (toPlayer != null && !toPlayer.isEmpty()) {
            p.sendMessage(msg.parse(p, toPlayer, v));
        }

        // Title on death screen (works for Java and Bedrock clients)
        boolean titleEnabled = plugin.getConfig().getBoolean("messages.death.title.enabled", true);
        if (titleEnabled) {
            String titleRaw = plugin.getConfig().getString("messages.death.title.title", "");
            String subRaw = plugin.getConfig().getString("messages.death.title.subtitle", "");
            Component title = msg.parse(p, titleRaw, v);
            Component sub = msg.parse(p, subRaw, v);
            p.showTitle(Title.title(title, sub));
        }
    }
}
