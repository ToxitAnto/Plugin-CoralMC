package it.stormcraft.clansystem.clan;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

public class Clan {

    private final int            id;
    private final String         name;
    private final String         tag;
    private final Instant        createdAt;
    private       String         description;
    private       Location       home;

    private final Map<UUID, ClanMember> members = new HashMap<>();

    public Clan(int id, String name, String tag,
                String description, Location home, Instant createdAt) {
        this.id          = id;
        this.name        = name;
        this.tag         = tag;
        this.description = description;
        this.home        = home;
        this.createdAt   = createdAt;
    }

    public void addMember(ClanMember member)  { members.put(member.getPlayerUuid(), member); }
    public void removeMember(UUID uuid)       { members.remove(uuid); }
    public ClanMember getMember(UUID uuid)    { return members.get(uuid); }
    public boolean isMember(UUID uuid)        { return members.containsKey(uuid); }
    public int getMemberCount()               { return members.size(); }

    public Collection<ClanMember> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    public ClanMember getLeader() {
        return members.values().stream()
                .filter(ClanMember::isLeader)
                .findFirst()
                .orElse(null);
    }

    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (ClanMember m : members.values()) {
            Player p = Bukkit.getPlayer(m.getPlayerUuid());
            if (p != null && p.isOnline()) online.add(p);
        }
        return online;
    }

    public int getOnlineCount() { return getOnlineMembers().size(); }

    public int      getId()          { return id; }
    public String   getName()        { return name; }
    public String   getTag()         { return tag; }
    public Instant  getCreatedAt()   { return createdAt; }
    public String   getDescription() { return description; }
    public Location getHome()        { return home; }
    public boolean  hasHome()        { return home != null; }

    public void setDescription(String description) { this.description = description; }
    public void setHome(Location home)             { this.home = home; }

    @Override
    public String toString() {
        return "Clan{id=" + id + ", name='" + name + "', tag='" + tag + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Clan clan)) return false;
        return id == clan.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
