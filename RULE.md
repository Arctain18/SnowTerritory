# SnowTerritory 项目开发规范

供开发人员与 AI 辅助编码参考，确保代码可读性与可维护性。

---

## 1. 项目结构

```
src/main/java/top/arctain/snowTerritory/
├── Main.java                    # 插件入口
├── config/                      # 全局配置
├── commands/                    # 主命令
├── listeners/                  # 全局监听器
├── utils/                      # 工具类
└── {模块}/                     # 功能模块
    ├── {模块}Module.java       # 模块入口
    ├── command/                # 子命令
    ├── config/                 # 模块配置
    ├── gui/                    # GUI 界面
    ├── listener/               # 事件监听
    ├── service/                # 业务逻辑
    ├── data/                   # 数据模型、DAO
    └── storage/                # 存储实现（可选）
```

---

## 2. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类/接口 | PascalCase | `ReinforceModule`, `QuestService` |
| 方法/变量 | camelCase | `loadConfig`, `playerId` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LEVEL`, `DEFAULT_PATH` |
| 包名 | 全小写 | `top.arctain.snowTerritory.reinforce` |
| 配置键 | 小写+连字符 | `reinforce.cost`, `quest.reload-done` |

---

## 3. 代码风格

- **缩进**：4 空格
- **行宽**：单行不超过 120 字符，超则换行
- **大括号**：K&R 风格，左括号不换行
- **空行**：类成员间 1 空行，逻辑块间 1 空行

```java
// 方法换行：参数过多时对齐
public void register(String name, int level, boolean enabled) {
    // ...
}
```

---

## 4. 注释规范

- **仅保留必要注释**：复杂业务逻辑、非显而易见的设计决策、公共 API
- **类注释**：一行说明职责，复杂类可加 `<p>` 段落
- **方法注释**：仅对公共 API 或复杂逻辑添加，说明「做什么」而非「怎么做」
- **禁止**：逐行翻译代码、无信息量的注释

```java
/** 强化模块入口，负责初始化配置、服务与命令/监听注册。 */
public class ReinforceModule { }

/** 更新可用余额（扣除冻结保证金）。 */
public void updateAvailableBalance(BigDecimal isolatedMargin) { }
```

---

## 5. 模块开发

- 模块入口实现 `enable()`、`disable()`、`reload()`
- 通过 `PluginConfig.isModuleEnabled("moduleName")` 控制启用
- 配置路径：`plugins/SnowTerritory/{模块}/config.yml`
- 数据访问：DAO 接口 + 实现，存储使用 HikariCP + SQLite

---

## 6. 异常与空值

- 不吞异常：`catch` 中至少记录日志或重新抛出
- 空值检查：对外 API 入参做 null 校验，必要时抛出 `IllegalArgumentException`
- 可选依赖：用 `Optional` 或显式 null 检查，避免 NPE

---

## 7. 依赖与可测试性

- 服务类通过构造函数注入依赖，避免在方法内 `Bukkit.getPluginManager().getPlugin()`
- 核心逻辑与 Bukkit 解耦，便于单元测试
- 可选依赖：`provided` + `optional`，运行时检测插件存在性

---

## 8. 配置与消息

- 默认配置：从 `default-configs/` 资源复制，首次运行写入 `plugins/SnowTerritory/`
- 消息：统一通过 `MessageUtils.sendConfigMessage()` 发送
- 占位符：`{key}` 格式，如 `{level}`、`{player}`

### 8.1 主配置与模块配置不重复

- **主配置**（`config.yml`）仅保留全局消息：`command`、`item`、`help`、`separator`、`debug` 等
- **模块消息** 由各模块自身配置提供，不写入主配置

### 8.2 模块消息注册（不合并）

- 模块在 `enable()` 时调用 `MessageUtils.registerModuleMessages(prefix, messages)`
- 模块在 `disable()` 时调用 `MessageUtils.unregisterModuleMessages(prefix)`
- 模块在 `reload()` 时重新注册（覆盖旧消息）
- ConfigManager 提供 `getMessagesForMerge()`，返回 `messages.{模块}.*` 格式的 Map
- MessageUtils 查找消息时优先从模块注册表查找，再回退到主配置

---

## 9. 权限

- 权限前缀：`st.loot.*`、`st.quest.*` 等，与 `plugin.yml` 一致
- 命令执行前校验权限，失败时返回配置消息

---

## 10. 禁止事项

- 禁止使用 `System.out.println`，改用 `MessageUtils.logInfo()`
- 禁止硬编码魔法数字/字符串，提取为常量或配置
- 禁止在监听器或命令中写长逻辑，应委托给 Service 层

---

## 11. Commit 规范

采用 Conventional Commits 风格，语言使用中文，格式：

```
<type>(<scope>): <subject>

[可选 body]

[可选 footer]
```

### type

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `refactor` | 重构（不改变行为） |
| `perf` | 性能优化 |
| `style` | 代码风格（格式、空格等） |
| `docs` | 文档 |
| `chore` | 构建、配置、依赖等 |
| `test` | 测试 |

### scope（可选）

模块或区域，如 `reinforce`、`quest`、`enderstorage`、`stocks`、`config`、`utils`。

### subject

- 简短描述，50 字以内
- 使用祈使句，如「添加」「修复」「移除」
- 首字母小写，结尾不加句号

### 示例

```
feat(reinforce): 添加保护符最高等级校验
fix(quest): 修复材料提交时进度不更新
refactor(config): 统一 ConfigUtils.copyIfMissing
docs: 更新 README 部署说明
chore: 升级 Maven 依赖版本
```
