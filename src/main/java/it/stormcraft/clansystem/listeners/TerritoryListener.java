package it.stormcraft.clansystem.listeners;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.territory.TerritoryManager;
import it.stormcraft.clansystem.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TerritoryListener implements Listener {

    private final ClanSystem       plugin;
    private final TerritoryManager tm;

    public TerritoryListener(ClanSystem plugin) {
        this.plugin = plugin;
        this.tm     = plugin.getTerritoryManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getPluginConfig().isPreventBuildByOutsiders()) return;
        if (plugin.isWorldGuardEnabled() && plugin.getPluginConfig().isUseWorldGuard()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("clansystem.admin")) return;

        var chunk = event.getBlock().getChunk();
        if (!tm.canBuild(player, event.getBlock().getWorld().getName(), chunk.getX(), chunk.getZ())) {
            event.setCancelled(true);
            MessageUtil.send(player, "§cNon puoi distruggere blocchi in un territorio altrui!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getPluginConfig().isPreventBuildByOutsiders()) return;
        if (plugin.isWorldGuardEnabled() && plugin.getPluginConfig().isUseWorldGuard()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("clansystem.admin")) return;

        var chunk = event.getBlock().getChunk();
        if (!tm.canBuild(player, event.getBlock().getWorld().getName(), chunk.getX(), chunk.getZ())) {
            event.setCancelled(true);
            MessageUtil.send(player, "§cNon puoi piazzare blocchi in un territorio altrui!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getPluginConfig().isPreventPvpByOutsiders()) return;
        if (plugin.isWorldGuardEnabled() && plugin.getPluginConfig().isUseWorldGuard()) return;

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity()  instanceof Player defender)) return;

        var chunk = event.getEntity().getLocation().getChunk();
        if (!tm.canPvP(attacker, defender, event.getEntity().getWorld().getName(), chunk.getX(), chunk.getZ())) {
            event.setCancelled(true);
            MessageUtil.send(attacker, "§cIl PvP non è permesso in questo territorio!");
        }
    }
}
