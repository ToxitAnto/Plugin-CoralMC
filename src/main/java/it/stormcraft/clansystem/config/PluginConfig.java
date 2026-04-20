package it.stormcraft.clansystem.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final FileConfiguration cfg;

    public PluginConfig(FileConfiguration cfg) { this.cfg = cfg; }

    public String getDbHost()     { return cfg.getString("database.host",     "localhost"); }
    public int    getDbPort()     { return cfg.getInt("database.port",         3306); }
    public String getDbName()     { return cfg.getString("database.name",     "clansystem"); }
    public String getDbUsername() { return cfg.getString("database.username", "root"); }
    public String getDbPassword() { return cfg.getString("database.password", ""); }

    public int  getPoolMaxSize()           { return cfg.getInt("database.pool.maximum-pool-size",    10); }
    public int  getPoolMinIdle()           { return cfg.getInt("database.pool.minimum-idle",          2); }
    public long getPoolConnectionTimeout() { return cfg.getLong("database.pool.connection-timeout", 30000L); }
    public long getPoolIdleTimeout()       { return cfg.getLong("database.pool.idle-timeout",      600000L); }
    public long getPoolMaxLifetime()       { return cfg.getLong("database.pool.max-lifetime",     1800000L); }

    public int  getNameMinLength()  { return cfg.getInt("clan.name-min-length", 3); }
    public int  getNameMaxLength()  { return cfg.getInt("clan.name-max-length", 20); }
    public int  getTagMinLength()   { return cfg.getInt("clan.tag-min-length",  2); }
    public int  getTagMaxLength()   { return cfg.getInt("clan.tag-max-length",  5); }
    public int  getMaxMembers()     { return cfg.getInt("clan.max-members",    30); }
    public int  getMaxTerritories() { return cfg.getInt("clan.max-territories", 10); }
    public int  getInviteExpiry()   { return cfg.getInt("clan.invite-expiry-seconds", 60); }
    public boolean isDefaultPvpInTerritory() { return cfg.getBoolean("clan.default-pvp-in-territory", false); }
    public boolean isDefaultMobSpawning()    { return cfg.getBoolean("clan.default-mob-spawning", true); }
    public int  getHomeTeleportDelay()       { return cfg.getInt("clan.home-teleport-delay", 3); }
    public int  getHomeTeleportCooldown()    { return cfg.getInt("clan.home-teleport-cooldown", 60); }

    public String getClanChatFormat()   { return cfg.getString("chat.clan-chat-format", "&8[&6{clan_tag}&8] &f{player}&7: &e{message}"); }
    public String getGlobalChatPrefix() { return cfg.getString("chat.global-chat-prefix", "&8[&6{clan_tag}&8] "); }

    public boolean isUseWorldGuard()            { return cfg.getBoolean("territory.use-worldguard", true); }
    public boolean isPreventBuildByOutsiders()  { return cfg.getBoolean("territory.protection.prevent-build-by-outsiders", true); }
    public boolean isPreventPvpByOutsiders()    { return cfg.getBoolean("territory.protection.prevent-pvp-by-outsiders", true); }
    public boolean isPreventMobDamageToBlocks() { return cfg.getBoolean("territory.protection.prevent-mob-damage-to-blocks", true); }

    public String getPrefix() { return cfg.getString("messages.prefix", "&8[&6Clan&8] &r"); }

    public String getMessage(String key) {
        String raw = cfg.getString("messages." + key, "&cMessaggio non configurato: " + key);
        return raw.replace("&", "§");
    }
}
