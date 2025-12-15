package com.toltol.combattoltol;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatListener implements Listener {

    private final CombatToltolPlugin plugin;
    private final CombatManager combatManager;

    private final Map<UUID, Long> lastCommandWarnAtMs = new HashMap<>();

    public CombatListener(CombatToltolPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = asPlayer(event.getEntity());
        if (victim == null) return;

        Player damager = resolveDamager(event.getDamager());
        if (damager == null) return;

        if (victim.getUniqueId().equals(damager.getUniqueId())) return;

        combatManager.tag(victim, damager);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CombatManager.PendingRejoinNotice notice = combatManager.pollRejoinNotice(player.getUniqueId());
        if (notice == null) return;
        if (notice.getOpponentName() == null || notice.getOpponentName().isBlank()) return;

        CombatMessages.sendPunishedRejoin(player, notice.getOpponentName(), notice.getReason());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player.getUniqueId())) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = lastCommandWarnAtMs.get(player.getUniqueId());
        if (last == null || (now - last) >= 1000L) {
            lastCommandWarnAtMs.put(player.getUniqueId(), now);
            CombatMessages.sendCommandBlocked(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!combatManager.isInCombat(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player.getUniqueId())) return;

        plugin.getLogger().info("[CombatDebug] onQuit punished=" + player.getName());
        combatManager.punishAndNotifyOpponents(player.getUniqueId(), player.getName(), CombatManager.OutcomeReason.QUIT);
        player.setHealth(0.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player.getUniqueId())) return;

        plugin.getLogger().info("[CombatDebug] onTeleport punished=" + player.getName() + " cause=" + event.getCause());

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }

        combatManager.punishAndNotifyOpponents(player.getUniqueId(), player.getName(), CombatManager.OutcomeReason.TELEPORT);
        player.setHealth(0.0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        if (combatManager.consumePunishedFlag(dead.getUniqueId())) {
            plugin.getLogger().info("[CombatDebug] onDeath skipped PvP handling (punished)");
            return;
        }
        combatManager.handleDeath(dead.getUniqueId());
    }

    private static Player asPlayer(Entity entity) {
        return (entity instanceof Player p) ? p : null;
    }

    private static Player resolveDamager(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile) {
            Object shooter = projectile.getShooter();
            if (shooter instanceof Player p) {
                return p;
            }
        }
        return null;
    }

}
