package it.stormcraft.clansystem;

import it.stormcraft.clansystem.clan.ClanManager;
import it.stormcraft.clansystem.commands.ClanCommand;
import it.stormcraft.clansystem.config.PluginConfig;
import it.stormcraft.clansystem.database.DatabaseManager;
import it.stormcraft.clansystem.listeners.ChatListener;
import it.stormcraft.clansystem.listeners.PlayerListener;
import it.stormcraft.clansystem.listeners.TerritoryListener;
import it.stormcraft.clansystem.placeholders.ClanPlaceholders;
import it.stormcraft.clansystem.territory.TerritoryManager;
import it.stormcraft.clansystem.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class ClanSystem extends JavaPlugin {

    private static ClanSystem instance;

    private PluginConfig     pluginConfig;
    private DatabaseManager  databaseManager;
    private ClanManager      clanManager;
    private TerritoryManager territoryManager;
    private ClanCommand      clanCommand;

    private boolean worldGuardEnabled     = false;
    private boolean placeholderApiEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        getLogger().info("╔════════════════════════════════╗");
        getLogger().info("║     ClanSystem v" + getDescription().getVersion() + "         ║");
        getLogger().info("║          by CkAnto              ║");
        getLogger().info("╚════════════════════════════════╝");

        if (!loadConfig()) {
            getLogger().severe("Configurazione non valida, disabilitazione plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!initDatabase()) {
            getLogger().severe("Connessione al database fallita, disabilitazione plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        clanManager      = new ClanManager(this);
        territoryManager = new TerritoryManager(this);

        registerCommands();
        registerListeners();
        hookDependencies();
        scheduleTasks();

        getLogger().info("Plugin avviato in " + (System.currentTimeMillis() - start) + "ms.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Chiusura ClanSystem...");
        if (clanManager != null)     clanManager.shutdown();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("ClanSystem disabilitato.");
    }

    private boolean loadConfig() {
        saveDefaultConfig();
        try {
            pluginConfig = new PluginConfig(getConfig());
            MessageUtil.setPrefix(pluginConfig.getPrefix());
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Errore nel caricamento della configurazione", e);
            return false;
        }
    }

    private boolean initDatabase() {
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
            databaseManager.runMigrations();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Errore database", e);
            return false;
        }
    }

    private void registerCommands() {
        clanCommand = new ClanCommand(this);
        var cmd = getCommand("clan");
        if (cmd != null) {
            cmd.setExecutor(clanCommand);
            cmd.setTabCompleter(clanCommand);
        }
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this, clanCommand), this);
        pm.registerEvents(new ChatListener(this),      this);
        pm.registerEvents(new TerritoryListener(this), this);
    }

    private void hookDependencies() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard trovato – integrazione attiva.");
        } else {
            getLogger().info("WorldGuard non trovato – uso sistema territori built-in.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholders(this).register();
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI trovato – placeholders registrati.");
        }
    }

    private void scheduleTasks() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> databaseManager.cleanExpiredInvites(),
                20L * 60,
                20L * 30
        );
    }

    public static ClanSystem getInstance()       { return instance; }
    public PluginConfig    getPluginConfig()      { return pluginConfig; }
    public DatabaseManager getDatabaseManager()   { return databaseManager; }
    public ClanManager     getClanManager()       { return clanManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public boolean isWorldGuardEnabled()          { return worldGuardEnabled; }
    public boolean isPlaceholderApiEnabled()      { return placeholderApiEnabled; }
}
