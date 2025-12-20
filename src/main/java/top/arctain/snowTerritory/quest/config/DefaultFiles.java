package top.arctain.snowTerritory.quest.config;

/**
 * 默认配置内容，首次运行时写入插件目录。
 */
public final class DefaultFiles {

    private DefaultFiles() {
    }

    public static final String DEFAULT_CONFIG = """
            features:
              debug: false
              default-language: zh_CN
            """;

    public static final String DEFAULT_MESSAGES_ZH = """
            quest-accepted: "&a✓ &f已接取任务: &e%quest%"
            quest-completed: "&a✓ &f任务完成！获得奖励: &e%rewards%"
            quest-expired: "&c✗ &f任务已过期"
            quest-progress: "&7任务进度: &e%current%&7/&e%required%"
            quest-not-found: "&c✗ &f未找到任务"
            quest-already-active: "&c✗ &f你已有进行中的任务"
            no-permission: "&c✗ &f没有权限"
            player-only: "&c✗ &f此命令仅限玩家使用"
            bounty-announcement: "&6[悬赏任务] &e%quest% &7- &f%description%"
            reward-questpoint: "&a+%amount% 成就点数"
            reward-currency: "&a+%amount%x %currency%"
            material-submitted: "&a✓ &f已提交 &e%amount%x %item% &7(进度: %current%/%required%)"
            quest-list-header: "&6=== 你的任务列表 ==="
            quest-list-item: "&7- &e%quest% &7(进度: %current%/%required%)"
            quest-list-empty: "&7暂无进行中的任务"
            """;

    public static final String DEFAULT_REWARDS_DEFAULT = """
            default:
              questpoint: 12
              currency:
                amount: 1
                type: CURRENCY
                stack-0: SD_0
                stack-1: SD_1
                stack-2: SD_2
              messages:
                # 任务完成提示 (%rating%=评级, %multiplier%=倍数, %questpoint%=成就点, %currency%=货币数量)
                completion: "&a✓ &f任务完成！评级: &e%rating% &7(奖励倍数: %multiplier%x) &f获得: &b%questpoint% 成就点 &f+ &e%currency% 货币"
            """;

    public static final String DEFAULT_REWARDS_LEVEL = """
            level:
              1: 1.0
              2: 1.5
              3: 2.0
              4: 2.5
              5: 3.0
            """;

    public static final String DEFAULT_BONUS_TIME_BONUS = """
            1:
              display: "&c&lUltra&r {#E260FC>}Ext{#9560FC<>}reme+{#607AFC<}"
              max-limit: 300000
              time-bonus: 2.0
            2:
              display: "{#E260FC>}Ext{#9560FC<>}reme{#607AFC<}"
              max-limit: 600000
              time-bonus: 1.8
            3:
              display: "&{#607AFC}Master"
              max-limit: 900000
              time-bonus: 1.5
            4:
              display: "&fNormal"
              max-limit: 1200000
              time-bonus: 1.2
            5:
              display: "&{#FCE260}Adequate"
              max-limit: 1800000
              time-bonus: 1.0
            6:
              display: "&4Poor"
              max-limit: -1
              time-bonus: 0.8
            """;

    public static final String DEFAULT_MATERIALS_WHITELIST = """
            materials:
              MM_DROPS:
                优质狼皮:
                  min: 16
                  max: 256
                  material-level: 1
                瓶装兽血:
                  min: 16
                  max: 256
                  material-level: 1
                狼王獠牙:
                  min: 8
                  max: 128
                  material-level: 2
            """;

    public static final String DEFAULT_BOUNTY_CONFIG = """
            bounty:
              # 发布间隔区间（分钟），上一个悬赏结束到下一个悬赏发布的间隔
              interval-min: 20
              interval-max: 40
              # 悬赏系数（奖励倍数）
              bounty-bonus: 1.5
              # 允许的任务类型: MATERIAL, KILL, BOTH
              allowed-types: MATERIAL
              # 消息配置
              messages:
                # 悬赏公告 (%material%=物品名, %amount%=数量)
                announcement: "&6[悬赏任务] &e收集 %material% x%amount% &7- &f完成任务可获得丰厚奖励！"
            """;

    public static final String DEFAULT_TASKS_MATERIAL = """
            # 材料任务配置
            # 任务将从 materials/whitelist.yml 中随机选择材料
            material:
              # 默认时间限制（毫秒），-1表示无限制
              default-time-limit: -1
            """;

    public static final String DEFAULT_TASKS_KILL = """
            # 击杀任务配置 (TODO)
            # 此功能尚未实现
            kill:
              # 默认时间限制（毫秒），-1表示无限制
              default-time-limit: 3600000
            """;
}

