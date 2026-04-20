package it.stormcraft.clansystem.clan;

public enum ClanRole {

    LEADER(3,  "§6Leader"),
    OFFICER(2, "§eOfficer"),
    MEMBER(1,  "§7Member");

    private final int    weight;
    private final String displayName;

    ClanRole(int weight, String displayName) {
        this.weight      = weight;
        this.displayName = displayName;
    }

    public int    getWeight()      { return weight; }
    public String getDisplayName() { return displayName; }

    public boolean isHigherThan(ClanRole other) { return this.weight > other.weight; }
    public boolean isAtLeast(ClanRole other)     { return this.weight >= other.weight; }

    public ClanRole nextUp() {
        return switch (this) {
            case MEMBER  -> OFFICER;
            case OFFICER -> LEADER;
            default      -> this;
        };
    }

    public ClanRole nextDown() {
        return switch (this) {
            case LEADER  -> OFFICER;
            case OFFICER -> MEMBER;
            default      -> this;
        };
    }
}
