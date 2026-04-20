package it.stormcraft.clansystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import it.stormcraft.clansystem.clan.ClanMember;
import it.stormcraft.clansystem.clan.ClanRole;
import it.stormcraft.clansystem.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final ClanSystem       plugin;
    private final PluginConfig     config;
    private       HikariDataSource dataSource;

    public DatabaseManager(ClanSystem plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }

    public void connect() {

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB driver non trovato nel JAR. Ricompila con maven-shade-plugin.", e);
        }

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mariadb:
                + config.getDbHost() + ":" + config.getDbPort()
                + "/" + config.getDbName()
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&characterEncoding=utf8&serverTimezone=UTC");
        hc.setUsername(config.getDbUsername());
        hc.setPassword(config.getDbPassword());

        hc.setMaximumPoolSize(config.getPoolMaxSize());
        hc.setMinimumIdle(config.getPoolMinIdle());
        hc.setConnectionTimeout(config.getPoolConnectionTimeout());
        hc.setIdleTimeout(config.getPoolIdleTimeout());
        hc.setMaxLifetime(config.getPoolMaxLifetime());
        hc.setPoolName("ClanSystem-Pool");

        hc.addDataSourceProperty("cachePrepStmts",       "true");
        hc.addDataSourceProperty("prepStmtCacheSize",     "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("useServerPrepStmts",    "true");

        dataSource = new HikariDataSource(hc);
        plugin.getLogger().info("Pool di connessioni DB creato.");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void runMigrations() {
        try (InputStream is = plugin.getResource("schema.sql")) {
            if (is == null) { plugin.getLogger().warning("schema.sql non trovato."); return; }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {

                        if (e.getErrorCode() == 1061 || e.getErrorCode() == 1050) {
                            plugin.getLogger().fine("Skipped already-existing schema object: " + e.getMessage());
                        } else {
                            throw e;
                        }
                    }
                }
            }
            plugin.getLogger().info("Migrazione schema DB completata.");
        } catch (IOException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la migrazione del DB", e);
            throw new RuntimeException(e);
        }
    }

    public List<Clan> loadAllClans() throws SQLException {
        List<Clan> list = new ArrayList<>();
        String sql = "SELECT id, name, tag, description, home_world, home_x, home_y, home_z, home_yaw, home_pitch, created_at FROM cs_clans";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapClan(rs));
        }
        return list;
    }

    public Clan insertClan(String name, String tag, UUID leaderUuid) throws SQLException {
        String sql = "INSERT INTO cs_clans (name, tag, leader_uuid) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, tag);
            ps.setString(3, leaderUuid.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new Clan(keys.getInt(1), name, tag, null, null, Instant.now());
            }
        }
        return null;
    }

    public void deleteClan(int clanId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cs_clans WHERE id = ?")) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
    }

    public void updateClanHome(int clanId, Location home) throws SQLException {
        String sql = "UPDATE cs_clans SET home_world=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, home.getWorld() != null ? home.getWorld().getName() : null);
            ps.setDouble(2, home.getX());
            ps.setDouble(3, home.getY());
            ps.setDouble(4, home.getZ());
            ps.setFloat(5,  home.getYaw());
            ps.setFloat(6,  home.getPitch());
            ps.setInt(7, clanId);
            ps.executeUpdate();
        }
    }

    public List<ClanMember> loadMembersForClan(int clanId) throws SQLException {
        List<ClanMember> list = new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, clan_id, role, joined_at FROM cs_members WHERE clan_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMember(rs));
            }
        }
        return list;
    }

    public void insertMember(ClanMember member) throws SQLException {
        String sql = "INSERT INTO cs_members (clan_id, player_uuid, player_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, member.getClanId());
            ps.setString(2, member.getPlayerUuid().toString());
            ps.setString(3, member.getPlayerName());
            ps.setString(4, member.getRole().name());
            ps.executeUpdate();
        }
    }

    public void deleteMember(UUID playerUuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cs_members WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateMemberRole(UUID playerUuid, ClanRole role) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE cs_members SET role = ? WHERE player_uuid = ?")) {
            ps.setString(1, role.name());
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    public void insertTerritory(int clanId, String world, int chunkX, int chunkZ) throws SQLException {
        String sql = "INSERT INTO cs_territories (clan_id, world, chunk_x, chunk_z) VALUES (?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
        }
    }

    public void deleteTerritory(String world, int chunkX, int chunkZ) throws SQLException {
        String sql = "DELETE FROM cs_territories WHERE world=? AND chunk_x=? AND chunk_z=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.executeUpdate();
        }
    }

    public int getTerritoryOwner(String world, int chunkX, int chunkZ) throws SQLException {
        String sql = "SELECT clan_id FROM cs_territories WHERE world=? AND chunk_x=? AND chunk_z=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("clan_id") : -1;
            }
        }
    }

    public List<int[]> getTerritoriesForClan(int clanId) throws SQLException {
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT chunk_x, chunk_z FROM cs_territories WHERE clan_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new int[]{rs.getInt("chunk_x"), rs.getInt("chunk_z")});
            }
        }
        return list;
    }

    public int getTerritoryCount(int clanId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cs_territories WHERE clan_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void deleteAllTerritories(int clanId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cs_territories WHERE clan_id = ?")) {
            ps.setInt(1, clanId);
            ps.executeUpdate();
        }
    }

    public Map<String, Integer> loadAllTerritories() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT clan_id, world, chunk_x, chunk_z FROM cs_territories";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString("world") + ":" + rs.getInt("chunk_x") + ":" + rs.getInt("chunk_z");
                map.put(key, rs.getInt("clan_id"));
            }
        }
        return map;
    }

    public void insertInviteAsync(int clanId, UUID inviterUuid, UUID inviteeUuid, Instant expires) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "INSERT INTO cs_invites (clan_id, inviter_uuid, invitee_uuid, expires_at) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at)";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, clanId);
                    ps.setString(2, inviterUuid.toString());
                    ps.setString(3, inviteeUuid.toString());
                    ps.setTimestamp(4, Timestamp.from(expires));
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Errore nell'inserimento dell'invito", e);
            }
        });
    }

    public void deleteInvite(UUID inviteeUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM cs_invites WHERE invitee_uuid = ?")) {
                ps.setString(1, inviteeUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Errore nella rimozione dell'invito", e);
            }
        });
    }

    public void cleanExpiredInvites() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cs_invites WHERE expires_at <= NOW()")) {
            int deleted = ps.executeUpdate();
            if (deleted > 0) plugin.getLogger().fine("Rimossi " + deleted + " inviti scaduti.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore nella pulizia degli inviti scaduti", e);
        }
    }

    private Clan mapClan(ResultSet rs) throws SQLException {
        int id      = rs.getInt("id");
        String name = rs.getString("name");
        String tag  = rs.getString("tag");
        String desc = rs.getString("description");

        Location home = null;
        String worldName = rs.getString("home_world");
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                home = new Location(world,
                        rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                        rs.getFloat("home_yaw"), rs.getFloat("home_pitch"));
            }
        }
        return new Clan(id, name, tag, desc, home, rs.getTimestamp("created_at").toInstant());
    }

    private ClanMember mapMember(ResultSet rs) throws SQLException {
        return new ClanMember(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getInt("clan_id"),
                ClanRole.valueOf(rs.getString("role")),
                rs.getTimestamp("joined_at").toInstant()
        );
    }
}
