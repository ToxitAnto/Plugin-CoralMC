package it.stormcraft.clansystem.listeners;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import it.stormcraft.clansystem.commands.ClanCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ClanSystem  plugin;
    private final ClanCommand clanCommand;

    public PlayerListener(ClanSystem plugin, ClanCommand clanCommand) {
        this.plugin      = plugin;
        this.clanCommand = clanCommand;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        clanCommand.cancelTeleport(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clanCommand.cancelTeleport(event.getPlayer().getUniqueId());

        Clan clan = plugin.getClanManager().getPlayerClan(event.getPlayer().getUniqueId());
        if (clan != null) {
            String msg = plugin.getPluginConfig().getMessage("member-disconnected")
                    .replace("{player}", event.getPlayer().getName());
            clan.getOnlineMembers().forEach(p -> {
                if (!p.equals(event.getPlayer())) p.sendMessage(msg);
            });
        }
    }
}
