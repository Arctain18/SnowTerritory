---
name: Quest System Implementation Plan
overview: ""
todos:
  - id: 66cf5a65-eca0-4db0-b421-6757dcfb1595
    content: Integrate QuestModule into Main.java and add quest subcommand to SnowTerritoryCommand.java
    status: pending
isProject: false
---

# Quest System Implementation Plan

## Overview

Create a new `/quest/` module following the structure of `Reinforce` and `enderstorage` modules. The system will support material quests (kill quests as TODO), bounty and normal release methods, time-based bonuses, and complex reward calculations.

## Module Structure

### Core Classes

- `quest/QuestModule.java` - Main module entry point (similar to EnderStorageModule)
- `quest/config/DefaultFiles.java` - Default configuration content (no default values in method parameters)
- `quest/config/QuestConfigManager.java` - Configuration manager
- `quest/config/MessageProvider.java` - Message provider (reuse pattern from enderstorage)
- `quest/command/QuestCommand.java` - Command handler
- `quest/service/QuestService.java` - Service interface
- `quest/service/QuestServiceImpl.java` - Service implementation
- `quest/data/Quest.java` - Quest data model
- `quest/data/QuestType.java` - Enum (MATERIAL, KILL)
- `quest/data/QuestReleaseMethod.java` - Enum (NORMAL, BOUNTY)
- `quest/data/QuestStatus.java` - Enum (ACTIVE, COMPLETED, EXPIRED)
- `quest/listener/QuestListener.java` - Handle material submission events
- `quest/utils/QuestUtils.java` - Utility methods (MMOItems identification, reward calculation)

### Configuration Files (in `plugins/SnowTerritory/quest/`)

1. `config.yml` - Main configuration (features, debug, default language)
2. `messages/zh_CN.yml` - All player-facing messages (support ChatColor)
3. `rewards/default.yml` - Default rewards (questpoint, currency with 64-base stacks)
4. `rewards/level.yml` - Level multipliers (key=level, value=multiplier)
5. `bonus/time-bonus.yml` - Time bonus configuration (ratings, max-limit, time-bonus)
6. `materials/whitelist.yml` - Material whitelist (type:name format, min/max/material-level)
7. `bounty/config.yml` - Bounty task configuration (interval range, types, bounty-bonus)
8. `tasks/material.yml` - Material task definitions
9. `tasks/kill.yml` - Kill task definitions (TODO placeholder)

## Key Features

### Quest Components

1. **Type**: MATERIAL (implement), KILL (TODO)
2. **Release Method**: NORMAL, BOUNTY
3. **Time Limit**: Track start time, calculate completion time, apply time bonus
4. **Quest Level**: Based on material level and quantity (algorithm placeholder for now)
5. **Rewards**: Calculated as `basic * level-bonus * bounty-bonus(if valid) * time-bonus`

### Reward System

- **Achievement Points**: Execute console command `qp give <player> <points>` via Bukkit.dispatchCommand
- **Entity Currency**: MMOItems currency items using 64-base system
- Example: 324 currency → 5 * stack-1 + 4 * stack-0
- Configuration: `stack-0`, `stack-1`, etc. with MMOItems type and ID

### Time Bonus System

- Track quest start time when accepted (normal) or published (bounty)
- Calculate completion time
- Match against time-bonus.yml ratings (1=best, higher=worse)
- Apply corresponding time-bonus multiplier

### Material Quests

- Randomly select material from whitelist
- Random quantity between min and max
- Track player progress
- Allow material submission via listener
- Calculate level based on material-level and quantity (placeholder algorithm)

### Bounty Tasks

- Random publishing at configured intervals (e.g., 20-40 minutes)
- Broadcast to all players via chat
- Multiple players can complete same bounty
- Apply bounty-bonus multiplier to rewards
- Configuration: interval range, allowed types, bounty-bonus value

### Normal Tasks

- Player-initiated (via command or GUI)
- Single player completion
- Standard reward calculation (no bounty-bonus)

## Implementation Details

### Configuration Management

- All configurable content in YAML files
- DefaultFiles.java contains default content as strings
- No default values in method parameters
- Support ChatColor parsing via ColorUtils.colorize()
- Separate logic into different YAML files

### MMOItems Integration

- Reuse MMOItems identification from Utils.isMMOItem()
- Use MMOItems.getType() and MMOItems.getID() for item matching
- Match items against whitelist using TYPE:NAME format

### Command Integration

- Add quest subcommand to SnowTerritoryCommand: `/sn q` or `/sn quest`
- Register QuestModule in Main.java
- Add quest command handler with tab completion

### Data Persistence

- Store active quests in memory (Map<UUID, List>)
- Optionally persist to database (similar to EnderStorage pattern)
- Track quest history for statistics

### Bounty Publishing

- Use Bukkit scheduler for interval-based publishing
- Random selection from material whitelist
- Broadcast formatted message to all players
- Store active bounty quests separately

## Files to Create

### Java Files

1. `src/main/java/top/arctain/snowTerritory/quest/QuestModule.java`
2. `src/main/java/top/arctain/snowTerritory/quest/config/DefaultFiles.java`
3. `src/main/java/top/arctain/snowTerritory/quest/config/QuestConfigManager.java`
4. `src/main/java/top/arctain/snowTerritory/quest/config/MessageProvider.java`
5. `src/main/java/top/arctain/snowTerritory/quest/command/QuestCommand.java`
6. `src/main/java/top/arctain/snowTerritory/quest/service/QuestService.java`
7. `src/main/java/top/arctain/snowTerritory/quest/service/QuestServiceImpl.java`
8. `src/main/java/top/arctain/snowTerritory/quest/data/Quest.java`
9. `src/main/java/top/arctain/snowTerritory/quest/data/QuestType.java`
10. `src/main/java/top/arctain/snowTerritory/quest/data/QuestReleaseMethod.java`
11. `src/main/java/top/arctain/snowTerritory/quest/data/QuestStatus.java`
12. `src/main/java/top/arctain/snowTerritory/quest/listener/QuestListener.java`
13. `src/main/java/top/arctain/snowTerritory/quest/utils/QuestUtils.java`

### Configuration Files (DefaultFiles.java content)

- All YAML files listed above with appropriate default content

### Integration Points

- Update `Main.java` to initialize QuestModule
- Update `SnowTerritoryCommand.java` to handle quest subcommand
- Update `plugin.yml` if needed (likely not, using main command)

## Implementation Order

1. Create data models (Quest, enums)
2. Create DefaultFiles.java with all default configurations
3. Create QuestConfigManager.java
4. Create MessageProvider.java
5. Create QuestUtils.java (reward calculation, MMOItems helpers)
6. Create QuestService interface and implementation
7. Create QuestListener.java (material submission)
8. Create QuestCommand.java
9. Create QuestModule.java
10. Integrate into Main.java and SnowTerritoryCommand.java

## Notes

- Level calculation algorithm is placeholder (to be implemented later)
- Kill quests marked as TODO
- All text supports ChatColor via ColorUtils.colorize()
- 64-base currency system prevents inventory overflow
- Bounty tasks use scheduler for random interval publishing

