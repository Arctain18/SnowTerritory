# SnowTerritory 插件逻辑流程图

本文档详细描述了 SnowTerritory 插件的完整逻辑判定流程，包括 Reinforce（强化）模块和 EnderStorage（末影存储）模块。

## 目录

1. [插件启动流程](#1-插件启动流程)
2. [Reinforce 模块命令执行流程](#2-reinforce-模块命令执行流程)
3. [EnderStorage 模块命令执行流程](#3-enderstorage-模块命令执行流程)
4. [GUI 交互流程](#4-gui-交互流程)
5. [强化判定核心流程](#5-强化判定核心流程)
6. [概率计算流程](#6-概率计算流程)
7. [EnderStorage 操作流程](#7-enderstorage-操作流程)

---

## 1. 插件启动流程

```mermaid
flowchart TD
    A[服务器启动] --> B[调用 onEnable]
    B --> C[初始化 NBTUtils]
    C --> D[初始化 MessageUtils]
    D --> E{检查依赖插件}
    E -->|MMOItems 不存在| F[记录严重错误]
    F --> G[禁用插件]
    G --> H[结束]
    E -->|MMOItems 存在| I[检查 Vault]
    I -->|Vault 不存在| J[记录警告]
    I -->|Vault 存在| K[检查 PlayerPoints]
    J --> K
    K -->|PlayerPoints 不存在| L[记录警告]
    K -->|PlayerPoints 存在| M[创建 PluginConfig]
    L --> M
    M --> N[加载配置文件]
    N --> O{配置文件存在?}
    O -->|否| P[创建默认配置]
    O -->|是| Q[读取配置]
    P --> Q
    Q --> R[初始化 EnderStorageModule]
    R --> S[加载 EnderStorage 配置]
    S --> T[初始化数据库]
    T --> U[注册 EnderStorage 命令和监听]
    U --> V[注册主命令 snowterritory]
    V --> W[注册 GUIListener]
    W --> X[注册 ItemEditListener]
    X --> Y[输出启动信息]
    Y --> Z[插件启用完成]
```

---

## 2. Reinforce 模块命令执行流程

### 2.1 /sn r 或 /sn reinforce 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn r] --> B{是否为玩家?}
    B -->|否| C[发送错误消息]
    C --> D[结束]
    B -->|是| E{检查权限}
    E -->|无权限且非OP| F[发送权限错误]
    F --> D
    E -->|有权限或OP| G{是否有玩家名参数?}
    G -->|是| H{检查 openforothers 权限}
    H -->|无权限| F
    H -->|有权限| I[查找目标玩家]
    I --> J{玩家是否存在?}
    J -->|否| K[发送玩家不存在错误]
    K --> D
    J -->|是| L[为目标玩家打开GUI]
    G -->|否| M[为自己打开GUI]
    L --> N[创建 ItemEditorGUI]
    M --> N
    N --> O[调用 openGUI]
    O --> P[创建 Inventory]
    P --> Q[加载自定义槽位装饰]
    Q --> R[创建确认按钮]
    R --> S[创建取消按钮]
    S --> T[打开GUI给玩家]
    T --> U[等待玩家操作]
```

### 2.2 /sn reload 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn reload] --> B{检查权限}
    B -->|无权限且非OP| C[发送权限错误]
    C --> D[结束]
    B -->|有权限或OP| E[调用 config.reloadConfig]
    E --> F[重新加载配置文件]
    F --> G[更新所有配置项]
    G --> H{是否有 EnderStorageModule?}
    H -->|是| I[调用 enderModule.reload]
    I --> J[重载 EnderStorage 配置]
    J --> K[发送成功消息]
    H -->|否| K
    K --> D
```

### 2.3 /sn checkid 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn checkid] --> B{是否为玩家?}
    B -->|否| C[发送错误消息]
    C --> D[结束]
    B -->|是| E{检查权限}
    E -->|无权限且非OP| F[发送权限错误]
    F --> D
    E -->|有权限或OP| G[获取手中物品]
    G --> H{是否为 MMOItem?}
    H -->|否| I[发送错误: 不是 MMOItems 物品]
    I --> D
    H -->|是| J[获取物品 Type 和 ID]
    J --> K[发送物品信息]
    K --> D
```

---

## 3. EnderStorage 模块命令执行流程

### 3.1 /sn es 或 /stes 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn es] --> B{是否为玩家?}
    B -->|否| C[发送错误消息]
    C --> D[结束]
    B -->|是| E{检查权限 st.loot.use}
    E -->|无权限| F[发送权限错误]
    F --> D
    E -->|有权限| G[获取 EnderStorageCommand]
    G --> H{命令是否存在?}
    H -->|否| I[发送功能未启用错误]
    I --> D
    H -->|是| J[调用 openSelf]
    J --> K[打开 LootStorageGUI]
    K --> L[加载玩家库存数据]
    L --> M[显示第一页]
    M --> N[等待玩家操作]
```

### 3.2 /sn es reload 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn es reload] --> B{检查权限 st.loot.admin}
    B -->|无权限| C[发送权限错误]
    C --> D[结束]
    B -->|有权限| E[调用 service.reload]
    E --> F[重载配置管理器]
    F --> G[重载白名单]
    G --> H[重载进度配置]
    H --> I[重载消息配置]
    I --> J[刷新缓存]
    J --> K[发送成功消息]
    K --> D
```

### 3.3 /sn es give 命令流程

```mermaid
flowchart TD
    A[玩家输入 /sn es give <player> <itemKey> <amount>] --> B{检查权限 st.loot.admin}
    B -->|无权限| C[发送权限错误]
    C --> D[结束]
    B -->|有权限| E{参数数量是否足够?}
    E -->|否| F[发送用法错误]
    F --> D
    E -->|是| G[解析目标玩家]
    G --> H{玩家是否存在?}
    H -->|否| I[发送玩家不存在错误]
    I --> D
    H -->|是| J[解析物品 key]
    J --> K[解析数量]
    K --> L{数量是否为数字?}
    L -->|否| M[发送数量格式错误]
    M --> D
    L -->|是| N[解析玩家权限等级]
    N --> O[计算单品上限]
    O --> P[计算仓库容量]
    P --> Q[调用 service.add]
    Q --> R[发送成功消息]
    R --> D
```

---

## 4. GUI 交互流程

### 4.1 Reinforce GUI 交互

```mermaid
flowchart TD
    A[玩家在GUI中操作] --> B[触发 InventoryClickEvent]
    B --> C{是否为玩家?}
    C -->|否| D[忽略事件]
    C -->|是| E{检查GUI标题}
    E -->|不是本插件GUI| D
    E -->|是本插件GUI| F[获取点击槽位]
    F --> G{是否为可编辑槽位?}
    G -->|是| H[允许操作]
    G -->|否| I{槽位在GUI范围内?}
    I -->|是| J[取消事件]
    I -->|否| H
    H --> K{点击的物品}
    K -->|确认按钮| L[调用 applyReinforce]
    K -->|取消按钮| M[关闭GUI]
    K -->|其他| N[正常处理]
    M --> O[发送取消消息]
    L --> P[进入强化流程]
    J --> D
    N --> D
    O --> D
```

### 4.2 EnderStorage GUI 交互

```mermaid
flowchart TD
    A[玩家在仓库GUI中操作] --> B[触发 InventoryClickEvent]
    B --> C{是否为 LootHolder?}
    C -->|否| D[忽略事件]
    C -->|是| E[取消事件]
    E --> F{是否为玩家?}
    F -->|否| D
    F -->|是| G[获取点击的物品]
    G --> H{物品是否有 itemKey?}
    H -->|否| D
    H -->|是| I{是否为翻页按钮?}
    I -->|是| J[解析目标页码]
    J --> K[打开目标页面]
    K --> D
    I -->|否| L[获取 WhitelistEntry]
    L --> M{Entry 是否存在?}
    M -->|否| D
    M -->|是| N[计算点击数量]
    N --> O{点击类型}
    O -->|左键/Shift左键| P[存入操作]
    O -->|右键/Shift右键| Q[取出操作]
    P --> R[从背包移除物品]
    R --> S{移除成功?}
    S -->|否| T[发送无物品错误]
    S -->|是| U[计算权限等级]
    U --> V[调用 service.add]
    V --> W[发送成功消息]
    Q --> X[获取当前库存]
    X --> Y{库存是否足够?}
    Y -->|否| Z[发送库存不足错误]
    Y -->|是| AA[生成物品]
    AA --> AB[调用 service.consume]
    AB --> AC[添加到玩家背包]
    AC --> AD[发送成功消息]
    T --> AE[刷新GUI]
    W --> AE
    Z --> AE
    AD --> AE
    AE --> D
```

---

## 5. 强化判定核心流程

```mermaid
flowchart TD
    A[玩家点击确认按钮] --> B[调用 applyReinforce]
    B --> C[获取武器槽位物品]
    C --> D[获取保护符槽位物品]
    D --> E[获取强化符槽位物品]
    E --> F[获取6个材料槽位物品]
    F --> G{武器是否为MMOItem?}
    G -->|否| H[发送错误: 请放置有效MMO物品]
    H --> Z[结束]
    G -->|是| I{物品是否可强化?}
    I -->|否| J[发送错误: 此物品不可强化]
    J --> Z
    I -->|是| K{检查Vault金币}
    K -->|启用且余额不足| L[发送错误: 金币不足]
    L --> Z
    K -->|通过或未启用| M{检查PlayerPoints}
    M -->|启用且余额不足| N[发送错误: 点券不足]
    N --> Z
    M -->|通过或未启用| O{检查材料数量}
    O -->|材料不足| P[发送错误: 材料不足]
    P --> Z
    O -->|材料足够| Q[扣除所有消耗]
    Q --> R[扣除Vault金币]
    R --> S[扣除PlayerPoints点券]
    S --> T[消耗所有材料]
    T --> U[消耗保护符]
    U --> V[消耗强化符]
    V --> W[获取当前强化等级]
    W --> X[计算基础成功率]
    X --> Y{是否有强化符?}
    Y -->|是| AA[成功率 +0.1]
    Y -->|否| AB[保持基础成功率]
    AA --> AC{是否有保护符?}
    AB --> AC
    AC -->|是| AD[失败降级概率 = 0]
    AC -->|否| AE[使用配置的失败降级概率]
    AD --> AF[执行概率判定]
    AE --> AF
    AF --> AG[进入概率计算流程]
    AG --> AH{判定结果}
    AH -->|成功| AI[修改属性: 提升]
    AH -->|失败降级| AJ[修改属性: 降低]
    AH -->|维持| AK[不修改属性]
    AI --> AL[更新物品名称: 等级+1]
    AJ --> AM[更新物品名称: 等级-1]
    AK --> AN[保持原等级]
    AL --> AO[发送成功消息]
    AM --> AP[发送失败消息]
    AN --> AQ[发送维持消息]
    AO --> AR[更新GUI中的物品]
    AP --> AR
    AQ --> AR
    AR --> Z
```

---

## 6. 概率计算流程

```mermaid
flowchart TD
    A[调用 attemptReinforce] --> B[生成随机数 0.0-1.0]
    B --> C{随机数 <= 成功率?}
    C -->|是| D[返回 SUCCESS]
    C -->|否| E{随机数 <= 成功率+维持概率?}
    E -->|是| F[返回 MAINTAIN]
    E -->|否| G{随机数 <= 成功率+维持概率+失败降级概率?}
    G -->|是| H[返回 FAIL_DEGRADE]
    G -->|否| I[返回 MAINTAIN 默认]
    D --> J[强化成功分支]
    F --> K[强化维持分支]
    H --> L[强化失败降级分支]
    I --> K
```

---

## 7. EnderStorage 操作流程

### 7.1 自动拾取流程

```mermaid
flowchart TD
    A[怪物死亡] --> B[触发 EntityDeathEvent]
    B --> C{是否有击杀者?}
    C -->|否| D[结束]
    C -->|是| E{击杀者是否有 st.loot.auto 权限?}
    E -->|否| D
    E -->|是| F[遍历掉落物]
    F --> G{还有掉落物?}
    G -->|否| D
    G -->|是| H[获取掉落物]
    H --> I[调用 service.matchItemKey]
    I --> J{是否在白名单中?}
    J -->|否| K[保留掉落物]
    K --> G
    J -->|是| L[计算权限等级]
    L --> M[计算单品上限]
    M --> N[计算仓库容量]
    N --> O[调用 service.add]
    O --> P[发送入库消息]
    P --> Q{是否取消掉落?}
    Q -->|是| R[移除掉落物]
    Q -->|否| K
    R --> G
```

### 7.2 存入物品流程

```mermaid
flowchart TD
    A[玩家左键点击仓库槽位] --> B[获取点击数量]
    B --> C[从背包查找匹配物品]
    C --> D{找到物品?}
    D -->|否| E[发送无物品错误]
    E --> F[结束]
    D -->|是| G[计算可存入数量]
    G --> H[从背包移除物品]
    H --> I[计算权限等级]
    I --> J[计算单品上限]
    J --> K[计算仓库容量]
    K --> L[调用 service.add]
    L --> M{是否成功?}
    M -->|否| N[发送错误消息]
    M -->|是| O[发送成功消息]
    N --> P[刷新GUI]
    O --> P
    P --> F
```

### 7.3 取出物品流程

```mermaid
flowchart TD
    A[玩家右键点击仓库槽位] --> B[获取点击数量]
    B --> C[获取 WhitelistEntry]
    C --> D[调用 service.getAmount]
    D --> E{库存是否足够?}
    E -->|否| F[发送库存不足错误]
    F --> G[结束]
    E -->|是| H[计算实际取出数量]
    H --> I[生成物品]
    I --> J{物品生成成功?}
    J -->|否| K[发送错误消息]
    J -->|是| L[调用 service.consume]
    L --> M[添加到玩家背包]
    M --> N[发送成功消息]
    K --> O[刷新GUI]
    N --> O
    O --> G
```

---

## 关键判定点总结

### 1. 权限判定
- Reinforce 模块：玩家必须拥有 `mmoitemseditor.edit` 权限或是 OP
- EnderStorage 模块：玩家必须拥有 `st.loot.use` 权限才能打开仓库
- 管理命令：需要 `st.loot.admin` 权限

### 2. 物品判定
- 强化物品必须存在且不为空气
- 强化物品必须是有效的 MMOItems 物品
- 强化物品必须在可强化列表中（如果列表不为空）
- 存储物品必须在白名单中

### 3. 消耗判定
- Vault 金币：如果启用，必须足够
- PlayerPoints 点券：如果启用，必须足够
- 材料：必须达到配置的数量要求

### 4. 概率判定
- 成功率：根据当前等级从配置读取
- 强化符：增加 0.1 成功率
- 保护符：将失败降级概率设为 0
- 维持概率：从配置读取

### 5. 容量判定
- 仓库容量：根据 `st.loot.size.X` 权限计算
- 单品上限：根据 `st.loot.stack.X` 权限计算
- 存入时检查容量和上限

### 6. 结果处理
- **强化成功**：属性提升，等级+1
- **强化失败降级**：属性降低，等级-1（有保护符则不降级）
- **强化维持**：属性不变，等级不变
- **存入成功**：物品从背包移除，添加到仓库
- **取出成功**：物品从仓库移除，添加到背包

---

## 配置影响点

1. **成功率配置**：影响 `attemptReinforce` 的判定
2. **消耗配置**：影响经济检查逻辑
3. **GUI配置**：影响界面布局和槽位
4. **可强化物品列表**：影响 `isReinforceable` 判定
5. **白名单配置**：影响物品是否可以存入仓库
6. **容量解锁配置**：影响仓库容量和单品上限

---

**流程图说明**：
- 菱形：判定节点（if/else）
- 矩形：处理节点（方法调用）
- 圆角矩形：开始/结束节点
- 箭头：流程方向

