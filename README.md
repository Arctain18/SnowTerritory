# SnowTerritory - MMOItems 强化与战利品存储插件

一个功能完整的 Minecraft Paper 服务器插件，包含物品强化系统和战利品存储系统。

## 功能特性

### 🔨 Reinforce（强化）模块
- ✅ **物品强化系统**：支持成功/失败/维持三种结果
- ✅ **GUI 界面**：直观的图形化强化界面
- ✅ **概率系统**：可配置不同等级的成功率
- ✅ **保护机制**：保护符防止失败降级
- ✅ **增强机制**：强化符提升成功率
- ✅ **经济集成**：支持 Vault 和 PlayerPoints
- ✅ **属性修改**：自动修改物品的攻击力、防御力等属性
- ✅ **等级显示**：物品名称显示强化等级（+1, +2...）

### 📦 EnderStorage（末影存储）模块
- ✅ **战利品仓库**：快速存储游戏物品，省略地皮系统繁琐的实体箱子存储
- ✅ **自动拾取**：MythicMobs 掉落物自动入库（需权限）
- ✅ **白名单系统**：支持 MMOItems 物品和原版物品的白名单配置
- ✅ **容量解锁**：基于权限节点的仓库容量和单品上限解锁
- ✅ **GUI 界面**：分页浏览、批量存取物品
- ✅ **数据库存储**：SQLite 数据库持久化存储
- ✅ **多语言支持**：可配置的消息系统

## 依赖要求

### 必需依赖
- **Paper/Spigot 1.21.1**
- **MMOItems 6.9.5+**

### 可选依赖
- **Vault** - 用于金币消耗功能
- **PlayerPoints** - 用于点券消耗功能
- **MythicMobs 5.7.2+** - 用于战利品自动拾取（EnderStorage 模块）

## 安装方法

1. 将编译好的 `SnowTerritory-1.0.jar` 放入服务器的 `plugins` 文件夹
2. 确保已安装 MMOItems 插件
3. 启动服务器，插件会自动生成配置文件
4. 根据需要修改配置文件：
   - `plugins/SnowTerritory/config.yml` - Reinforce 模块配置
   - `plugins/SnowTerritory/ender-storage/` - EnderStorage 模块配置

## 使用方法

### 主命令

主命令：`/snowterritory` 或 `/sn`

#### Reinforce（强化）模块命令
- `/sn r` 或 `/sn reinforce` - 打开物品强化界面（需要权限 `mmoitemseditor.edit`）
- `/sn r <玩家名>` - 为其他玩家打开强化界面（需要权限 `mmoitemseditor.openforothers`）
- `/sn checkid` 或 `/sn check` - 查看手中物品的 MMOItems ID（需要权限 `mmoitemseditor.itemid`）

#### EnderStorage（末影存储）模块命令
- `/sn es` 或 `/sn enderstorage` - 打开战利品仓库 GUI（需要权限 `st.loot.use`）
- `/sn es reload` - 重载 EnderStorage 配置（需要权限 `st.loot.admin`）
- `/sn es give <玩家> <物品key> <数量>` - 管理员发放物品到仓库（需要权限 `st.loot.admin`）

#### 通用命令
- `/sn reload` - 重载插件配置（需要权限 `mmoitemseditor.reload`）

### 强化流程

1. 手持或背包中有 MMOItems 物品
2. 执行 `/sn r` 打开强化界面
3. 在武器槽位放置要强化的物品
4. （可选）放置保护符防止降级
5. （可选）放置强化符提升成功率
6. 在材料槽位放置所需材料
7. 点击确认按钮进行强化

### 强化结果

- **成功**：物品等级 +1，属性提升
- **失败降级**：物品等级 -1，属性降低（使用保护符可避免）
- **维持不变**：等级和属性不变

### 战利品仓库使用

1. **打开仓库**：执行 `/sn es` 打开仓库 GUI
2. **存入物品**：
   - 左键点击仓库中的物品槽位：存入 8 个
   - Shift + 左键：存入 64 个
   - 物品必须在白名单中才能存入
3. **取出物品**：
   - 右键点击仓库中的物品槽位：取出 8 个
   - Shift + 右键：取出 64 个
4. **自动拾取**：拥有 `st.loot.auto` 权限的玩家，击杀怪物时白名单物品会自动入库

## 配置说明

### Reinforce 模块配置

配置文件位于 `plugins/SnowTerritory/config.yml`

