package com.toltol.combattoltol;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CombatModeCommand implements CommandExecutor {

    private final CombatToltolPlugin plugin;

    public CombatModeCommand(CombatToltolPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combattoltol.admin")) {
            sender.sendMessage(CombatMessages.color("명령어를 다시 확인해주세요"));
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("설정")) {
            sender.sendMessage(CombatMessages.color("명령어를 다시 확인해주세요"));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(CombatMessages.color("명령어를 다시 확인해주세요"));
            return true;
        }

        if (seconds <= 0) {
            sender.sendMessage(CombatMessages.color("명령어를 다시 확인해주세요"));
            return true;
        }

        plugin.getConfig().set("combatSeconds", seconds);
        plugin.saveConfig();

        sender.sendMessage(CombatMessages.color("&c[ Combat System ] &f" + seconds + "초 만큼 전투 모드를 설정했습니다"));
        sender.sendMessage(CombatMessages.color("&c[ Combat System ] &f설정이 잘됬습니다"));

        return true;
    }
}
