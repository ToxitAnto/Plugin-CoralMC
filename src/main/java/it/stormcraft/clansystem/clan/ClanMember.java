package it.stormcraft.clansystem.clan;

import java.time.Instant;
import java.util.UUID;

public class ClanMember {

    private final UUID      playerUuid;
    private final String    playerName;
    private final int       clanId;
    private       ClanRole  role;
    private final Instant   joinedAt;

    public ClanMember(UUID playerUuid, String playerName, int clanId,
                      ClanRole role, Instant joinedAt) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.clanId     = clanId;
        this.role       = role;
        this.joinedAt   = joinedAt;
    }

    public UUID     getPlayerUuid() { return playerUuid; }
    public String   getPlayerName() { return playerName; }
    public int      getClanId()     { return clanId; }
    public ClanRole getRole()       { return role; }
    public Instant  getJoinedAt()   { return joinedAt; }

    public void setRole(ClanRole role) { this.role = role; }

    public boolean isLeader()          { return role == ClanRole.LEADER; }
    public boolean isOfficerOrAbove()  { return role.isAtLeast(ClanRole.OFFICER); }

    @Override
    public String toString() {
        return "ClanMember{uuid=" + playerUuid + ", name=" + playerName + ", role=" + role + "}";
    }
}
