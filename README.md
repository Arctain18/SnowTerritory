# SnowTerritory

SnowTerritory is a modular **Paper 1.21** plugin for Minecraft servers that extend **MMOItems**-based gameplay. It ships several optional features—reinforcement, loot storage, quests, paper trading, fishing, and procedural armor sets—behind toggles in the main config.

**Version:** 1.2.0 (see `pom.xml`)  
**Main class:** `top.arctain.snowTerritory.Main`  
**Package:** `top.arctain.snowTerritory`

---

## Features

### Reinforce

- GUI-driven **MMOItems reinforcement** with configurable success / maintain / fail-degrade behavior.
- **Cost system:** Vault currency, PlayerPoints, and material slots (see module config).
- **Charm slots:** preservation and upgrade tokens (MMOItems types from config).
- **MMOCore integration (optional):** class/level display in the UI when MMOCore is present; gracefully disabled if not.

### EnderStorage (loot warehouse)

- **Per-player loot storage** with paginated GUI (SQLite by default, HikariCP connection pooling).
- **Whitelist** driven by `gui.yml` → `gui.materials` (MMOItems type → item id → per-item stack cap and optional lore).
- **Progression:** extra slots and per-item caps via permission-based rules (`progression/size.yml`, `progression/stack.yml`).
- **MythicMobs:** optional auto-pickup of whitelisted drops for players with the right permission.
- Standalone command `snowterritoryenderstorage` (alias `stes`) is registered in addition to `/sn es`.

### Quest

- **Material** and **collect** style quests, rewards, time bonuses, and **bounty** tasks (see `quest/` defaults).
- **SQLite** persistence for quest data.
- Admin reload: `/sn q reload` (permission `st.quest.admin`).
- **Kill** quest type is not implemented; attempting to accept it shows a not-implemented message.

### Stocks (futures-style trading)

- In-game **contracts** with configurable symbols (e.g. `BTCUSDT`, `ETHUSDT`), leverage, margin, and fees.
- **Price feed** via REST (default sample uses CoinGecko-style config in `stocks/config.yml`).
- Commands: price query, open/close positions, margin, leverage, balance (players only).

### ST Fish

- **Fish atlas** GUI, fish definitions and tiers in YAML.
- **Admin give:** `/sn fish give <player> <fishId> [amount]`.
- **Weather summon:** `/sn weather summon` (costs Vault balance; requires economy + permission).

### Armor

- Generates **full MMOItems armor sets** from `armor/sets.yml` and related config.
- Command: `/sn armor generate all <setId>[w1,w2,w3]` — optional three non-negative integer weights for common / rare / epic quality rolls.
- Overflow drops at the player’s feet if inventory is full.

### Core / misc

