package top.arctain.snowTerritory.enderstorage.config;

/**
 * 默认配置内容，首次运行时写入插件目录。
 */
public final class DefaultFiles {

    private DefaultFiles() {
    }

    public static final String DEFAULT_CONFIG = """
            database:
              type: sqlite
              file: ender-storage.db
              mysql:
                host: localhost
                port: 3306
                database: enderstorage
                username: root
                password: root
              pool:
                maximum-pool-size: 4
                minimum-idle: 1
                connection-timeout: 30000

            features:
              auto-pickup: true
              cancel-entity-drop: true
              debug: false
              default-language: zh_CN
            """;

    public static final String DEFAULT_WHITELIST = """
            MM_DROPS:
              平原荒骨:
                lore:
                  - "test"
                  - "test2"
                default_max: 256
              平原腐肉:
                lore:
                  - "test"
                  - "test2"
                default_max: 256
              铁锭:
                lore:
                  - "test"
                default_max: 256
            """;

    public static final String DEFAULT_SIZE = """
            levels:
              - perm: st.loot.size.1
                slots: 9
              - perm: st.loot.size.2
                slots: 18
              - perm: st.loot.size.3
                slots: 27
            """;

    public static final String DEFAULT_STACK = """
            levels:
              - perm: st.loot.stack.1
                per_item_max: 256
              - perm: st.loot.stack.2
                per_item_max: 512
            """;

    public static final String DEFAULT_MESSAGES_ZH = """
            loot-gained: "&a+%amount%x %item% 已存入战利品仓库"
            no-permission: "&c✗ &f没有权限"
            not-whitelisted: "&c✗ &f该物品无法存入战利品仓库"
            exceed-limit: "&c✗ &f已达到上限"
            gui-title: "战利品仓库 (%page%)"
            reload-done: "&a✓ &f战利品仓库配置已重载"
            open: "&a✓ &f打开战利品仓库"
            """;
}

