package it.stormcraft.clansystem.territory;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import it.stormcraft.clansystem.clan.ClanMember;
import it.stormcraft.clansystem.database.DatabaseManager;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TerritoryManager {

    private final ClanSystem      plugin;
    private final DatabaseManager db;

    private final Map<String, Integer> chunkCache = new ConcurrentHashMap<>();

    public TerritoryManager(ClanSystem plugin) {
        this.plugin = plugin;
        this.db     = plugin.getDatabaseManager();
        preloadCache();
    }

    private void preloadCache() {
        try {
            Map<String, Integer> loaded = db.loadAllTerritories();
            chunkCache.putAll(loaded);
            plugin.getLogger().info("Caricati " + loaded.size() + " chunk di territorio nel cache.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore nel precaricamento dei territori", e);
        }
    }

    public ClaimResult claimChunk(Clan clan, Player player) {
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        int cx = chunk.getX(), cz = chunk.getZ();

        int existingOwner = getChunkOwnerFromCache(world.getName(), cx, cz);
        if (existingOwner != -1) {
            return existingOwner == clan.getId()
                    ? ClaimResult.ALREADY_CLAIMED_BY_SELF
                    : ClaimResult.CLAIMED_BY_OTHER;
        }

        int maxTerr = plugin.getPluginConfig().getMaxTerritories();
        if (maxTerr > 0) {

            long count = chunkCache.values().stream().filter(id -> id == clan.getId()).count();
            if (count >= maxTerr) return ClaimResult.LIMIT_REACHED;
        }

        try {
            db.insertTerritory(clan.getId(), world.getName(), cx, cz);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore nel claim del territorio", e);
            return ClaimResult.ERROR;
        }

        chunkCache.put(cacheKey(world.getName(), cx, cz), clan.getId());

        if (plugin.isWorldGuardEnabled() && plugin.getPluginConfig().isUseWorldGuard()) {
            createWorldGuardRegion(clan, world, chunk);
        }

        return ClaimResult.SUCCESS;
    }

    public ClaimResult unclaimChunk(Clan clan, Player player) {
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        int cx = chunk.getX(), cz = chunk.getZ();

        int owner = getChunkOwnerFromCache(world.getName(), cx, cz);
        if (owner == -1)           return ClaimResult.NOT_CLAIMED;
        if (owner != clan.getId()) return ClaimResult.CLAIMED_BY_OTHER;

        try {
            db.deleteTerritory(world.getName(), cx, cz);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore nell'unclaim del territorio", e);
            return ClaimResult.ERROR;
        }

        chunkCache.remove(cacheKey(world.getName(), cx, cz));

        if (plugin.isWorldGuardEnabled() && plugin.getPluginConfig().isUseWorldGuard()) {
            removeWorldGuardRegion(clan, world, chunk);
        }

        return ClaimResult.SUCCESS;
    }

    public int getChunkOwnerFromCache(String worldName, int chunkX, int chunkZ) {
        return chunkCache.getOrDefault(cacheKey(worldName, chunkX, chunkZ), -1);
    }

    public int getChunkOwner(String worldName, int chunkX, int chunkZ) {
        return getChunkOwnerFromCache(worldName, chunkX, chunkZ);
    }

    public boolean canBuild(Player player, String worldName, int chunkX, int chunkZ) {
        if (player.hasPermission("clansystem.admin")) return true;
        int owner = getChunkOwnerFromCache(worldName, chunkX, chunkZ);
        if (owner == -1) return true;
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        return clan != null && clan.getId() == owner;
    }

    public boolean canPvP(Player attacker, Player defender, String worldName, int chunkX, int chunkZ) {
        int owner = getChunkOwnerFromCache(worldName, chunkX, chunkZ);
        if (owner == -1) return true;
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());
        Clan defenderClan = plugin.getClanManager().getPlayerClan(defender.getUniqueId());

        if (attackerClan != null && defenderClan != null
                && attackerClan.getId() == defenderClan.getId()) return false;
        return plugin.getPluginConfig().isDefaultPvpInTerritory();
    }

    public void removeAllRegions(Clan clan) {
        chunkCache.entrySet().removeIf(e -> e.getValue() == clan.getId());

        if (!plugin.isWorldGuardEnabled() || !plugin.getPluginConfig().isUseWorldGuard()) return;

        for (World world : plugin.getServer().getWorlds()) {
            RegionManager rm = getRegionManager(world);
            if (rm == null) continue;
            rm.getRegions().keySet().stream()
                    .filter(id -> id.startsWith("clan_" + clan.getId() + "_"))
                    .toList()
                    .forEach(rm::removeRegion);
        }
    }

    public int getCachedTerritoryCount(int clanId) {
        return (int) chunkCache.values().stream().filter(id -> id == clanId).count();
    }

    private void createWorldGuardRegion(Clan clan, World world, Chunk chunk) {
        try {
            RegionManager rm = getRegionManager(world);
            if (rm == null) return;

            String regionId = wgRegionId(clan.getId(), world.getName(), chunk.getX(), chunk.getZ());
            com.sk89q.worldedit.math.BlockVector3 min =
                    com.sk89q.worldedit.math.BlockVector3.at(chunk.getX() * 16,      -64, chunk.getZ() * 16);
            com.sk89q.worldedit.math.BlockVector3 max =
                    com.sk89q.worldedit.math.BlockVector3.at(chunk.getX() * 16 + 15, 320, chunk.getZ() * 16 + 15);

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
            region.setFlag(Flags.BUILD,        StateFlag.State.DENY);
            region.setFlag(Flags.PVP,          StateFlag.State.DENY);
            region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.ALLOW);

            com.sk89q.worldguard.domains.DefaultDomain domain = new com.sk89q.worldguard.domains.DefaultDomain();
            for (ClanMember m : clan.getMembers()) domain.addPlayer(m.getPlayerUuid());
            region.setMembers(domain);
            region.setOwners(domain);

            rm.addRegion(region);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Errore nella creazione della regione WG", e);
        }
    }

    private void removeWorldGuardRegion(Clan clan, World world, Chunk chunk) {
        try {
            RegionManager rm = getRegionManager(world);
            if (rm == null) return;
            rm.removeRegion(wgRegionId(clan.getId(), world.getName(), chunk.getX(), chunk.getZ()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Errore nella rimozione della regione WG", e);
        }
    }

    private RegionManager getRegionManager(World world) {
        try {
            return WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
        } catch (Exception e) {
            return null;
        }
    }

    private String cacheKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }

    private String wgRegionId(int clanId, String world, int cx, int cz) {
        return "clan_" + clanId + "_" + world.toLowerCase() + "_" + cx + "_" + cz;
    }

    public enum ClaimResult {
        SUCCESS, ALREADY_CLAIMED_BY_SELF, CLAIMED_BY_OTHER, NOT_CLAIMED, LIMIT_REACHED, ERROR
    }
}
