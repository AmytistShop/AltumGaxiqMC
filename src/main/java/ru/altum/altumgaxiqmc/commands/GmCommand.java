package ru.altum.altumgaxiqmc.commands;

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String noPerm = plugin.getConfig().getString("messages.errors.no-permission", "&cНет прав.");
        String usage = plugin.getConfig().getString("messages.errors.usage", "&cИспользование: /gm <c|s|a|sp> [ник]");
        String notFound = plugin.getConfig().getString("messages.errors.player-not-found", "&cИгрок не найден.");

        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.colorizeAmpersand("&cКоманда доступна только игроку."));
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

        if (args.length == 1) {
            p.setGameMode(gm);

            Map<String, String> v = msg.baseVars(p);
            v.put("gamemode", modeName(gm));
            msg.send(p, "messages.gamemode.self", v);
            return true;
        }

        if (!p.hasPermission("altumgaxiq.gm.others")) {
            p.sendMessage(msg.parse(p, noPerm, msg.baseVars(p)));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
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
            return Arrays.asList("c", "s", "a", "sp", "creative", "survival", "adventure", "spectator")
                    .stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (!p.hasPermission("altumgaxiq.gm.others")) return Collections.emptyList();
            String pref = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pref))
                    .limit(20)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
