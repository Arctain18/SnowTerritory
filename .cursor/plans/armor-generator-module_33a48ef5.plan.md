---
name: armor-generator-module
overview: 在 SnowTerritory 插件中新增一个基于 MMOItems 的制式防具系统，支持按套装名称随机生成四件不同品质与数值的防具，并通过可配置的 YML 文件控制数值分布、品质概率及文字消息。
todos:
  - id: analyze-existing-modules
    content: 阅读 Main、StfishModule、ReinforceModule 等以了解模块加载、配置与指令模式
    status: completed
  - id: design-armor-config
    content: 在 default-configs 下为 armor 设计 config.yml、sets.yml、messages/zh_CN.yml 的结构与示例
    status: completed
  - id: implement-armor-config-manager
    content: 实现 ArmorConfigManager 与相关数据类以加载和提供套装、防具部位、属性与品质配置
    status: completed
  - id: implement-random-and-generate-services
    content: 实现 ArmorRandomService 与 ArmorGenerateService，完成品质抽取、正态随机与 MMOItems 集成
    status: completed
  - id: implement-commands-and-module
    content: 实现 ArmorModule 与 ArmorCommand，接入主插件与权限系统，并支持 sn armor generate all <name>
    status: completed
  - id: test-and-tune
    content: 在测试环境中验证生成逻辑、随机分布与配置灵活性，并调整默认参数
    status: completed
isProject: false
---

## 目标

- **实现一个“制式防具”模块**：基于 MMOItems 生成成套防具，支持指令 `sn armor generate all [name]` 获取指定套装的四件防具（头、胸甲、护腿、鞋）。
- **支持多属性正态分布随机**：每套防具有“基础值”，四件部位按等比例系数计算出各自基础属性；实际生成时在基础值附近做正态随机，并受“品质”与“品质系数”影响。
- **高度可配置**：通过新建的 `default-configs` 子目录（仿照 `stfish` 结构）来配置文字消息、MMOItems 类型、名称列表、品质后缀、部位列表、数值范围与品质概率等。

## 现有结构调研与集成思路

- **阅读项目结构与模块模式**
  - 查看主插件入口 `top/arctain/snowTerritory/Main` 与已有模块（如 `ReinforceModule`, `StfishModule`）的注册方式，复用模块化模式。
  - 参考 `stfish` 模块在代码与资源层面的组织方式：
    - 代码：`top/arctain/snowTerritory/stfish/...`
    - 配置：`src/main/resources/default-configs/stfish/...`（如 `fish.yml`）。
- **参考类似指令与服务设计**
  - 查阅 `ReinforceCommand`, `StfishCommand`、相关 `ConfigManager` 和 `Service` 类，借鉴：
    - 指令注册与参数解析模式。
    - YML 配置加载、热重载（如果已有）、以及默认值填充方式。
    - 使用 MMOItems / Vault / 其他外部依赖的封装方式（例如服务层、适配器层）。

## 配置文件设计

