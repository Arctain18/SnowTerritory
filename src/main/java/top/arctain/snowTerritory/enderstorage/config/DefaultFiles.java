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

    // plugins/SnowTerritory/enderstorage/gui.yml
    public static final String DEFAULT_GUI = """
            gui:
              # GUI标题 支持chatcolor解析
              title: "[&{#3ab835}&l末影&{#6dc97f}&l存储&8]"

              # GUI大小 (必须是9的倍数，如9, 18, 27, 36, 45, 54)
              size: 54

              # 槽位配置 (0-53)
              slots:
                # 上一页
                previous-page: 48
                # 下一页
                next-page: 50

                # 物品槽位 (支持范围表达式)
                material-slots: [ "28-34", "37-43" ]

                # 装饰槽位
                decoration-slots:
                  "0-17":
                    material: "GREEN_STAINED_GLASS_PANE"
                    name: "&{#d6fff6}&m-&{#fffad6}&m=&{#FFFFFF}&m-&8 [&{#3ab835}末影&{#6dc97f}存储&8] &{#FFFFFF}&m-&{#fffad6}&m=&{#d6fff6}&m-"
                    lore:
                      - ""
                  "18-53":
                    material: "BLACK_STAINED_GLASS_PANE"
                    name: "&{#d6fff6}&m-&{#fffad6}&m=&{#FFFFFF}&m-&8 [&{#3ab835}末影&{#6dc97f}存储&8] &{#FFFFFF}&m-&{#fffad6}&m=&{#d6fff6}&m-"
                    lore:
                      - ""
              
              # 默认物品 lore 模板，占位符: {amount} {max}
              default-lore:
                - "&7数量: &e{amount} / {max}"
                - "&8| &7左键 ▸ 存入 8"
                - "&8| &7SHIFT+左键 ▸ 存入 64"
                - "&8| &7右键 ▸ 取出 8"
                - "&8| &7中键 ▸ 取出 64"

              # 物品顺序与分组（只展示出现在这里并且在白名单中的物品）
              materials:
                MM_DROPS:
                  优质狼皮:
                    max: 256
                  瓶装兽血:
                    max: 256
                  狼王獠牙:
                    max: 128
              # 注意:
              # 1. 如果物品配置为数字（如 "优质狼皮: 256"），等价于 {max: 256}，同样会使用上面的 default-lore
              # 2. 如果某个物品需要单独的提示，可以在该物品下增加 lore 节点覆盖默认值:
              #    示例:
              #      某物品:
              #        max: 64
              #        lore:
              #          - "&7自定义第一行"
              #          - "&7自定义第二行"
            """;

    public static final String DEFAULT_MESSAGES_ZH = """
            loot-gained: "&a+{amount}x {item} 已存入战利品仓库"
            no-permission: "&c✗ &f没有权限"
            not-whitelisted: "&c✗ &f该物品无法存入战利品仓库"
            exceed-limit: "&c✗ &f已达到上限"
            gui-title: "战利品仓库 ({page})"
            reload-done: "&a✓ &f战利品仓库配置已重载"
            open: "&a✓ &f打开战利品仓库"
            """;
}

