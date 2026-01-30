package ru.altum.altumgaxiqmc.commands;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.altum.altumgaxiqmc.AltumGaxiqMC;
import ru.altum.altumgaxiqmc.util.Msg;

import java.util.*;
import java.util.stream.Collectors;

public class GmCommand implements CommandExecutor, TabCompleter {

    private final AltumGaxiqMC plugin;
    private final Msg msg;

    public GmCommand(AltumGaxiqMC plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    private GameMode parseMode(String s) {
        if (s == null) return null;
        s = s.toLowerCase(Locale.ROOT);

        return switch (s) {
            case "c", "creative", "1" -> GameMode.CREATIVE;
            case "s", "survival", "0" -> GameMode.SURVIVAL;
            case "a", "adventure", "2" -> GameMode.ADVENTURE;
            case "sp", "spec", "spectator", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String modeName(GameMode gm) {
        return switch (gm) {
            case CREATIVE -> "CREATIVE";
            case SURVIVAL -> "SURVIVAL";
            case ADVENTURE -> "ADVENTURE";
            case SPECTATOR -> "SPECTATOR";
        };
    }

    private String normalizeName(String s) {
        if (s == null) return "";
        s = s.replace('§', '&');
        // remove legacy & color codes
        s = s.replaceAll("(?i)&[0-9A-FK-OR]", "");
        s = s.trim();
        if (s.startsWith(".")) s = s.substring(1); // Floodgate dot support
        s = s.replaceAll("\s+", " ");
        return s.toLowerCase(Locale.ROOT);
    }

    private String displayNamePlain(Player p) {
        try {
            return PlainTextComponentSerializer.plainText().serialize(p.displayName());
        } catch (Throwable t) {
            return p.getName();
        }
    }

    private Player findOnlinePlayer(String input) {
        if (input == null || input.isEmpty()) return null;

        String wanted = normalizeName(input);

        // Exact username
        Player p = Bukkit.getPlayerExact(input);
        if (p != null) return p;

        // Bukkit helper (partial, ignore-case)
        p = Bukkit.getPlayer(input);
        if (p != null) return p;

        // Username ignore-case (+ dotless)
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (normalizeName(pl.getName()).equals(wanted)) return pl;
            // accept ".Name" vs "Name"
            if (normalizeName("." + pl.getName()).equals(wanted)) return pl;
        }

        // DisplayName exact
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (normalizeName(displayNamePlain(pl)).equals(wanted)) return pl;
        }

        // Contains / word match for displayName prefixes like "[VIP] Alex"
        for (Player pl : Bukkit.getOnlinePlayers()) {
            String uname = normalizeName(pl.getName());
            String dname = normalizeName(displayNamePlain(pl));
            if (uname.contains(wanted) || dname.contains(wanted)) return pl;
            for (String part : dname.split(" ")) {
                if (part.equals(wanted)) return pl;
            }
        }

        // Starts-with fallback
        for (Player pl : Bukkit.getOnlinePlayers()) {
            String uname = normalizeName(pl.getName());
            String dname = normalizeName(displayNamePlain(pl));
            if (uname.startsWith(wanted) || dname.startsWith(wanted)) return pl;
        }

        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String noPerm = plugin.getConfig().getString("messages.errors.no-permission", "&cНет прав.");
        String usage = plugin.getConfig().getString("messages.errors.usage", "&cИспользование: /gm <c|s|a|sp> [ник]");
        String notFound = plugin.getConfig().getString("messages.errors.player-not-found", "&cИгрок не найден.");

        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.parse(null, "&cКоманда доступна только игроку.", null));
            return true;
        }

        if (!p.hasPermission("altumgaxiq.gm")) {
            p.sendMessage(msg.parse(p, noPerm, msg.baseVars(p)));
            return true;
        }

        if (args.length < 1) {
            p.sendMessage(msg.parse(p, usage, msg.baseVars(p)));
            return true;
        }

        GameMode gm = parseMode(args[0]);
        if (gm == null) {
            p.sendMessage(msg.parse(p, usage, msg.baseVars(p)));
            return true;
        }

        // /gm c  -> self
        if (args.length == 1) {
            p.setGameMode(gm);

            Map<String, String> v = msg.baseVars(p);
            v.put("gamemode", modeName(gm));
            msg.send(p, "messages.gamemode.self", v);
            return true;
        }

        // /gm c nick -> other
        if (!p.hasPermission("altumgaxiq.gm.others")) {
            p.sendMessage(msg.parse(p, noPerm, msg.baseVars(p)));
            return true;
        }

        Player target = findOnlinePlayer(args[1]);
        if (target == null) {
            p.sendMessage(msg.parse(p, notFound, msg.baseVars(p)));
            return true;
        }

        target.setGameMode(gm);

        Map<String, String> senderVars = msg.baseVars(p);
        senderVars.put("gamemode", modeName(gm));
        senderVars.put("target", target.getName());
        msg.send(p, "messages.gamemode.other-sender", senderVars);

        Map<String, String> targetVars = msg.baseVars(target);
        targetVars.put("gamemode", modeName(gm));
        msg.send(target, "messages.gamemode.other-target", targetVars);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();

        if (args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            return Arrays.asList("c", "s", "a", "sp", "creative", "survival", "adventure", "spectator")
                    .stream()
                    .filter(x -> x.startsWith(pref))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (!p.hasPermission("altumgaxiq.gm.others")) return Collections.emptyList();
            String pref = args[1].toLowerCase(Locale.ROOT);

            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                String name = pl.getName();
                names.add(name);
                if (name.startsWith(".")) names.add(name.substring(1));
            }

            return names.stream()
                    .distinct()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pref))
                    .limit(20)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
