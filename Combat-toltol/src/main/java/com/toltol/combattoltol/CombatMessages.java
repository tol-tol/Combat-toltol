package com.toltol.combattoltol;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class CombatMessages {

    private CombatMessages() {
    }

    public static void sendCombatStart(Player player, String opponentName) {
        player.sendMessage(color(""));
        player.sendMessage(color("&c[ Combat System ] &a" + opponentName + "&f님과 전투가 시작되었습니다."));
        player.sendMessage(color("&c[ Combat System ] &f서버에서 나가시면 자동으로 &4사망 &f처리됩니다."));
        player.sendMessage(color(""));
    }

    public static void sendCombatEnd(Player player) {
        player.sendMessage(color("&c[ Combat System ] &fCombat System 가 종료되었습니다."));
        player.sendMessage(color("&c[ Combat System ] &f이제 서버에서 나가셔도 &4사망 &f처리되지 않습니다"));
        player.sendMessage(color(""));
    }

    public static void sendOpponentPunished(Player player, String punishedName) {
        player.sendMessage(color("&c[ Combat System ] &c" + punishedName + " &f님이 전투 중 서버를 나가 &4사망 &f처리되었습니다."));
    }

    public static void sendOpponentPunished(Player player, String punishedName, CombatManager.OutcomeReason reason) {
        player.sendMessage(color("&c[ Combat System ] &c" + punishedName + "&f님이 " + reasonToLoseDetail(reason) + " &4사망 &f처리되었습니다."));
    }

    public static void sendCombatEndedLine(Player player) {
        player.sendMessage(color("&c[ Combat System ] &fCombat System 가 종료되었습니다."));
    }

    public static void sendCombatActionBar(Player player, int remainingSeconds) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                color("&c[ Combat System ] &f남은 시간: &e" + remainingSeconds + "초")
        ));
    }

    public static void sendVictory(Player player, String opponentName) {
        player.sendMessage(color("&c[ Combat System ] &c" + opponentName + "&f님으로부터 &a승리&f하였습니다."));
        player.sendMessage(color("&c[ Combat System ] &f승리 판정으로 인해 Combat System 를 종료 합니다."));
    }

    public static void sendCombatLogWin(Player player, String opponentName) {
        player.sendMessage(color("&c[ Combat System ] &c" + opponentName + "&f님이 전투 중 서버를 나가 &a승리&f하였습니다."));
        player.sendMessage(color("&c[ Combat System ] &f전투 승리 판정으로 인해 Combat System 를 종료 합니다."));
    }

    public static void sendCombatLogWin(Player player, String opponentName, CombatManager.OutcomeReason reason) {
        player.sendMessage(color("&c[ Combat System ] &c" + opponentName + "&f님이 " + reasonToWinDetail(reason) + " &a승리&f하였습니다."));
        player.sendMessage(color("&c[ Combat System ] &f" + reasonToWinTitle(reason) + " 판정으로 인해 Combat System 를 종료 합니다."));
    }

    public static void sendCombatLogDefeat(Player player, String opponentName, CombatManager.OutcomeReason reason) {
        player.sendMessage(color("&c[ Combat System ] &a" + opponentName + "&f와 전투 중 " + reasonToLoseDetail(reason) + " &4패배&f하였습니다."));
        player.sendMessage(color("&c[ Combat System ] &4패배 &f판정으로 인해 Combat System 를 종료 합니다."));
    }

    public static void sendDefeat(Player player, String opponentName) {
        player.sendMessage(color("&c[ Combat System ] &a" + opponentName + "&f님한테 &4패배&f하였습니다."));
        player.sendMessage(color("&c[ Combat System ] &4패배 &f판정으로 인해 Combat System 를 종료 합니다."));
    }

    public static void sendCommandBlocked(Player player) {
        player.sendMessage(color("&c[ Combat System ] &4전투 중에는 명령어를 이용할수 없습니다!"));
    }

    public static void sendPunishedRejoin(Player player, String opponentName) {
        player.sendMessage(color("&c[ Combat System ] &a" + opponentName + "&f와 전투중에 나가서 &4사망 &f처리되었습니다."));
    }

    public static void sendPunishedRejoin(Player player, String opponentName, CombatManager.OutcomeReason reason) {
        player.sendMessage(color("&c[ Combat System ] &a" + opponentName + "&f와 전투 중 " + reasonToLoseDetail(reason) + " &4사망 &f처리되었습니다."));
    }

    private static String reasonToLoseDetail(CombatManager.OutcomeReason reason) {
        if (reason == null) return "이탈하여";
        return switch (reason) {
            case QUIT -> "서버를 나가";
            case TELEPORT -> "텔레포트를 사용하여";
            case PVP -> "패배하여";
        };
    }

    private static String reasonToWinDetail(CombatManager.OutcomeReason reason) {
        if (reason == null) return "전투 중 이탈하여";
        return switch (reason) {
            case QUIT -> "전투 중 서버를 나가";
            case TELEPORT -> "전투 중 텔레포트를 사용하여";
            case PVP -> "PVP에서 승리하여";
        };
    }

    private static String reasonToWinTitle(CombatManager.OutcomeReason reason) {
        if (reason == null) return "승리";
        return switch (reason) {
            case QUIT, TELEPORT -> "전투 승리";
            case PVP -> "승리";
        };
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