主要配置项：
```yaml
reinforce:
  # 失败降级概率
  fail-degrade-chance: 0.3
  
  # 维持概率
  maintain-chance: 0.2
  
  # 成功时属性提升百分比
  attribute-boost-percent: 1.1  # 1.1 = +10%
  
  # 不同等级的成功率
  success-rates:
    level-0: 0.9   # 0级升1级: 90%
    level-1: 0.8   # 1级升2级: 80%
    # ...
  
  # 消耗配置
  cost:
    vault-gold: 1000      # 金币消耗
    player-points: 50     # 点券消耗
    materials: 6          # 材料数量
```

详细配置说明请查看生成的 `config.yml` 文件中的注释。

### EnderStorage 模块配置

配置文件位于 `plugins/SnowTerritory/ender-storage/`

- `config.yml` - 数据库、基础开关、调试、默认消息语言
- `messages/*.yml` - 多语言文本（提示、错误、GUI 文本）
- `loot/whitelist.yml` - 可存储物品列表、默认单品容量、显示名
- `progression/size.yml` - 仓库容量解锁规则（权限节点 → 槽位数）
- `progression/stack.yml` - 单品上限解锁规则（权限节点 → 单品上限）

详细配置说明请查看 [文档目录](docs/) 中的相关文档。

## 权限

### Reinforce 模块权限
- `mmoitemseditor.use` - 使用 SnowTerritory 命令（默认：OP）
- `mmoitemseditor.edit` - 使用物品编辑功能（默认：OP）
- `mmoitemseditor.reload` - 重载插件配置（默认：OP）
- `mmoitemseditor.itemid` - 查看物品ID（默认：OP）
- `mmoitemseditor.openforothers` - 为其他玩家打开物品强化界面（默认：OP）

### EnderStorage 模块权限
- `st.loot.use` - 打开战利品仓库（默认：true）
- `st.loot.auto` - 掉落物自动入库（默认：OP）
- `st.loot.admin` - 管理战利品仓库（默认：OP）
- `st.loot.size.X` - 仓库容量等级（X 为等级数字）
- `st.loot.stack.X` - 单品上限等级（X 为等级数字）

## 文档

- [测试指南](docs/TEST_GUIDE.md) - 详细的测试清单和测试方法
- [逻辑流程图](docs/FLOWCHART.md) - 插件完整逻辑判定流程图

## 开发信息

### 编译

```bash
mvn clean package
```

编译后的文件位于 `target/SnowTerritory-1.0.jar`

### 项目结构

```
src/main/java/top/arctain/snowTerritory/
├── Main.java                    # 主类
├── commands/                    # 命令处理
│   ├── SnowTerritoryCommand.java
│   ├── ItemIdCommand.java
│   └── ...
├── config/                      # 配置管理
│   └── PluginConfig.java
├── gui/                         # GUI界面
│   └── ItemEditorGUI.java
├── listeners/                   # 事件监听
│   ├── GUIListener.java
│   └── ItemEditListener.java
├── enderstorage/                # EnderStorage 模块
│   ├── EnderStorageModule.java
│   ├── command/
│   ├── config/
│   ├── gui/
│   ├── listener/
│   └── service/
└── utils/                       # 工具类
    ├── Utils.java
    ├── MessageUtils.java
    └── ...
```

## 常见问题

### Q: 插件无法启用？
A: 请确保已安装 MMOItems 插件，并检查控制台错误信息。

### Q: 强化后物品属性没有变化？
A: 请确保物品是有效的 MMOItems 物品，并且物品有可修改的属性。

### Q: 经济系统不工作？
A: 请确保已安装 Vault 和对应的经济插件（如 EssentialsX），或 PlayerPoints 插件。

### Q: 战利品仓库无法打开？
A: 请检查是否有 `st.loot.use` 权限，并确认 EnderStorage 模块已正确加载。

### Q: 物品无法存入仓库？
A: 请检查物品是否在 `loot/whitelist.yml` 白名单中配置。

## 更新日志

### v1.0
- ✅ 初始版本
- ✅ 实现基本的物品强化功能
- ✅ 支持 GUI 界面
- ✅ 集成经济系统
- ✅ 实现 EnderStorage 战利品仓库模块
- ✅ 支持自动拾取、白名单、容量解锁等功能

## 作者

**Arctain**

## 许可证

本项目为私有项目，未经授权不得使用。

---

**注意**：本插件需要 MMOItems 插件才能正常工作。请确保服务器已正确安装并配置 MMOItems。
