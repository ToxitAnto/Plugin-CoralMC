<div align="center">

<img src="https://img.shields.io/badge/Minecraft-1.21.x-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft 1.21.x"/>
<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java 17+"/>
<img src="https://img.shields.io/badge/Spigot-1.21.1-yellow?style=for-the-badge" alt="Spigot"/>
<img src="https://img.shields.io/badge/MariaDB-Required-blue?style=for-the-badge&logo=mariadb" alt="MariaDB"/>
<img src="https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge" alt="License"/>

# 🏰 ClanSystem

**Plugin Bukkit/Spigot per la gestione avanzata di clan e territori**  
Sviluppato per **CoralMC** da [ckanto](https://github.com/ckanto)

[📦 Download](#-installazione) · [📖 Wiki](#-comandi) · [🐛 Bug Report](https://github.com/ckanto/ClanSystem/issues) · [💬 Discord](#)

</div>

---

## 📋 Indice

- [Panoramica](#-panoramica)
- [Funzionalità](#-funzionalità)
- [Requisiti](#-requisiti)
- [Installazione](#-installazione)
- [Configurazione Database](#-configurazione-database)
- [Configurazione Plugin](#%EF%B8%8F-configurazione-plugin)
- [Comandi](#-comandi)
- [Permessi](#-permessi)
- [PlaceholderAPI](#-placeholderapi)
- [Schema Database](#-schema-database)
- [Struttura Progetto](#-struttura-progetto)
- [Build dal Sorgente](#-build-dal-sorgente)

---

## 🌊 Panoramica

ClanSystem è un plugin completo e performante per la gestione di **clan** e **territori** su server Minecraft Spigot/Bukkit. Progettato con un'architettura modulare e scalabile, offre un sistema di gestione ruoli differenziato, protezione dei territori tramite WorldGuard e integrazione nativa con PlaceholderAPI.

Il plugin utilizza **MariaDB** con connection pooling HikariCP per garantire prestazioni elevate anche sotto carichi importanti, ed è pensato per integrarsi perfettamente nell'ecosistema di **CoralMC**.

---

## ✨ Funzionalità

### 👥 Sistema Clan
- Creazione e scioglimento clan con nome personalizzato e tag univoco
- Gestione membri con tre ruoli distinti: **Leader**, **Officer**, **Member**
- Sistema di inviti con scadenza configurabile e conferma esplicita
- Espulsione membri con controlli di permesso basati sul ruolo
- Promozione e retrocessione con gerarchia rispettata
- Chat clan privata separata dalla chat globale con formato personalizzabile
- Notifiche di connessione/disconnessione dei membri

### 🗺️ Sistema Territori
- Claim di territori per chunk con supporto WorldGuard nativo
- Protezioni automatiche configurabili: build, PvP, mob spawning
- Sistema di home clan con teletrasporto ritardato e cooldown anti-exploit
- Limite massimo di territori per clan configurabile
- Visualizzazione delle informazioni sul territorio in tempo reale

### 🗄️ Database
- Backend **MariaDB** con pool di connessioni **HikariCP**
- Schema ottimizzato con indici, chiavi univoche e foreign key a cascata
- Tabelle: `cs_clans`, `cs_members`, `cs_territories`, `cs_invites`
- Pulizia automatica degli inviti scaduti

### 🔌 Integrazioni
- **PlaceholderAPI** — 4 placeholder pronti all'uso
- **WorldGuard** — protezione territori tramite regioni native (soft-depend)
- Compatibile con sistemi di rank/chat esterni

---

## 📦 Requisiti

| Dipendenza | Versione | Tipo |
|---|---|---|
| Java | 17+ | Obbligatoria |
| Spigot / Paper | 1.21.1 | Obbligatoria |
| MariaDB | 10.6+ | Obbligatoria |
| PlaceholderAPI | 2.11.5+ | Consigliata |
| WorldGuard | 7.0.9+ | Opzionale |
| WorldEdit | 7.2.15+ | Opzionale (richiesta da WorldGuard) |

---

## 🚀 Installazione

1. **Scarica** il file `ClanSystem-x.x.x.jar` dalla sezione [Releases](https://github.com/ckanto/ClanSystem/releases).
2. **Copia** il JAR nella cartella `plugins/` del tuo server.
3. **Assicurati** che MariaDB sia attivo e raggiungibile dal server.
4. **Avvia** il server per generare i file di configurazione.
5. **Modifica** `plugins/ClanSystem/config.yml` con le credenziali del tuo database.
6. **Riavvia** il server per applicare la configurazione.

> **Nota:** Se WorldGuard e WorldEdit sono presenti, verranno rilevati automaticamente per la protezione dei territori. In assenza, il plugin userà il sistema custom interno.

---

## 🗄️ Configurazione Database

Prima di avviare il plugin, crea il database su MariaDB:

```sql
CREATE DATABASE clansystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'clansystem'@'localhost' IDENTIFIED BY 'password_sicura';
GRANT ALL PRIVILEGES ON clansystem.* TO 'clansystem'@'localhost';
FLUSH PRIVILEGES;
```

Le tabelle vengono create automaticamente all'avvio del plugin. In alternativa, puoi importare manualmente lo schema:

```bash
mysql -u clansystem -p clansystem < schema.sql
```

---

## ⚙️ Configurazione Plugin

Il file `config.yml` viene generato automaticamente in `plugins/ClanSystem/`. Di seguito le sezioni principali:

```yaml
database:
  host: "localhost"
  port: 3306
  name: "clansystem"
  username: "clansystem"
  password: "password_sicura"
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000

clan:
  name-min-length: 3
  name-max-length: 20
  tag-min-length: 2
  tag-max-length: 5
  max-members: 30
  max-territories: 10
  invite-expiry-seconds: 60
  home-teleport-delay: 3       # secondi prima del teletrasporto (0 = istantaneo)
  home-teleport-cooldown: 60   # cooldown in secondi tra un /clan home e il successivo

territory:
  use-worldguard: true         # false = sistema custom interno
  claim-by-chunk: true
  protection:
    prevent-build-by-outsiders: true
    prevent-pvp-by-outsiders: true
    prevent-mob-damage-to-blocks: true

chat:
  clan-chat-format: "&8[&6{clan_tag}&8] &7{role} &f{player}&7: &e{message}"
  global-chat-prefix: "&8[&6{clan_tag}&8] "
```

Tutti i messaggi del plugin sono completamente personalizzabili nella sezione `messages:` del `config.yml`.

---

## 📜 Comandi

Tutti i comandi principali usano `/clan` (alias: `/c`, `/clans`).

### Gestione Clan

| Comando | Descrizione | Ruolo Minimo |
|---|---|---|
| `/clan create <nome> <tag>` | Crea un nuovo clan | — |
| `/clan disband` | Scioglie il clan | Leader |
| `/clan info [clan]` | Mostra informazioni sul clan | Member |
| `/clan leave` | Abbandona il clan | Member |

### Gestione Membri

| Comando | Descrizione | Ruolo Minimo |
|---|---|---|
| `/clan invite <player>` | Invita un giocatore | Officer |
| `/clan accept` | Accetta un invito | — |
| `/clan decline` | Rifiuta un invito | — |
| `/clan kick <player>` | Espelle un membro | Officer |
| `/clan promote <player>` | Promuove un membro | Leader |
| `/clan demote <player>` | Retrocede un membro | Leader |

### Territorio & Home

| Comando | Descrizione | Ruolo Minimo |
|---|---|---|
| `/clan claim` | Reclama il chunk attuale | Officer |
| `/clan unclaim` | Libera il chunk attuale | Officer |
| `/clan sethome` | Imposta la home del clan | Leader |
| `/clan home` | Teletrasporto alla home | Member |

### Comunicazione

| Comando | Descrizione | Ruolo Minimo |
|---|---|---|
| `/clan chat <messaggio>` | Invia un messaggio in chat clan | Member |

---

## 🔒 Permessi

| Permesso | Descrizione | Default |
|---|---|---|
| `clansystem.clan.create` | Creare un clan | `true` |
| `clansystem.clan.disband` | Sciogliere il clan | `true` |
| `clansystem.clan.invite` | Invitare giocatori | `true` |
| `clansystem.clan.kick` | Espellere membri | `true` |
| `clansystem.clan.promote` | Promuovere/retrocedere | `true` |
| `clansystem.clan.chat` | Usare la chat clan | `true` |
| `clansystem.clan.leave` | Abbandonare il clan | `true` |
| `clansystem.clan.claim` | Reclamare territori | `true` |
| `clansystem.clan.home` | Teletrasporto alla home | `true` |
| `clansystem.admin` | Tutti i permessi admin | `op` |

---

## 🏷️ PlaceholderAPI

Il plugin espone i seguenti placeholder tramite **PlaceholderAPI**:

| Placeholder | Descrizione | Esempio |
|---|---|---|
| `%clans_player_clan%` | Nome del clan del giocatore | `CoralWarriors` |
| `%clans_player_tag%` | Tag del clan | `CW` |
| `%clans_player_role%` | Ruolo nel clan | `LEADER` |
| `%clans_clan_members_online%` | Membri del clan online in quel momento | `4` |

> PlaceholderAPI deve essere installato sul server. Se non è presente, i placeholder vengono semplicemente ignorati.

---

## 🗃️ Schema Database

Il plugin crea automaticamente 4 tabelle con il prefisso `cs_`:

```
cs_clans        — Dati principali del clan (nome, tag, leader, home)
cs_members      — Membri con ruolo e data di ingresso
cs_territories  — Chunk reclamati per mondo e coordinate
cs_invites      — Inviti pendenti con scadenza automatica
```

Le relazioni utilizzano **foreign key con ON DELETE CASCADE**, garantendo la pulizia automatica dei dati correlati quando un clan viene eliminato.

---

## 📁 Struttura Progetto

```
ClanSystem/
├── src/main/
│   ├── java/it/stormcraft/clansystem/
│   │   ├── ClanSystem.java               # Entry point del plugin
│   │   ├── clan/
│   │   │   ├── Clan.java                 # Modello clan
│   │   │   ├── ClanMember.java           # Modello membro
│   │   │   ├── ClanRole.java             # Enum ruoli
│   │   │   └── ClanManager.java          # Business logic clan
│   │   ├── territory/
│   │   │   └── TerritoryManager.java     # Gestione claim/protezioni
│   │   ├── database/
│   │   │   └── DatabaseManager.java      # HikariCP + query MariaDB
│   │   ├── commands/
│   │   │   └── ClanCommand.java          # Handler comandi
│   │   ├── listeners/
│   │   │   ├── ChatListener.java         # Intercetta chat clan
│   │   │   ├── PlayerListener.java       # Join/quit notifiche
│   │   │   └── TerritoryListener.java    # Protezioni territorio
│   │   ├── placeholders/
│   │   │   └── ClanPlaceholders.java     # Hook PlaceholderAPI
│   │   ├── config/
│   │   │   └── PluginConfig.java         # Wrapper config.yml
│   │   └── utils/
│   │       ├── MessageUtil.java          # Helper messaggi colorati
│   │       └── ValidationUtil.java       # Validazione input
│   └── resources/
│       ├── config.yml
│       ├── plugin.yml
│       └── schema.sql
└── pom.xml
```

---

## 🔨 Build dal Sorgente

Assicurati di avere **Java 17+** e **Maven 3.8+** installati.

```bash
git clone https://github.com/ckanto/ClanSystem.git
cd ClanSystem
mvn clean package -DskipTests
```

Il JAR compilato (con HikariCP incluso via shade) si troverà in `target/ClanSystem-1.0.0.jar`.

---

## 📝 Changelog

### v1.0.0
- Release iniziale
- Sistema clan con ruoli Leader/Officer/Member
- Gestione inviti con scadenza
- Claim territori con integrazione WorldGuard
- Chat clan privata
- Supporto PlaceholderAPI
- Backend MariaDB con HikariCP

---

## 👤 Autore

Sviluppato con ❤️ da **ckanto** per **CoralMC**

- GitHub: [@ckanto](https://github.com/ckanto)
- Server: CoralMC

---

<div align="center">
<sub>ClanSystem © 2024 ckanto — CoralMC. Released under the MIT License.</sub>
</div>
