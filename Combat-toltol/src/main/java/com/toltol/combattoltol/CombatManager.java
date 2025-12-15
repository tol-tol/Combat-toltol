package com.toltol.combattoltol;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public final class CombatManager {

    public enum OutcomeReason {
        QUIT,
        TELEPORT,
        PVP
    }

    private final CombatToltolPlugin plugin;

    private final Map<UUID, CombatState> states = new HashMap<>();

    private final Map<UUID, PendingRejoinNotice> pendingRejoinNotices = new HashMap<>();

    private final Set<UUID> punishedThisSession = new HashSet<>();

    private BukkitTask tickTask;

    public CombatManager(CombatToltolPlugin plugin) {
        this.plugin = plugin;

        loadPendingRejoinNotices();

        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void loadPendingRejoinNotices() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pendingRejoinNotices");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection item = section.getConfigurationSection(key);
                if (item == null) continue;

                String opponentName = item.getString("opponent", "");
                String reasonRaw = item.getString("reason", "");

                if (opponentName == null || opponentName.isBlank()) continue;
                OutcomeReason reason;
                try {
                    reason = OutcomeReason.valueOf(reasonRaw);
                } catch (IllegalArgumentException ex) {
                    reason = OutcomeReason.QUIT;
                }

                pendingRejoinNotices.put(playerId, new PendingRejoinNotice(opponentName, reason));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void savePendingRejoinNotice(UUID playerId, String opponentName, OutcomeReason reason) {
        pendingRejoinNotices.put(playerId, new PendingRejoinNotice(opponentName, reason));
        plugin.getConfig().set("pendingRejoinNotices." + playerId + ".opponent", opponentName);
        plugin.getConfig().set("pendingRejoinNotices." + playerId + ".reason", String.valueOf(reason));
        plugin.saveConfig();
    }

    private void removePendingRejoinNotice(UUID playerId) {
        pendingRejoinNotices.remove(playerId);
        plugin.getConfig().set("pendingRejoinNotices." + playerId, null);
        plugin.saveConfig();
    }

    public boolean isInCombat(UUID playerId) {
        return states.containsKey(playerId);
    }

    public CombatState getState(UUID playerId) {
        return states.get(playerId);
    }

    public void tag(Player a, Player b) {
        if (a == null || b == null) return;
        if (!a.isOnline() || !b.isOnline()) return;
        if (a.getUniqueId().equals(b.getUniqueId())) return;

        tagOneSide(a, b);
        tagOneSide(b, a);
    }

    private void tagOneSide(Player owner, Player opponent) {
        UUID ownerId = owner.getUniqueId();

        CombatState state = states.get(ownerId);
        boolean wasInCombat = (state != null);
        if (state == null) {
            state = new CombatState();
            states.put(ownerId, state);
        }

        state.opponents.put(opponent.getUniqueId(), opponent.getName());
        state.endAtMs = System.currentTimeMillis() + (plugin.getCombatSeconds() * 1000L);

        if (!wasInCombat) {
            CombatMessages.sendCombatStart(owner, opponent.getName());
        }
    }

    public void punishAndNotifyOpponents(UUID punishedId, String punishedName, OutcomeReason reason) {
        CombatState punishedState = states.get(punishedId);
        if (punishedState == null) return;

        plugin.getLogger().info("[CombatDebug] punishAndNotifyOpponents punished=" + punishedName + " reason=" + reason + " opponents=" + punishedState.opponents.size());

        punishedThisSession.add(punishedId);

        Player punished = Bukkit.getPlayer(punishedId);

        String opponentNameForRejoin = "";
        if (!punishedState.opponents.isEmpty()) {
            opponentNameForRejoin = punishedState.opponents.values().iterator().next();
        }
        if (!opponentNameForRejoin.isBlank()) {
            savePendingRejoinNotice(punishedId, opponentNameForRejoin, reason);

            if (punished != null && punished.isOnline()) {
                CombatMessages.sendCombatLogDefeat(punished, opponentNameForRejoin, reason);
            }
        }

        for (UUID opponentId : punishedState.opponents.keySet()) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                CombatMessages.sendOpponentPunished(opponent, punishedName, reason);
            }

            CombatState opponentState = states.get(opponentId);
            if (opponentState != null) {
                opponentState.opponents.remove(punishedId);
                if (opponentState.opponents.isEmpty()) {
                    states.remove(opponentId);
                    if (opponent != null && opponent.isOnline()) {
                        CombatMessages.sendCombatLogWin(opponent, punishedName, reason);
                        CombatMessages.sendCombatEndedLine(opponent);
                    }
                }
            }
        }

        states.remove(punishedId);
    }

    public boolean consumePunishedFlag(UUID playerId) {
        return punishedThisSession.remove(playerId);
    }

    public PendingRejoinNotice pollRejoinNotice(UUID playerId) {
        PendingRejoinNotice notice = pendingRejoinNotices.get(playerId);
        if (notice == null) return null;
        removePendingRejoinNotice(playerId);
        return notice;
    }

    public static final class PendingRejoinNotice {
        private final String opponentName;
        private final OutcomeReason reason;

        public PendingRejoinNotice(String opponentName, OutcomeReason reason) {
            this.opponentName = opponentName;
            this.reason = reason;
        }

        public String getOpponentName() {
            return opponentName;
        }

        public OutcomeReason getReason() {
            return reason;
        }
    }

    public void handleDeath(UUID deadId) {
        CombatState deadState = states.remove(deadId);
        if (deadState == null) return;

        Player dead = Bukkit.getPlayer(deadId);
        plugin.getLogger().info("[CombatDebug] handleDeath dead=" + (dead != null ? dead.getName() : deadId) + " opponents=" + deadState.opponents.size());

        if (deadState.opponents.size() == 1) {
            Map.Entry<UUID, String> only = deadState.opponents.entrySet().iterator().next();
            UUID opponentId = only.getKey();
            String opponentName = only.getValue();

            plugin.getLogger().info("[CombatDebug] handleDeath 1v1 opponentName=" + opponentName);

            CombatState opponentState = states.get(opponentId);
            if (opponentState != null && opponentState.opponents.size() == 1 && opponentState.opponents.containsKey(deadId)) {
                if (dead != null && dead.isOnline()) {
                    CombatMessages.sendDefeat(dead, opponentName);
                }

                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    String deadName = opponentState.opponents.get(deadId);
                    if (deadName == null) {
                        deadName = "";
                    }
                    CombatMessages.sendVictory(opponent, deadName);
                }

                states.remove(opponentId);
                return;
            }
        }

        for (UUID opponentId : deadState.opponents.keySet()) {
            CombatState opponentState = states.get(opponentId);
            if (opponentState != null) {
                opponentState.opponents.remove(deadId);
                if (opponentState.opponents.isEmpty()) {
                    states.remove(opponentId);
                    Player opponent = Bukkit.getPlayer(opponentId);
                    if (opponent != null && opponent.isOnline()) {
                        CombatMessages.sendCombatEnd(opponent);
                    }
                }
            }
        }
    }

    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        states.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, CombatState>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CombatState> entry = it.next();
            UUID playerId = entry.getKey();
            CombatState state = entry.getValue();

            long remainingMs = state.endAtMs - now;
            if (remainingMs <= 0) {
                it.remove();
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    CombatMessages.sendCombatEnd(player);
                }
                continue;
            }

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            long remainingSeconds = Math.max(1, (remainingMs + 999) / 1000);
            CombatMessages.sendCombatActionBar(player, (int) remainingSeconds);
        }
    }

    public static final class CombatState {
        private final Map<UUID, String> opponents = new HashMap<>();
        private long endAtMs;

        public Map<UUID, String> getOpponents() {
            return opponents;
        }

        public long getEndAtMs() {
            return endAtMs;
        }
    }
}