- **PlaceholderAPI** parsing in messages when the plugin is installed (otherwise text is unchanged).
- **`/sn debug resetconfig`** — destructive config reset with in-game confirmation (see [Debug](#debug)); database files are kept.

---

## Requirements

| Component | Required |
|-----------|----------|
| **Server** | Paper (or compatible) **1.21.x** — API targets **1.21.1** |
| **Java** | **21** |
| **MMOItems** | **Yes** (hard dependency in `plugin.yml`) |

### Soft dependencies

- **Vault** — gold costs (Reinforce, ST Fish weather summon, etc.) when an economy provider is present.
- **PlayerPoints** — point costs where configured (e.g. Reinforce).
- **PlaceholderAPI** — placeholders in configured messages.
- **MythicMobs** — enhanced / auto loot behavior for EnderStorage when installed.
- **MMOCore** — optional Reinforce UI enhancements (no hard runtime requirement).

---

## Installation

1. Build or obtain `SnowTerritory-1.2.0.jar` (artifact name follows `${project.version}` in Maven).
2. Place the JAR in the server `plugins` folder.
3. Ensure **MMOItems** is installed and loads before SnowTerritory.
4. Start the server once to generate `plugins/SnowTerritory/`.
5. Edit `plugins/SnowTerritory/config.yml` — especially `modules:` — to enable or disable features.

Default module flags (all `true` in shipped defaults):

```yaml
modules:
  reinforce: true
  enderstorage: true
  quest: true
  stocks: true
  stfish: true
  armor: true
```

---

## Configuration layout

| Path | Purpose |
|------|---------|
| `plugins/SnowTerritory/config.yml` | Global toggles, shared message prefix/help/debug strings |
| `plugins/SnowTerritory/reinforce/` | Reinforce rules, GUI, costs (`config.yml`, optional cost YAML) |
| `plugins/SnowTerritory/enderstorage/` | DB settings, `gui.yml` (whitelist + GUI), `messages/`, `progression/` |
| `plugins/SnowTerritory/quest/` | Tasks, rewards, bounty, whitelists, messages |
| `plugins/SnowTerritory/stocks/` | Exchange URL, symbols, risk/price intervals |
| `plugins/SnowTerritory/stfish/` | Fish definitions, economy hooks, weather world |
| `plugins/SnowTerritory/armor/` | Set definitions, qualities, stat mapping, messages |

First-run files are copied from the plugin JAR under `default-configs/` (see `src/main/resources/default-configs/` in the source tree).

### Messages and placeholders

- Global chat strings live under `messages` in the main `config.yml`.
- Each module can register its own message YAML; lookups use `{key}` style placeholders as documented in the default configs.
- Color formats supported by the project include `&` codes and hex/gradient styles as described in the default `config.yml` comments.

---

## Commands

Primary command: **`/snowterritory`** (alias **`/sn`**). Subcommands below assume `/sn`.

| Command | Description |
|---------|-------------|
| `/sn` | Help |
| `/sn reload` | Reload main config and all **enabled** modules (`mmoitemseditor.reload` or OP) |
| `/sn r [player]` | Open Reinforce GUI for self or target (`reinforce` module) |
| `/sn checkid` | Show MMOItems id of held item |
| `/sn es` | Open EnderStorage GUI (`st.loot.use`) |
| `/sn es reload` | Reload EnderStorage (`st.loot.admin`) |
| `/sn es give <player> <itemKey> <amount>` | Admin deposit to warehouse |
| `/sn q`, `/sn quest` | Quest: list; `accept`/`a`, `complete`/`c`, `setlevel`, `getlevel`, `reload` (see in-game usage) |
| `/sn stock …` | Stocks: `price`, `open`, `close`, `pos`, `margin`, `setlev`, `bal` (player-only) |
| `/sn fish` | Open fish atlas (`st.fish.use`) |
| `/sn fish give <player> <fishId> [amount]` | Give fish (`st.fish.give`) |
| `/sn weather summon` | Paid weather summon (`st.fish.weather`, Vault) |
| `/sn armor generate all <setId>[w1,w2,w3]` | Generate full armor set (`st.armor.generate`) |
| `/sn debug resetconfig [module]` | Reset config files after typing `yes` (see [Debug](#debug)) |

**EnderStorage** is also exposed as **`/snowterritoryenderstorage`** (alias **`/stes`**) per `plugin.yml`.

### Debug

`/sn debug resetconfig` prompts for confirmation (`yes` within 30 seconds). With no module argument, it removes configurable files under the plugin data folder **except** `*.db` databases, then reloads. Modules: `reinforce`, `enderstorage`, `quest`, `stocks`, `stfish`, `armor`, or `all`.

---

## Permissions

Declared in `plugin.yml` (summarized):

| Permission | Default | Use |
|------------|---------|-----|
| `mmoitemseditor.use` | op | Base `/sn` usage |
| `mmoitemseditor.edit` | op | Reinforce GUI (self) |
| `mmoitemseditor.reload` | op | `/sn reload`, debug reset |
| `mmoitemseditor.itemid` | op | `/sn checkid` |
| `mmoitemseditor.openforothers` | op | `/sn r <player>` |
| `st.loot.use` | true | Open loot warehouse |
| `st.loot.auto` | op | Auto-pickup to warehouse |
| `st.loot.admin` | op | EnderStorage admin |
| `st.fish.use` | true | Fish atlas |
| `st.fish.weather` | op | Weather summon |
| `st.fish.give` | op | Give fish |
| `st.armor.generate` | op | Armor set generation |

Quest admin reload uses **`st.quest.admin`** (used in code; add to `plugin.yml` on your server if you rely on permission plugins to define unknown nodes).

EnderStorage slot/stack progression permissions are configured in `enderstorage/progression/*.yml` (e.g. `st.loot.size.*` style nodes — align with your edited YAML).

---

## Building

```bash
mvn clean package
```

Output: `target/SnowTerritory-1.2.0.jar`

### Bundled runtime libraries

The shaded JAR includes **HikariCP**, **SQLite JDBC**, and **exp4j**. The Maven Shade plugin **excludes non-Windows SQLite native libraries** in the default POM to trim size. If you deploy on **Linux**, adjust the `sqlite-jdbc` filter in `pom.xml` to keep the appropriate native artifacts and exclude others.

### Optional remote deploy profile

The POM defines a **`deploy`** profile that runs `scp` after package (host/path placeholders). Activate with:

```bash
mvn clean package -Pdeploy
```

Override `deploy.host`, `deploy.user`, and `deploy.path` via `-D` properties or Maven settings if needed.

---

## Project layout (source)

```
src/main/java/top/arctain/snowTerritory/
├── Main.java                 # Plugin bootstrap, module lifecycle
├── config/                   # Global PluginConfig
├── commands/                 # Root command, item id, debug reset
├── listeners/                # Shared listeners
├── utils/                    # Config, messages, NBT, placeholders, etc.
├── reinforce/                # Reinforce module
├── enderstorage/             # Loot storage module
├── quest/                    # Quest module
├── stocks/                   # Stocks module
├── stfish/                   # Fishing module
└── armor/                    # Armor generation module
```

---

## Documentation

- **Detailed usage (Chinese):** [`docs/USAGE.md`](docs/USAGE.md) — installation, commands, permissions, per-module configuration and workflows.
- Other Chinese material under [`docs/`](docs/README.md): test guide, flowcharts.

---

## Troubleshooting

- **Plugin disables on startup** — MMOItems must be installed; check the console for the dependency error from `Main.checkDependencies()`.
- **Reinforce costs not deducting** — Install **Vault** and an economy provider, and/or **PlayerPoints**, matching your `reinforce` cost config.
- **Cannot deposit items in EnderStorage** — Add entries under `gui.materials` in `plugins/SnowTerritory/enderstorage/gui.yml`.
- **Stocks prices stale or failing** — Verify `stocks/config.yml` exchange settings and network access from the game server.
- **ST Fish weather “economy disabled”** — Vault must be present and hooked to an economy plugin.

---

## Author & license

**Author:** Arctain (see `plugin.yml` / `website`)

This project is **private**; do not redistribute or use without authorization from the rights holder.

---

SnowTerritory is built for servers that already run **MMOItems**. Configure each module under `plugins/SnowTerritory/` and set `modules.*` to `false` to disable anything you do not use.
