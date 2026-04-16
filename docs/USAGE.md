# SnowTerritory 使用文档

本文面向**服主与管理员**，说明如何安装、配置与使用各模块。玩家向说明可直接摘取「命令速查」与对应小节。

**适用版本：** 与仓库 `pom.xml` 中版本一致（当前示例为 1.2.0）。  
**运行环境：** Paper 1.21.x、Java 21、**必须安装 MMOItems**。

---

## 目录

1. [安装与首次启动](#1-安装与首次启动)
2. [主配置与模块开关](#2-主配置与模块开关)
3. [命令速查](#3-命令速查)
4. [权限说明](#4-权限说明)
5. [Reinforce 强化模块](#5-reinforce-强化模块)
6. [EnderStorage 战利品仓库](#6-enderstorage-战利品仓库)
7. [Quest 任务模块](#7-quest-任务模块)
8. [Stocks 期货模块](#8-stocks-期货模块)
9. [ST Fish 钓鱼模块](#9-st-fish-钓鱼模块)
10. [Armor 制式防具](#10-armor-制式防具)
11. [调试与配置重置](#11-调试与配置重置)
12. [常见问题](#12-常见问题)

---

## 1. 安装与首次启动

1. 将构建得到的 `SnowTerritory-x.x.x.jar` 放入服务器 `plugins` 目录。
2. 确保 **MMOItems** 已安装且能正常加载（本插件在 `plugin.yml` 中声明为硬依赖）。
3. 按需安装软依赖：
   - **Vault** + 任意经济插件：金币类消耗（强化、钓鱼唤天等）。
   - **PlayerPoints**：点券类消耗（强化等）。
   - **PlaceholderAPI**：消息中的 PAPI 占位符解析。
   - **MythicMobs**：战利品仓库自动拾取等扩展行为。
   - **MMOCore**（可选）：强化界面中与职业/等级相关的展示；未安装时相关功能自动关闭。
4. 启动服务器，插件会在 `plugins/SnowTerritory/` 下生成主配置与各模块默认文件（来自 JAR 内 `default-configs`）。
5. 用 [`/sn reload`](#3-命令速查) 或重启使配置生效（部分破坏性改动建议重启）。

---

## 2. 主配置与模块开关

**文件位置：** `plugins/SnowTerritory/config.yml`

### 2.1 模块开关 `modules`

每个键对应一个子系统，`false` 为关闭，`true` 为开启（若缺省，插件会按实现可能默认写入并视为开启，以你服务器上实际文件为准）：

| 键 | 模块 |
|----|------|
| `reinforce` | 强化 |
| `enderstorage` | 战利品仓库 |
| `quest` | 任务 |
| `stocks` | 期货 |
| `stfish` | 钓鱼 / 图鉴 / 唤天 |
| `armor` | 制式防具生成 |

关闭模块后，对应子命令会提示功能未启用；`/sn reload` 会按当前开关重新加载已启用模块。

### 2.2 全局消息 `messages`

主配置中的 `messages` 提供**全服共用**的提示样式、帮助行、调试文案等。各模块另有自己的 `messages` 或配置内文案，并通过 `MessageUtils.registerModuleMessages` 注册，**不会**与主配置重复定义同一业务键时冲突——查找顺序以实现为准（模块优先，再回退主配置）。

颜色与占位符说明见默认 `config.yml` 内注释（`&` 色码、十六进制、渐变、`{占位符}` 等）。

---

## 3. 命令速查

主命令：**`/snowterritory`**，简写 **`/sn`**。

| 命令 | 说明 |
|------|------|
| `/sn` | 显示帮助 |
| `/sn reload` | 重载主配置 + 所有**已启用**模块 |
| `/sn r` | 为自己打开强化界面 |
| `/sn r <玩家>` | 为在线玩家打开强化界面（需权限） |
| `/sn checkid` | 查看手持物品的 MMOItems 信息 |
| `/sn es` | 打开战利品仓库 GUI |
| `/sn es reload` | 重载 EnderStorage 配置与服务 |
| `/sn es give <玩家> <物品键> <数量>` | 向玩家仓库写入物品（管理） |
| `/sn q …` | 任务子命令（见 [Quest](#7-quest-任务模块)） |
| `/sn stock …` | 期货子命令（见 [Stocks](#8-stocks-期货模块)） |
| `/sn fish` | 打开鱼类图鉴 |
| `/sn fish give <玩家> <鱼种ID> [数量]` | 给予鱼类（管理） |
| `/sn weather summon` | 消耗金币召唤天气（需经济） |
| `/sn armor generate all <套装ID>[权重]` | 生成整套防具（见 [Armor](#10-armor-制式防具)） |
| `/sn debug resetconfig [模块]` | 危险：重置配置文件（见 [§11](#11-调试与配置重置)） |

**独立命令（与 `/sn es` 等价入口）：** `snowterritoryenderstorage`，别名 **`/stes`**（见 `plugin.yml`）。

---

## 4. 权限说明

以下为 `plugin.yml` 中声明的权限摘要；实际权限插件请以服务器为准。

| 权限节点 | 默认 | 用途 |
|----------|------|------|
| `mmoitemseditor.use` | op | 使用主命令体系 |
| `mmoitemseditor.edit` | op | 使用强化界面（自己） |
| `mmoitemseditor.reload` | op | `/sn reload`、`/sn debug resetconfig` |
| `mmoitemseditor.itemid` | op | `/sn checkid` |
| `mmoitemseditor.openforothers` | op | `/sn r <玩家>` |
| `st.loot.use` | true | 打开战利品仓库 |
| `st.loot.auto` | op | Mythic 等掉落自动入仓库（行为依赖模块实现） |
| `st.loot.admin` | op | `es reload`、`es give` |
| `st.fish.use` | true | 鱼类图鉴 |
| `st.fish.weather` | op | `/sn weather summon` |
| `st.fish.give` | op | `/sn fish give` |
| `st.armor.generate` | op | `/sn armor generate …` |

**任务模块：** 配置重载使用代码中的 **`st.quest.admin`**。若你使用 LuckPerms 等，请自行授予该节点（未写入 `plugin.yml` 时部分权限插件仍可通过自定义节点生效）。

**仓库容量 / 单品堆叠上限：** 由 `plugins/SnowTerritory/enderstorage/progression/size.yml` 与 `stack.yml` 中配置的**权限节点**决定，请与权限插件中的实际节点保持一致。

---

## 5. Reinforce 强化模块

### 5.1 配置目录

`plugins/SnowTerritory/reinforce/`

- **`config.yml`**：成功率、失败降级/维持概率、属性增幅、可强化物品 ID、可强化属性、基础金币/点券/材料数量、保护符与强化符的 MMOItems `type`、GUI 标题/大小/槽位等。
- **消耗表达式（若启用）：** 见同目录下由 `CostConfigManager` 加载的 YAML（默认包内带有 `example-cost.yml` 等参考，以你服务器上实际文件为准）。

### 5.2 使用流程（玩家）

1. 手持或背包中准备：**要强化的 MMOItems 物品**、满足配置数量的**材料**（占用 GUI 中材料槽）、可选**保护符 / 强化符**。
2. 执行 **`/sn r`** 打开界面。
3. 将物品放入配置中指定的槽位（武器槽、保护符、强化符、材料格、确认/取消等，槽位索引见 `config.yml` 的 `gui.slots`）。
4. 点击**确认**进行强化；**取消**关闭界面。

### 5.3 结果类型（概念说明）

- **成功：** 强化等级上升，并按配置提升属性（如 `attribute-boost-percent`）。
- **失败降级：** 按 `fail-degrade-chance` 等逻辑可能降等；**保护符**在有效等级内可抑制降级（具体以符的 lore 规则与 `preservation-token` 配置为准）。
- **维持：** `maintain-chance` 控制「不变」分支；**强化符**可按 lore 提升成功率（见 `upgrade-token` 说明）。

### 5.4 经济

- **Vault 金币**、**PlayerPoints** 在 `reinforce.cost` 中可设为 `0` 关闭。
- 实际扣费与**表达式消耗**由 `CostCalculationService` 等实现；若安装了 **MMOCore**，部分展示或计算可能与之联动。

### 5.5 管理员

- **`/sn r <玩家>`**：为指定在线玩家打开强化 GUI（需 `mmoitemseditor.openforothers` 或控制台等规则以代码为准）。

---

## 6. EnderStorage 战利品仓库

### 6.1 配置目录

`plugins/SnowTerritory/enderstorage/`

| 文件 | 作用 |
|------|------|
| `config.yml` | 数据库类型与连接池、`features`（自动拾取、取消实体掉落、调试、默认语言等） |
| `gui.yml` | **白名单与展示**：在 `gui.materials` 下按 MMOItems **type → 物品 id** 配置；值为数字表示该物品默认单品上限，也可用对象形式写 `max`、`lore` 等 |
| `messages/*.yml` | 多语言包 |
| `progression/size.yml` | 仓库总槽位数与权限节点对应关系 |
| `progression/stack.yml` | 单品堆叠上限与权限节点对应关系 |

**重要：** 可存入物品**必须**出现在 `gui.yml` 的 `gui.materials` 中；不再使用独立的 `loot/whitelist.yml` 文件。

### 6.2 打开方式

- **`/sn es`**（玩家，需 `st.loot.use`）
- **`/stes`** / **`snowterritoryenderstorage`**（与上等价，见 `plugin.yml`）

### 6.3 GUI 操作（与当前代码一致）

点击仓库中**带物品数据的槽位**时：

| 点击 | 行为 | 数量 |
|------|------|------|
| **左键** | 从背包**存入** | 8 |
| **Shift + 左键** | 从背包**存入** | 64 |
| **右键** | **取出**到背包 | 8 |
| **中键** | **取出** | 64（依赖客户端是否触发中键） |
| **Shift + 右键** | 当前实现下取量为 0，**无效** |

存入时只会匹配 **MMOItems** 且 **白名单 key** 与槽位一致的物品。取存后会刷新当前页。

### 6.4 管理命令

- **`/sn es reload`**：重载配置（需 `st.loot.admin`）。
- **`/sn es give <玩家> <itemKey> <数量>`**：向该玩家仓库增加物品（`itemKey` 格式与内部白名单 key 一致，一般为 `MMOItemsType:ItemId`）。

### 6.5 数据库

- 默认 **SQLite**，数据库文件路径由 `config.yml` 的 `database.file` 决定（相对 `enderstorage` 目录）。
- 插件使用 **HikariCP** 连接池；**请勿**在服务器运行时随意删除正在使用的 `.db` 文件。

---

## 7. Quest 任务模块

### 7.1 配置目录

`plugins/SnowTerritory/quest/`

主要包含：`config.yml`、任务与奖励（`tasks/`、`rewards/`、`bonus/`）、`bounty/`、`materials/whitelist.yml`、`crops/whitelist.yml`、`messages/` 等。首次运行会从 JAR 拷贝默认结构。

### 7.2 命令（前缀 `/sn q` 或 `/sn quest`）

以下参数中，`accept` 可简写为 `a`，`list` 为 `l`，`complete` 为 `c`。

| 命令 | 说明 |
|------|------|
| `/sn q` 或 `/sn q list` | 列出自己进行中的任务与悬赏等 |
| `/sn q accept` | 接取**材料类**默认任务 |
| `/sn q accept material` | 同上 |
| `/sn q accept collect` | 接取**收集类**任务 |
| `/sn q accept kill` | 当前版本会提示**击杀任务尚未实现** |
| `/sn q complete` | 领取已完成的**悬赏**奖励（批量领取逻辑以服务端为准） |
| `/sn q setlevel <等级> [玩家]` | 设置材料任务等级上限；指定他人时需 **OP** |
| `/sn q getlevel` | 查看自己的材料任务等级上限 |
| `/sn q reload` | 重载任务配置（需 **`st.quest.admin`**） |

等级合法范围来自 `rewards/level.yml` 的键集合。

### 7.3 数据

任务进度等使用 **SQLite** 持久化（DAO 实现见 `quest` 包）。备份时请一并备份插件数据目录下的数据库文件。

---

## 8. Stocks 期货模块

### 8.1 配置目录

`plugins/SnowTerritory/stocks/`

- **`config.yml`**：行情源（如 REST URL、更新间隔）、交易对 `symbols`（价格步长、数量步长、最大杠杆、维持保证金率、手续费等）、风控轮询间隔等。

默认示例中包含 `BTCUSDT`、`ETHUSDT` 等符号；**实际价格能否拉取**取决于你配置的行情 API 与服务器出网情况。

### 8.2 命令（仅玩家）

所有子命令挂在 **`/sn stock`**（或 `stocks`）下：

| 命令 | 说明 |
|------|------|
| `/sn stock price <symbol>` | 查询标记价与最新价 |
| `/sn stock open <long\|short> <symbol> <数量> <杠杆>` | 开仓 |
| `/sn stock close <symbol> [数量]` | 平仓；省略数量时按实现平掉可平部分 |
| `/sn stock pos` | 查看持仓 |
| `/sn stock margin add <symbol> <金额>` | 追加保证金 |
| `/sn stock setlev <symbol> <杠杆>` | 设置杠杆（**已有持仓时通常不可改**，以实现为准） |
| `/sn stock bal` | 查看钱包、可用余额、未实现盈亏、净值等 |

别名：`position` ↔ `pos`，`balance` ↔ `bal`，`setleverage` ↔ `setlev`。

### 8.3 说明

该模块为**游戏内模拟交易**，数值单位与规则以配置和 `TradeEngine` / `RiskEngine` 为准。部署前建议在测试服完整跑通开仓、平仓、强平等流程。

---

## 9. ST Fish 钓鱼模块

### 9.1 配置目录

`plugins/SnowTerritory/stfish/`

- **`config.yml`**：唤天消耗、天气世界名、经济开关等。
- **`fish.yml`**：鱼种、长度区间、品质分层等定义。

### 9.2 命令

| 命令 | 说明 |
|------|------|
| `/sn fish` | 打开**图鉴**（等价于 `/sn fish atlas`） |
| `/sn fish atlas` | 同上 |
| `/sn fish give <玩家> <鱼种ID> [数量]` | 按配置生成鱼并放入背包（满则掉落在地）；数量默认 1，最大 64 |

### 9.3 天气召唤

- **`/sn weather summon`**
- 需要 **`st.fish.weather`**，且 **Vault 经济可用**；从配置读取消耗金币，成功后在配置的世界/逻辑中召唤天气。
- 若经济未接入或世界未加载，会收到对应错误提示。

### 9.4 游戏内行为

钓鱼相关监听、出售、图鉴解锁等见 `stfish.listener` 与 `FishLootService` 等实现；具体是否与世界、物品 NBT 联动以你当前版本代码为准。

---

## 10. Armor 制式防具

### 10.1 配置目录

`plugins/SnowTerritory/armor/`

- **`config.yml`**：MMOItems 类型、槽位、品质、随机区间、属性映射等。
- **`sets.yml`**：套装定义（id、展示名、各部位等）。
- **`messages/*.yml`**：模块消息。

### 10.2 生成命令

```
/sn armor generate all <套装ID>
/sn armor generate all <套装ID>[w1,w2,w3]
```

- 仅 **玩家**可执行，需 **`st.armor.generate`**。
- **`<套装ID>`**：与 `sets.yml` 中配置的套装 id 一致；可用 Tab 补全（输入 `套装ID` 前缀部分）。
- **可选权重后缀：** `套装ID[整数,整数,整数]`，必须**恰好三个非负整数**，对应 **common / rare / epic** 品质的随机权重；格式错误会提示「品质权重格式错误」。
- 生成物会尝试放入背包，**放不下则掉落在玩家脚边**。

---

## 11. 调试与配置重置

### 11.1 `/sn debug resetconfig`

1. 执行：`/sn debug resetconfig` 或 `/sn debug resetconfig <模块>`。
2. 插件提示后在 **30 秒内**聊天输入 **`yes`** 确认。
3. **不跟模块名**：删除整个 `SnowTerritory` 数据目录下**除 `*.db` 以外**的配置类文件，然后重载（数据库保留）。
4. **跟模块名**：仅清理该模块子目录下同类文件并重载该模块。可选：`reinforce`、`enderstorage`、`quest`、`stocks`、`stfish`、`armor`、`all`（或中文「全部」）。

**警告：** 会导致自定义配置丢失并从默认模板重新生成；仅建议在测试服或已备份时使用。

### 11.2 权限

需要与 **`/sn reload`** 相同的管理权限（`mmoitemseditor.reload` 或 OP 规则以 `SnowTerritoryCommand` 实现为准）。

---

## 12. 常见问题

**Q：插件启动失败？**  
A：检查是否安装 **MMOItems**；查看控制台首条红色错误。

**Q：关闭某个模块后命令还能用吗？**  
A：主命令仍可用，但对应子命令会提示该功能未启用。

**Q：强化不扣钱？**  
A：确认安装 **Vault** 与经济插件，且 `reinforce.cost` 中非零；点券同理检查 **PlayerPoints**。

**Q：仓库里放不进东西？**  
A：在 **`enderstorage/gui.yml`** 的 `gui.materials` 中为该物品的 type 与 id 添加条目；存入物品必须是 **MMOItems**。

**Q：任务接不了击杀？**  
A：当前版本击杀任务未实现，请使用 `accept` 或 `accept collect`。

**Q：期货价格不动或报错？**  
A：检查 `stocks/config.yml` 中 API、网络防火墙、以及符号大小写是否与配置一致。

**Q：Linux 服务器 JAR 特别大或 SQLite 报错？**  
A：查看项目 `pom.xml` 中 **maven-shade-plugin** 对 `sqlite-jdbc` 的排除项；默认可能只保留 Windows 原生库，Linux 需按注释改过滤器。

**Q：如何备份？**  
A：定期备份整个 `plugins/SnowTerritory/`，尤其所有 **`.db`** 文件。

---

## 相关链接

- 英文概览与构建说明：[项目根目录 README](../README.md)
- 测试与流程图：[docs/README.md](README.md)

文档版本随插件迭代而变化，修改配置前请先对照你服务器上的实际 JAR 版本与源码。
