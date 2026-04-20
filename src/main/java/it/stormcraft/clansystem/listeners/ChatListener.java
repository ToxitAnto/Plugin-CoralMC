package it.stormcraft.clansystem.listeners;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ClanSystem plugin;

    public ChatListener(ClanSystem plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String prefixTemplate = plugin.getPluginConfig().getGlobalChatPrefix();
        if (prefixTemplate == null || prefixTemplate.isEmpty()) return;

        Clan clan = plugin.getClanManager().getPlayerClan(event.getPlayer().getUniqueId());
        if (clan == null) return;

        String prefix = prefixTemplate
                .replace("&", "§")
                .replace("{clan_tag}", clan.getTag())
                .replace("{clan_name}", clan.getName());

        event.setFormat(prefix + event.getFormat());
    }
}
