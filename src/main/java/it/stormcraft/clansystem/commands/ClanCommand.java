package it.stormcraft.clansystem.commands;

import it.stormcraft.clansystem.ClanSystem;
import it.stormcraft.clansystem.clan.Clan;
import it.stormcraft.clansystem.clan.ClanMember;
import it.stormcraft.clansystem.clan.ClanManager;
import it.stormcraft.clansystem.clan.ClanRole;
import it.stormcraft.clansystem.config.PluginConfig;
import it.stormcraft.clansystem.territory.TerritoryManager.ClaimResult;
import it.stormcraft.clansystem.utils.MessageUtil;
import it.stormcraft.clansystem.utils.ValidationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final ClanSystem    plugin;
    private final ClanManager   cm;
    private final PluginConfig  cfg;

    private final Map<UUID, Long> homeCooldowns = new HashMap<>();

    private final Map<UUID, Integer> teleportTasks = new HashMap<>();

    public ClanCommand(ClanSystem plugin) {
        this.plugin = plugin;
        this.cm     = plugin.getClanManager();
        this.cfg    = plugin.getPluginConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, cfg.getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create"  -> handleCreate(player, args);
            case "disband" -> handleDisband(player);
            case "invite"  -> handleInvite(player, args);
            case "accept"  -> handleAccept(player);
            case "decline" -> handleDecline(player);
            case "leave"   -> handleLeave(player);
            case "kick"    -> handleKick(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote"  -> handleDemote(player, args);
            case "chat", "c" -> handleChat(player, args);
            case "claim"   -> handleClaim(player);
            case "unclaim" -> handleUnclaim(player);
            case "home"    -> handleHome(player);
            case "sethome" -> handleSetHome(player);
            case "info"    -> handleInfo(player, args);
            case "help"    -> sendHelp(player);
            default        -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.create")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        if (args.length < 3) {
            MessageUtil.send(player, "§cUso: /clan create <nome> <tag>"); return;
        }

        String name = args[1];
        String tag  = args[2];

        ValidationUtil.NameResult nameResult = ValidationUtil.validateName(name, cfg);
        if (nameResult != ValidationUtil.NameResult.OK) {
            sendNameError(player, nameResult, "name"); return;
        }
        ValidationUtil.NameResult tagResult = ValidationUtil.validateTag(tag, cfg);
        if (tagResult != ValidationUtil.NameResult.OK) {
            sendNameError(player, tagResult, "tag"); return;
        }

        if (cm.getPlayerClan(player.getUniqueId()) != null) {
            MessageUtil.send(player, cfg.getMessage("already-in-clan")); return;
        }
        if (cm.isNameTaken(name)) {
            MessageUtil.send(player, cfg.getMessage("clan-already-exists")); return;
        }

        Clan clan = cm.createClan(player, name, tag);
        if (clan == null) {
            MessageUtil.send(player, cfg.getMessage("clan-already-exists")); return;
        }
        MessageUtil.send(player, cfg.getMessage("clan-created").replace("{clan}", clan.getName()));
    }

    private void handleDisband(Player player) {
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }

        String msg = cfg.getMessage("clan-disbanded").replace("{clan}", clan.getName());
        clan.getOnlineMembers().forEach(p -> MessageUtil.send(p, msg));
        cm.disbandClan(clan);
    }

    private void handleInvite(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.invite")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isOfficerOrAbove()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "§cUso: /clan invite <giocatore>"); return;
        }

        int maxMembers = cfg.getMaxMembers();
        if (maxMembers > 0 && clan.getMemberCount() >= maxMembers) {
            MessageUtil.send(player, cfg.getMessage("max-members-reached")); return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.send(player, cfg.getMessage("player-not-found")); return; }
        if (cm.getPlayerClan(target.getUniqueId()) != null) {
            MessageUtil.send(player, cfg.getMessage("target-in-clan")); return;
        }

        cm.createInvite(clan, player, target);
        MessageUtil.send(player, cfg.getMessage("invite-sent").replace("{player}", target.getName()));
    }

    private void handleAccept(Player player) {
        ClanManager.PendingInvite invite = cm.getInvite(player.getUniqueId());
        if (invite == null) { MessageUtil.send(player, cfg.getMessage("no-invite")); return; }
        if (invite.isExpired()) {
            cm.declineInvite(player);
            MessageUtil.send(player, cfg.getMessage("invite-expired")); return;
        }
        if (cm.getPlayerClan(player.getUniqueId()) != null) {
            cm.declineInvite(player);
            MessageUtil.send(player, cfg.getMessage("already-in-clan")); return;
        }

        Clan clan = cm.acceptInvite(player);
        if (clan == null) {
            MessageUtil.send(player, "§cIl clan non esiste più."); return;
        }

        String msg = cfg.getMessage("invite-accepted").replace("{player}", player.getName());
        clan.getOnlineMembers().forEach(p -> MessageUtil.send(p, msg));
    }

    private void handleDecline(Player player) {
        if (!cm.declineInvite(player)) {
            MessageUtil.send(player, cfg.getMessage("no-invite")); return;
        }
        MessageUtil.send(player, cfg.getMessage("invite-declined-self"));
    }

    private void handleLeave(Player player) {
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null) { MessageUtil.send(player, cfg.getMessage("not-in-clan")); return; }
        if (me.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("leader-cannot-leave")); return;
        }

        String playerName = player.getName();
        cm.removeMember(clan, player.getUniqueId());
        MessageUtil.send(player, cfg.getMessage("left-clan").replace("{clan}", clan.getName()));

        String notify = cfg.getMessage("member-left").replace("{player}", playerName);
        clan.getOnlineMembers().forEach(p -> MessageUtil.send(p, notify));
    }

    private void handleKick(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.kick")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isOfficerOrAbove()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "§cUso: /clan kick <giocatore>"); return;
        }

        ClanMember target = findMemberByName(clan, args[1]);
        if (target == null) { MessageUtil.send(player, cfg.getMessage("player-not-found")); return; }
        if (target.getPlayerUuid().equals(player.getUniqueId())) {
            MessageUtil.send(player, cfg.getMessage("cannot-target-self")); return;
        }
        if (target.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("cannot-kick-leader")); return;
        }
        if (target.getRole().isHigherThan(me.getRole())) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }

        String targetName = target.getPlayerName();
        cm.removeMember(clan, target.getPlayerUuid());

        Player onlineTarget = Bukkit.getPlayer(target.getPlayerUuid());
        if (onlineTarget != null) MessageUtil.send(onlineTarget, cfg.getMessage("kicked"));

        String kickMsg = cfg.getMessage("kick-success").replace("{player}", targetName);
        MessageUtil.send(player, kickMsg);
        clan.getOnlineMembers().stream()
                .filter(p -> !p.equals(player))
                .forEach(p -> MessageUtil.send(p, kickMsg));
    }

    private void handlePromote(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.promote")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }
        if (args.length < 2) { MessageUtil.send(player, "§cUso: /clan promote <giocatore>"); return; }

        ClanMember target = findMemberByName(clan, args[1]);
        if (target == null) { MessageUtil.send(player, cfg.getMessage("player-not-found")); return; }
        if (target.getPlayerUuid().equals(player.getUniqueId())) {
            MessageUtil.send(player, cfg.getMessage("cannot-target-self")); return;
        }
        if (target.getRole() != ClanRole.MEMBER) {
            MessageUtil.send(player, cfg.getMessage("already-officer")
                    .replace("{player}", target.getPlayerName())); return;
        }

        ClanRole newRole = cm.promoteMember(clan, target.getPlayerUuid());
        if (newRole == null) { MessageUtil.send(player, "§cErrore durante la promozione."); return; }

        String msg = cfg.getMessage("promoted")
                .replace("{player}", target.getPlayerName())
                .replace("{role}", newRole.getDisplayName());
        clan.getOnlineMembers().forEach(p -> MessageUtil.send(p, msg));
    }

    private void handleDemote(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.promote")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }
        if (args.length < 2) { MessageUtil.send(player, "§cUso: /clan demote <giocatore>"); return; }

        ClanMember target = findMemberByName(clan, args[1]);
        if (target == null) { MessageUtil.send(player, cfg.getMessage("player-not-found")); return; }
        if (target.getPlayerUuid().equals(player.getUniqueId())) {
            MessageUtil.send(player, cfg.getMessage("cannot-target-self")); return;
        }
        if (target.getRole() == ClanRole.MEMBER) {
            MessageUtil.send(player, cfg.getMessage("already-member")
                    .replace("{player}", target.getPlayerName())); return;
        }

        ClanRole newRole = cm.demoteMember(clan, target.getPlayerUuid());
        if (newRole == null) { MessageUtil.send(player, "§cErrore durante la retrocessione."); return; }

        String msg = cfg.getMessage("demoted")
                .replace("{player}", target.getPlayerName())
                .replace("{role}", newRole.getDisplayName());
        clan.getOnlineMembers().forEach(p -> MessageUtil.send(p, msg));
    }

    private void handleChat(Player player, String[] args) {
        if (!player.hasPermission("clansystem.clan.chat")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        if (args.length < 2) { MessageUtil.send(player, "§cUso: /clan chat <messaggio>"); return; }

        ClanMember me = clan.getMember(player.getUniqueId());
        String role = me != null ? me.getRole().getDisplayName() : "";

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String formatted = cfg.getClanChatFormat()
                .replace("&", "§")
                .replace("{clan_tag}", clan.getTag())
                .replace("{clan_name}", clan.getName())
                .replace("{role}", role)
                .replace("{player}", player.getName())
                .replace("{message}", message);

        clan.getOnlineMembers().forEach(p -> p.sendMessage(formatted));

        plugin.getLogger().info("[CLAN-CHAT][" + clan.getTag() + "] " + player.getName() + ": " + message);
    }

    private void handleClaim(Player player) {
        if (!player.hasPermission("clansystem.clan.claim")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isOfficerOrAbove()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }

        ClaimResult result = plugin.getTerritoryManager().claimChunk(clan, player);
        switch (result) {
            case SUCCESS              -> MessageUtil.send(player, cfg.getMessage("territory-claimed"));
            case ALREADY_CLAIMED_BY_SELF -> MessageUtil.send(player, cfg.getMessage("territory-already-claimed"));
            case CLAIMED_BY_OTHER     -> MessageUtil.send(player, cfg.getMessage("territory-not-yours"));
            case LIMIT_REACHED        -> MessageUtil.send(player, cfg.getMessage("territory-limit-reached"));
            default                   -> MessageUtil.send(player, "§cErrore durante il claim.");
        }
    }

    private void handleUnclaim(Player player) {
        if (!player.hasPermission("clansystem.clan.claim")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isOfficerOrAbove()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }

        ClaimResult result = plugin.getTerritoryManager().unclaimChunk(clan, player);
        switch (result) {
            case SUCCESS          -> MessageUtil.send(player, cfg.getMessage("territory-unclaimed"));
            case NOT_CLAIMED      -> MessageUtil.send(player, "§cQuesto territorio non è reclamato.");
            case CLAIMED_BY_OTHER -> MessageUtil.send(player, cfg.getMessage("territory-not-yours"));
            default               -> MessageUtil.send(player, "§cErrore durante l'unclaim.");
        }
    }

    private void handleHome(Player player) {
        if (!player.hasPermission("clansystem.clan.home")) {
            MessageUtil.send(player, cfg.getMessage("no-permission")); return;
        }
        Clan clan = requireClan(player); if (clan == null) return;
        if (!clan.hasHome()) { MessageUtil.send(player, cfg.getMessage("home-not-set")); return; }

        int cooldownSec = cfg.getHomeTeleportCooldown();
        if (cooldownSec > 0) {
            long lastUse = homeCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long remaining = cooldownSec - (System.currentTimeMillis() - lastUse) / 1000;
            if (remaining > 0) {
                MessageUtil.send(player, cfg.getMessage("home-cooldown")
                        .replace("{seconds}", String.valueOf(remaining)));
                return;
            }
        }

        int delaySec = cfg.getHomeTeleportDelay();
        if (delaySec <= 0) {
            teleportHome(player, clan);
        } else {
            MessageUtil.send(player, cfg.getMessage("home-teleporting"));
            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teleportTasks.remove(player.getUniqueId());
                teleportHome(player, clan);
            }, 20L * delaySec).getTaskId();
            teleportTasks.put(player.getUniqueId(), taskId);
        }
    }

    public void cancelTeleport(UUID playerUuid) {
        Integer taskId = teleportTasks.remove(playerUuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    private void teleportHome(Player player, Clan clan) {
        player.teleport(clan.getHome());
        homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        MessageUtil.send(player, cfg.getMessage("home-teleported"));
    }

    private void handleSetHome(Player player) {
        Clan clan = requireClan(player); if (clan == null) return;
        ClanMember me = clan.getMember(player.getUniqueId());
        if (me == null || !me.isLeader()) {
            MessageUtil.send(player, cfg.getMessage("not-enough-rank")); return;
        }

        clan.setHome(player.getLocation());
        try {
            plugin.getDatabaseManager().updateClanHome(clan.getId(), player.getLocation());
        } catch (Exception e) {
            plugin.getLogger().warning("Errore nel salvataggio della home: " + e.getMessage());
        }
        MessageUtil.send(player, cfg.getMessage("home-set"));
    }

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        if (args.length >= 2) {
            clan = cm.getClanByName(args[1]);
            if (clan == null) clan = cm.getClanByTag(args[1]);
            if (clan == null) { MessageUtil.send(player, "§cClan non trovato."); return; }
        } else {
            clan = cm.getPlayerClan(player.getUniqueId());
            if (clan == null) { MessageUtil.send(player, cfg.getMessage("not-in-clan")); return; }
        }

        ClanMember leader = clan.getLeader();
        player.sendMessage("§8§m-----------------------");
        player.sendMessage("§6 " + clan.getName() + " §7[§e" + clan.getTag() + "§7]");
        player.sendMessage("§7 Leader: §f" + (leader != null ? leader.getPlayerName() : "N/A"));
        player.sendMessage("§7 Membri: §f" + clan.getMemberCount()
                + " §7(Online: §a" + clan.getOnlineCount() + "§7)");
        player.sendMessage("§7 Territorio: §f" + getTerritoryCount(clan) + " chunk");
        if (clan.getDescription() != null) {
            player.sendMessage("§7 Descrizione: §f" + clan.getDescription());
        }
        player.sendMessage("§7 Creato: §f" + clan.getCreatedAt().toString().substring(0, 10));
        player.sendMessage("§8§m-----------------------");
    }

    private void sendHelp(Player player) {
        player.sendMessage(cfg.getMessage("help-header"));
        player.sendMessage("§e/clan create §7<nome> <tag> §f- Crea un clan");
        player.sendMessage("§e/clan disband §f- Sciogli il tuo clan");
        player.sendMessage("§e/clan invite §7<giocatore> §f- Invita un giocatore");
        player.sendMessage("§e/clan accept §f- Accetta un invito");
        player.sendMessage("§e/clan decline §f- Rifiuta un invito");
        player.sendMessage("§e/clan leave §f- Abbandona il clan");
        player.sendMessage("§e/clan kick §7<giocatore> §f- Espelli un membro");
        player.sendMessage("§e/clan promote §7<giocatore> §f- Promuovi un membro");
        player.sendMessage("§e/clan demote §7<giocatore> §f- Retrocedi un membro");
        player.sendMessage("§e/clan chat §7<messaggio> §f- Chat clan");
        player.sendMessage("§e/clan claim §f- Reclama territorio");
        player.sendMessage("§e/clan unclaim §f- Libera territorio");
        player.sendMessage("§e/clan home §f- Teletrasportati alla home");
        player.sendMessage("§e/clan sethome §f- Imposta home clan");
        player.sendMessage("§e/clan info §7[clan] §f- Info clan");
        player.sendMessage(cfg.getMessage("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create", "disband", "invite", "accept", "decline", "leave",
                    "kick", "promote", "demote", "chat", "claim",
                    "unclaim", "home", "sethome", "info", "help"
            );
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "invite", "kick", "promote", "demote" -> onlinePlayers(args[1]);
                case "info" -> {
                    List<String> names = new ArrayList<>();
                    cm.getAllClans().forEach(c -> names.add(c.getName()));
                    yield names.stream().filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                }
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    private Clan requireClan(Player player) {
        Clan clan = cm.getPlayerClan(player.getUniqueId());
        if (clan == null) MessageUtil.send(player, cfg.getMessage("not-in-clan"));
        return clan;
    }

    private ClanMember findMemberByName(Clan clan, String name) {
        return clan.getMembers().stream()
                .filter(m -> m.getPlayerName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    private int getTerritoryCount(Clan clan) {
        return plugin.getTerritoryManager().getCachedTerritoryCount(clan.getId());
    }

    private void sendNameError(Player player, ValidationUtil.NameResult result, String type) {
        switch (result) {
            case TOO_SHORT -> MessageUtil.send(player, cfg.getMessage(type + "-too-short")
                    .replace("{min}", type.equals("tag")
                            ? String.valueOf(cfg.getTagMinLength())
                            : String.valueOf(cfg.getNameMinLength())));
            case TOO_LONG  -> MessageUtil.send(player, cfg.getMessage(type + "-too-long")
                    .replace("{max}", type.equals("tag")
                            ? String.valueOf(cfg.getTagMaxLength())
                            : String.valueOf(cfg.getNameMaxLength())));
            case INVALID   -> MessageUtil.send(player, cfg.getMessage("invalid-name"));
            default        -> {}
        }
    }
}
