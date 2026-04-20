package it.stormcraft.clansystem.placeholders;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import it.stormcraft.clansystem.clan.ClanMember;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClanPlaceholders extends PlaceholderExpansion {

    private final ClanSystem plugin;

    public ClanPlaceholders(ClanSystem plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "clans"; }
    @Override public @NotNull String getAuthor()     { return "CkAnto"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "player_clan"         -> clan != null ? clan.getName() : "Nessuno";
            case "player_tag"          -> clan != null ? clan.getTag()  : "";
            case "player_role"         -> {
                if (clan == null) yield "";
                ClanMember member = clan.getMember(player.getUniqueId());
                yield member != null ? member.getRole().getDisplayName() : "";
            }
            case "clan_members_online" -> clan != null ? String.valueOf(clan.getOnlineCount())  : "0";
            case "clan_members_total"  -> clan != null ? String.valueOf(clan.getMemberCount())  : "0";
            default -> null;
        };
    }
}
