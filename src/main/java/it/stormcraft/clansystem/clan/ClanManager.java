package it.stormcraft.clansystem.clan;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.database.DatabaseManager;
import it.stormcraft.clansystem.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ClanManager {

    private final ClanSystem plugin;
    private final DatabaseManager db;

    private final Map<Integer, Clan>   clansById    = new ConcurrentHashMap<>();
    private final Map<String, Integer> clansByName  = new ConcurrentHashMap<>();
    private final Map<String, Integer> clansByTag   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>   playerClanMap = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    public ClanManager(ClanSystem plugin) {
        this.plugin = plugin;
        this.db     = plugin.getDatabaseManager();
        loadAll();
    }

    private void loadAll() {
        try {
            List<Clan> loaded = db.loadAllClans();
            for (Clan clan : loaded) {
                indexClan(clan);
                List<ClanMember> members = db.loadMembersForClan(clan.getId());
                for (ClanMember m : members) {
                    clan.addMember(m);
                    playerClanMap.put(m.getPlayerUuid(), clan.getId());
                }
            }
            plugin.getLogger().info("Caricati " + loaded.size() + " clan dal database.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nel caricamento dei clan", e);
        }
    }

    private void indexClan(Clan clan) {
        clansById.put(clan.getId(), clan);
        clansByName.put(clan.getName().toLowerCase(), clan.getId());
        clansByTag.put(clan.getTag().toLowerCase(), clan.getId());
    }

    private void deIndexClan(Clan clan) {
        clansById.remove(clan.getId());
        clansByName.remove(clan.getName().toLowerCase());
        clansByTag.remove(clan.getTag().toLowerCase());
    }

    public Clan createClan(Player leader, String name, String tag) {
        if (isNameTaken(name) || isTagTaken(tag)) return null;
        if (getPlayerClan(leader.getUniqueId()) != null) return null;

        try {
            Clan clan = db.insertClan(name, tag, leader.getUniqueId());
            if (clan == null) return null;

            ClanMember leaderMember = new ClanMember(
                    leader.getUniqueId(), leader.getName(),
                    clan.getId(), ClanRole.LEADER, Instant.now()
            );
            db.insertMember(leaderMember);
            clan.addMember(leaderMember);
            indexClan(clan);
            playerClanMap.put(leader.getUniqueId(), clan.getId());
            return clan;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nella creazione del clan", e);
            return null;
        }
    }

    public boolean disbandClan(Clan clan) {
        try {
            for (ClanMember m : clan.getMembers()) playerClanMap.remove(m.getPlayerUuid());
            db.deleteClan(clan.getId());
            deIndexClan(clan);
            if (plugin.isWorldGuardEnabled()) plugin.getTerritoryManager().removeAllRegions(clan);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nello scioglimento del clan", e);
            return false;
        }
    }

    public boolean addMember(Clan clan, Player player) {
        if (playerClanMap.containsKey(player.getUniqueId())) return false;

        ClanMember member = new ClanMember(
                player.getUniqueId(), player.getName(),
                clan.getId(), ClanRole.MEMBER, Instant.now()
        );
        try {
            db.insertMember(member);
            clan.addMember(member);
            playerClanMap.put(player.getUniqueId(), clan.getId());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nell'aggiunta del membro", e);
            return false;
        }
    }

    public boolean removeMember(Clan clan, UUID target) {
        ClanMember member = clan.getMember(target);
        if (member == null) return false;
        try {
            db.deleteMember(target);
            clan.removeMember(target);
            playerClanMap.remove(target);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nella rimozione del membro", e);
            return false;
        }
    }

    public ClanRole promoteMember(Clan clan, UUID target) {
        ClanMember member = clan.getMember(target);
        if (member == null) return null;

        if (member.getRole() != ClanRole.MEMBER) return null;

        ClanRole newRole = ClanRole.OFFICER;
        member.setRole(newRole);
        try {
            db.updateMemberRole(target, newRole);
            return newRole;
        } catch (Exception e) {
            member.setRole(ClanRole.MEMBER);
            plugin.getLogger().log(Level.SEVERE, "Errore nella promozione", e);
            return null;
        }
    }

    public ClanRole demoteMember(Clan clan, UUID target) {
        ClanMember member = clan.getMember(target);
        if (member == null || member.getRole() == ClanRole.MEMBER) return null;

        ClanRole oldRole = member.getRole();
        ClanRole newRole = oldRole.nextDown();
        member.setRole(newRole);
        try {
            db.updateMemberRole(target, newRole);
            return newRole;
        } catch (Exception e) {
            member.setRole(oldRole);
            plugin.getLogger().log(Level.SEVERE, "Errore nella retrocessione", e);
            return null;
        }
    }

    public void createInvite(Clan clan, Player inviter, Player invitee) {
        int expirySec = plugin.getPluginConfig().getInviteExpiry();
        Instant expires = Instant.now().plusSeconds(expirySec);
        PendingInvite invite = new PendingInvite(clan.getId(), inviter.getUniqueId(), expires);

        pendingInvites.put(invitee.getUniqueId(), invite);
        db.insertInviteAsync(clan.getId(), inviter.getUniqueId(), invitee.getUniqueId(), expires);

        MessageUtil.send(invitee, plugin.getPluginConfig().getMessage("invite-received")
                .replace("{clan}", clan.getName()));
    }

    public Clan acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) return null;

        Clan clan = getClanById(invite.getClanId());
        if (clan == null) return null;

        db.deleteInvite(player.getUniqueId());
        if (!addMember(clan, player)) return null;
        return clan;
    }

    public boolean declineInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null) return false;
        db.deleteInvite(player.getUniqueId());
        return true;
    }

    public PendingInvite getInvite(UUID playerUuid) { return pendingInvites.get(playerUuid); }

    public Clan getClanById(int id)       { return clansById.get(id); }
    public Clan getClanByName(String name) {
        Integer id = clansByName.get(name.toLowerCase());
        return id != null ? clansById.get(id) : null;
    }
    public Clan getClanByTag(String tag) {
        Integer id = clansByTag.get(tag.toLowerCase());
        return id != null ? clansById.get(id) : null;
    }
    public Clan getPlayerClan(UUID playerUuid) {
        Integer id = playerClanMap.get(playerUuid);
        return id != null ? clansById.get(id) : null;
    }

    public boolean isNameTaken(String name) { return clansByName.containsKey(name.toLowerCase()); }
    public boolean isTagTaken(String tag)   { return clansByTag.containsKey(tag.toLowerCase()); }

    public Collection<Clan> getAllClans() {
        return Collections.unmodifiableCollection(clansById.values());
    }

    public void shutdown() {
        pendingInvites.clear();
        clansById.clear();
        clansByName.clear();
        clansByTag.clear();
        playerClanMap.clear();
    }

    public static class PendingInvite {
        private final int     clanId;
        private final UUID    inviterUuid;
        private final Instant expires;

        public PendingInvite(int clanId, UUID inviterUuid, Instant expires) {
            this.clanId      = clanId;
            this.inviterUuid = inviterUuid;
            this.expires     = expires;
        }

        public int     getClanId()      { return clanId; }
        public UUID    getInviterUuid() { return inviterUuid; }
        public Instant getExpires()     { return expires; }
        public boolean isExpired()      { return Instant.now().isAfter(expires); }
    }
}
