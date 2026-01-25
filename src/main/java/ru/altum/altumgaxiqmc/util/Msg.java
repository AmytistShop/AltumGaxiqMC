package ru.altum.altumgaxiqmc.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;

import java.util.HashMap;
import java.util.Map;

public class Msg {
    private final AltumGaxiqMC plugin;

    public Msg(AltumGaxiqMC plugin) {
        this.plugin = plugin;
    }

    public boolean hasPapi() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isBedrock(Player player) {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if (floodgate == null) return false;

        // Reflection: don't hard-crash if API changes
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object isFloodgate = apiClass.getMethod("isFloodgatePlayer", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
            return (boolean) isFloodgate;
        } catch (Throwable t) {
            return false;
        }
    }

    public String platform(Player player) {
        return isBedrock(player) ? "BEDROCK" : "JAVA";
    }

    public String colorizeAmpersand(String s) {
        if (s == null) return "";
        // Replace & -> § for legacy colors
        return s.replace("&", "§");
    }

    public Component parse(Player p, String raw, Map<String, String> vars) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        String s = raw;

        // custom vars
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                s = s.replace("%" + e.getKey() + "%", e.getValue());
            }
        }

        // geyser vars
        if (p != null) {
            s = s.replace("%is_bedrock%", String.valueOf(isBedrock(p)));
            s = s.replace("%platform%", platform(p));
        } else {
            s = s.replace("%is_bedrock%", "false");
            s = s.replace("%platform%", "JAVA");
        }

        // PlaceholderAPI vars
        if (p != null && hasPapi()) {
            s = PlaceholderAPI.setPlaceholders(p, s);
        }

        s = colorizeAmpersand(s);

        // Paper renders legacy codes in Component.text properly in most cases
        return Component.text(s);
    }

    public void send(Player p, String path, Map<String, String> vars) {
        String raw = plugin.getConfig().getString(path, "");
        if (raw == null || raw.isEmpty()) return;
        p.sendMessage(parse(p, raw, vars));
    }

    public void broadcast(String raw, Player contextPlayer, Map<String, String> vars) {
        if (raw == null || raw.isEmpty()) return;
        Component c = parse(contextPlayer, raw, vars);
        Bukkit.getServer().broadcast(c);
    }

    public Map<String, String> baseVars(Player p) {
        Map<String, String> v = new HashMap<>();
        if (p != null) {
            v.put("player_name", p.getName());
            World w = p.getWorld();
            v.put("world_name", w != null ? w.getName() : "");
        }
        return v;
    }
}