- **新增配置目录结构**（示例）：
  - `[src/main/resources/default-configs/armor/config.yml]`：全局控制项与默认值，例如：
    - MMOItems 类型与子类型：`mmoitem-type`, `armor-type-list`（头/胸/腿/鞋对应的 MMOItems template 或 type/subtype）。
    - 部位顺序和键名映射：`slots: [HELMET, CHESTPLATE, LEGGINGS, BOOTS]`，以及与 MMOItems template 的映射。
    - 品质枚举与后缀：`qualities: COMMON(无后缀), RARE(精品), EPIC(极品)`，并配置品质概率与数值系数：
      - `probability`：基础权重或概率。
      - `value-multiplier`：品质对应的基础数值倍率。
    - 全局随机参数：正态分布相关配置，如：
      - `random.normal.enabled: true`
      - `random.normal.sigma-factor`：标准差为基础值的多少倍（例如 0.1 代表 ±10% 主体集中）。
      - 防止极端值的钳制比例（如 `min-multiplier`, `max-multiplier`）。
    - 公共消息路径：指令提示、错误信息等引用的 key 集合。
  - `[src/main/resources/default-configs/armor/messages/zh_CN.yml]`：消息文本（可按 locale 拓展）：
    - 指令帮助：`command.usage`, `command.help`。
    - 生成成功：`armor.generated-set`, `armor.generated-piece`。
    - 错误：`error.unknown-set`, `error.no-permission`, `error.missing-mmoitems` 等。
  - `[src/main/resources/default-configs/armor/sets.yml]`：套装与数值配置：
    - 每个套装一个 key，例如：
      - `sets.frost_guard`: 
        - `display-name`: "冰霜守卫"。
        - `base-stats`: 多属性基础值，例如：
          - `defense`, `max-health`, `tenacity`, `dodge` 等。
        - `slot-ratios`: 部位系数（按基础值等比例分配）：
          - `helmet: 0.25`, `chestplate: 0.4`, `leggings: 0.2`, `boots: 0.15`。
        - `stat-ranges`: 针对每个属性的全局约束，如 `min-multiplier`, `max-multiplier` 或绝对最小/最大。
        - 额外可选：每属性单独的随机波动控制（如防御波动大些，生命波动小些）。

## 代码结构设计

- **新模块包结构**
  - 创建 `top/arctain/snowTerritory/armor` 根包，内容包括：
    - `ArmorModule`：负责在插件启用时注册配置、指令等。
    - `command/ArmorCommand`：处理 `sn armor ...` 子命令。
    - `config/ArmorConfigManager`：加载 `config.yml`、`sets.yml`、`messages`。
    - `data`：
      - `ArmorSetDefinition`：描述一个套装的基础信息与属性分布。
      - `ArmorQuality`：品质枚举（包含后缀、概率、倍率等）。
      - `ArmorSlot`：头/胸/腿/鞋与 MMOItems 模板映射信息。
      - `ArmorStatRange` / `ArmorStats`：多属性基础值及范围约束结构。
    - `service`：
      - `ArmorRandomService`：实现正态分布随机逻辑、品质抽取、属性波动计算。
      - `ArmorGenerateService`：组合 `ArmorRandomService` 与 `MMOItems` 生成并发放物品。
    - `util/ArmorMessages`：封装消息 key 的获取与格式化（可重用项目已有消息工具）。
- **模块注册**
  - 在 `Main` 或对应模块注册中心中：
    - 实例化 `ArmorModule`，加载配置并注册指令。
    - 遵循已有模块风格，支持重载命令（如 `/sn reload armor`）时刷新配置缓存。

## 核心逻辑与算法

- **品质与随机数值计算**
  - 在 `ArmorRandomService` 中实现：
    - **品质抽取**：基于配置的权重/概率列表随机选择 `ArmorQuality`。
    - **基础属性计算**：
      - 从 `ArmorSetDefinition` 的 `base-stats` 读取每项属性基础值（例如 defense=100, max-health=50）。
      - 按部位系数（`slot-ratios`）计算部位基础值：
        - baseSlotStat = baseSetStat * slotRatio 。
      - 再乘品质倍率：
        - qualifiedBase = baseSlotStat * qualityMultiplier 。
    - **正态分布扰动**：
      - 使用近似正态分布（例如多次均匀取样平均）或引用已有工具类（如项目中已实现的随机工具）。
      - 计算扰动值：
        - finalStat = clamp(qualifiedBase * N(1, sigmaFactor)) ，并按配置做上下限钳制。
    - 为每个属性（defense、max-health 等）分别执行上述流程，得到最终 `ArmorStats`。
- **MMOItems 集成**
  - 根据 `config.yml` 中的映射信息找到对应的 MMOItems 类型与模板：
    - 例如 `type: ARMOR`, `subtype: HELMET`, 或直接使用预先定义的模板 ID。
  - 使用插件中已有的 MMOItems 封装（或直接调用 MMOItems API）来：
    - 以模板为基础克隆物品。
    - 将 `ArmorStats` 中的各项属性转成 MMOItems stat（如 `ARMOR`, `MAX_HEALTH`, `DODGE_RATE` 等）。
  - 返回 `ItemStack` 列表给调用方（指令处理器），并发放给玩家背包或掉落到脚下。

## 指令设计

- **指令结构**
  - 主命令：`/sn armor generate all <setName>`。
  - 权限：例如 `snowterritory.armor.generate`（在 `plugin.yml` 或对应命令注册处添加）。
- **流程**
  - 解析参数 `<setName>`：
    - 在 `ArmorConfigManager` 中查找对应 `ArmorSetDefinition`，若不存在：
      - 使用 `messages.error.unknown-set` 提示。
  - 依次为四个 `ArmorSlot`：`HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS` 调用 `ArmorGenerateService.generatePiece(player, setDefinition, slot)`：
    - 每个部位独立抽品质和随机数值。
  - 将生成结果展示给玩家：
    - 发送整套提示消息 `armor.generated-set`，可包含套装名与品质分布概要。

## 配置项可定制范围（映射到实现）

- **文字消息**
  - 全部消息文本从 `messages/zh_CN.yml` 读取，支持占位符（玩家名、套装名、品质等）。
- **MMOItems 类型与名称**
  - `config.yml` 中：
    - `mmoitems.type` / `subtype` 或 `template-id` 列表。
    - 装备名称列表：`set.display-name` 或 per-quality 前后缀。
    - 品质后缀名称：`qualities.RARE.suffix: " 精品"`，`qualities.EPIC.suffix: " 极品"`。
    - 护具类型列表：`slots` 与其对应的 MMOItems 模板键。
- **数值与范围**
  - `sets.yml` 中：
    - 每属性基础值 `base-stats`。
    - 全局或 per-stat 的 `min`, `max` 或倍率范围。
  - `config.yml` 中：
    - 全局正态扰动参数与默认范围策略。
- **品质概率与品质系数**
  - `config.yml` 中：
    - `qualities.<id>.probability: 0.7 / 0.25 / 0.05`。
    - `qualities.<id>.value-multiplier: 1.0 / 1.1 / 1.25`。

## 与现有代码风格对齐

- **遵守 `RULE.md` 与注释规范**
  - 仿照其他模块的代码风格与命名习惯（例如 service / manager / listener 的分层方式）。
  - 仅保留非显而易见的注释，用代码表达意图。

## 主要开发步骤（Todo）

1. **调研与准备**
  - 阅读 `Main` 与现有模块（尤其是 `stfish`、`reinforce`）代码，确认模块加载、配置管理与指令注册的统一模式。
2. **配置结构落地**
  - 在 `default-configs` 下创建 `armor` 目录与 `config.yml`, `sets.yml`, `messages/zh_CN.yml` 样例文件，填入最小可运行示例。
3. **数据模型与配置加载**
  - 实现 `ArmorQuality`, `ArmorSlot`, `ArmorStats`, `ArmorSetDefinition` 等数据类。
  - 实现 `ArmorConfigManager` 负责从 YML 解析映射为上述数据结构，并提供按套装名查询的接口。
4. **服务层实现**
  - 实现 `ArmorRandomService`（品质/数值随机与正态分布逻辑）。
  - 实现 `ArmorGenerateService`（调用随机服务与 MMOItems 生成物品）。
5. **命令与模块集成**
  - 实现 `ArmorCommand` 支持 `sn armor generate all <setName>`，并与权限、消息系统集成。
  - 实现 `ArmorModule` 并在插件主类中完成注册与配置初始化。
6. **测试与调优**
  - 在测试服务器上验证：
    - 不同套装、不同品质的生成效果。
    - 正态分布范围是否合理（极端值概率与期望一致）。
    - 消息与多语言覆盖情况。
  - 根据需要微调默认配置与随机参数。

