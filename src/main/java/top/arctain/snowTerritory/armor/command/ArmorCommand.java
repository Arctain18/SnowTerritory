package top.arctain.snowTerritory.armor.command;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.data.ArmorSetDefinition;
import top.arctain.snowTerritory.armor.service.ArmorConfirmSessionService;
import top.arctain.snowTerritory.armor.service.ArmorCostService;
import top.arctain.snowTerritory.armor.service.ArmorGenerateService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArmorCommand implements CommandExecutor, TabCompleter {

    private final ArmorGenerateService generateService;
    private final ArmorConfigManager configManager;
    private final ArmorCostService costService;
    private final ArmorConfirmSessionService confirmSessionService;

    public ArmorCommand(ArmorGenerateService generateService, ArmorConfigManager configManager,
                        ArmorCostService costService, ArmorConfirmSessionService confirmSessionService) {
        this.generateService = generateService;
        this.configManager = configManager;
        this.costService = costService;
        this.confirmSessionService = confirmSessionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return true;
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sub.equals("armor")) {
            return false;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "generate" -> handleGenerate(sender, args);
            case "confirm" -> handleConfirm(sender, args);
            case "cancel" -> handleCancel(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleGenerate(CommandSender sender, String[] args) {
        if (args.length < 5 || args.length > 6) {
            sendUsage(sender);
            return true;
        }
        String mode = args[2].toLowerCase(Locale.ROOT);
        if (!mode.equals("all")) {
            sendUsage(sender);
            return true;
        }
        String setId = args[3];
        String profile = args[4].toLowerCase(Locale.ROOT);

        ArmorSetDefinition setDef = configManager.getSet(setId);
        if (setDef == null) {
            MessageUtils.sendConfigMessage(sender, "armor.command-unknown-set",
                    "&c✗ &f未找到套装: &e{set}", "set", setId);
            return true;
        }
        int[] profileWeights = configManager.getGenerationProfileWeights(profile);
        if (profileWeights == null) {
            MessageUtils.sendConfigMessage(sender, "armor.command-invalid-profile",
                    "&c✗ &f无效概率模板: &e{profile}", "profile", profile);
            return true;
        }

        Player target;
        boolean selfGenerate;
        if (args.length == 6) {
            target = resolveOnlinePlayer(args[5]);
            if (target == null) {
                MessageUtils.sendConfigMessage(sender, "armor.command-target-not-found",
                        "&c✗ &f未找到在线玩家: &e{name}", "name", args[5]);
                return true;
            }
            selfGenerate = sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId());
        } else {
            if (!(sender instanceof Player player)) {
                MessageUtils.sendConfigMessage(sender, "armor.command-console-need-player",
                        "&c✗ &f控制台执行必须指定在线玩家名");
                return true;
            }
            target = player;
            selfGenerate = true;
        }

        if (!selfGenerate && !canGenerateForOthers(sender)) {
            MessageUtils.sendConfigMessage(sender, "armor.command-no-permission",
                    "&c✗ &f您没有权限使用该命令");
            return true;
        }

        if (selfGenerate) {
            openConfirmPrompt(target, setDef, profile, profileWeights);
            return true;
        }

        return executeGenerate(sender, target, setId, profileWeights);
    }

    private boolean handleConfirm(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "armor.command-player-only-confirm",
                    "&c✗ &f该操作仅限玩家使用");
            return true;
        }
        if (args.length < 3) {
            MessageUtils.sendConfigMessage(player, "armor.confirm-token-missing",
                    "&c✗ &f缺少确认令牌，请重新执行生成命令");
            return true;
        }
        String token = args[2];
        ArmorConfirmSessionService.PendingGenerate pending = confirmSessionService.consume(token, player);
        if (pending == null) {
            MessageUtils.sendConfigMessage(player, "armor.confirm-expired",
                    "&c✗ &f确认已过期，请重新执行命令");
            return true;
        }

        ArmorSetDefinition setDef = configManager.getSet(pending.setId());
        if (setDef == null) {
            MessageUtils.sendConfigMessage(player, "armor.command-unknown-set",
                    "&c✗ &f未找到套装: &e{set}", "set", pending.setId());
            return true;
        }
        ArmorCostService.CostPreview latest = costService.buildPreview(player, setDef);
        if (!latest.enough()) {
            MessageUtils.sendConfigMessage(player, "armor.confirm-insufficient",
                    "&c✗ &f货币不足，无法生成。SL 缺少: &e{sl} &7| QP 缺少: &e{qp}",
                    "sl", num(latest.slShortage()), "qp", num(latest.qpShortage()));
            return true;
        }
        if (!costService.tryCharge(player, latest)) {
            MessageUtils.sendConfigMessage(player, "armor.confirm-charge-failed",
                    "&c✗ &f扣费失败，请检查经济插件状态");
            return true;
        }

        return executeGenerate(player, player, pending.setId(), pending.weights());
    }

    private boolean handleCancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (args.length < 3) {
            MessageUtils.sendConfigMessage(player, "armor.cancel-token-missing",
                    "&e⚠ &f已取消当前生成请求");
            return true;
        }
        boolean cancelled = confirmSessionService.cancel(args[2], player);
        if (cancelled) {
            MessageUtils.sendConfigMessage(player, "armor.confirm-cancelled",
                    "&e⚠ &f已取消本次防具生成");
        } else {
            MessageUtils.sendConfigMessage(player, "armor.confirm-expired",
                    "&c✗ &f确认已过期，请重新执行命令");
        }
        return true;
    }

    private void openConfirmPrompt(Player player, ArmorSetDefinition setDef, String profile, int[] weights) {
        ArmorCostService.CostPreview preview = costService.buildPreview(player, setDef);
        String setDisplay = setDef.getDisplayName() != null && !setDef.getDisplayName().isBlank() ? setDef.getDisplayName() : setDef.getId();
        ArmorConfirmSessionService.PendingGenerate pending = confirmSessionService.create(
                player, setDef.getId(), setDisplay, profile, weights, preview
        );

        String vipSuffix = preview.vip().active()
                ? " &9| " + preview.vip().name() + " &a-" + preview.vip().percentOff() + "%"
                : "";
        String previewText = MessageUtils.getConfigMessage("armor.confirm-preview",
                "&b✦ &f即将为你生成制式防具：&e{set} &7(profile: &f{profile}&7)\n" +
                        "&7玩家: &f{name} &8| &7等级: &f{level} &8| &7职业: &f{class}\n" +
                        "&7本次消耗: Sl &8{rawSl} &7-> &a{finalSl} &7QP &8{rawQp} &7-> &a{finalQp}{vip}",
                "set", setDisplay,
                "profile", profile,
                "name", preview.playerName(),
                "level", String.valueOf(preview.level()),
                "class", preview.className(),
                "rawSl", num(preview.rawSl()),
                "finalSl", num(preview.finalSl()),
                "rawQp", num(preview.rawQp()),
                "finalQp", num(preview.finalQp()),
                "vip", vipSuffix);
        MessageUtils.sendRaw(player, previewText);

        String confirmCmd = "/sn armor confirm " + pending.token();
        String cancelCmd = "/sn armor cancel " + pending.token();
        String confirmHover = buildConfirmHover(preview);
        String cancelHover = MessageUtils.colorize("&7点击取消本次生成");

        Component confirm = LegacyComponentSerializer.legacySection().deserialize(MessageUtils.colorize("&a&l[确认生成]"))
                .clickEvent(ClickEvent.runCommand(confirmCmd))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(confirmHover)));
        Component spacer = LegacyComponentSerializer.legacySection().deserialize(MessageUtils.colorize(" &8| "));
        Component cancel = LegacyComponentSerializer.legacySection().deserialize(MessageUtils.colorize("&c&l[取消]"))
                .clickEvent(ClickEvent.runCommand(cancelCmd))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(cancelHover)));
        player.sendMessage(confirm.append(spacer).append(cancel));
    }

    private String buildConfirmHover(ArmorCostService.CostPreview preview) {
        if (!preview.economyEnabled() || !preview.pointsEnabled()) {
            return MessageUtils.colorize("&c经济插件未就绪，无法执行生成");
        }
        if (preview.enough()) {
            return MessageUtils.colorize("&a点击确认并扣费生成");
        }
        return MessageUtils.colorize(
                "&c货币不足，无法确认\n" +
                        "&7SL 当前: &f" + num(preview.slBalance()) + " &8/ 缺少: &c" + num(preview.slShortage()) + "\n" +
                        "&7QP 当前: &f" + num(preview.qpBalance()) + " &8/ 缺少: &c" + num(preview.qpShortage())
        );
    }

    private boolean executeGenerate(CommandSender feedback, Player target, String setId, int[] qualityWeights) {
        var items = generateService.generateFullSet(target, feedback, setId, qualityWeights);
        if (items.isEmpty()) return true;
        for (ItemStack item : items) {
            var overflow = target.getInventory().addItem(item);
            overflow.values().forEach(stack -> {
                if (stack != null && !stack.getType().isAir()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), stack);
                }
            });
        }
        var setDef = configManager.getSet(setId);
        String display = setDef != null && setDef.getDisplayName() != null && !setDef.getDisplayName().isBlank()
                ? setDef.getDisplayName()
                : setId;
        MessageUtils.sendConfigMessage(feedback, "armor.generate-set-success",
                "&a✓ &f已为 &e{player} &f生成 &e{set-display} &f套装",
                "player", target.getName(), "set-display", display);
        return true;
    }

    private static void sendUsage(CommandSender sender) {
        MessageUtils.sendConfigMessage(sender, "armor.command-usage",
                "&e用法: &f/sn armor generate all <套装ID> <profile> &7[&f玩家名&7]");
    }

    /** 代发给别人时，控制台和有权限玩家可执行。 */
    private static boolean canGenerateForOthers(CommandSender sender) {
        return !(sender instanceof Player p) || p.hasPermission("st.armor.generate") || p.isOp();
    }

    private static Player resolveOnlinePlayer(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            if ("armor".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                result.add("armor");
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("armor")) {
            if ("generate".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                result.add("generate");
            }
            if ("confirm".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                result.add("confirm");
            }
            if ("cancel".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                result.add("cancel");
            }
            return result;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("generate")) {
            if ("all".startsWith(args[2].toLowerCase(Locale.ROOT))) {
                result.add("all");
            }
            return result;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("armor")
                && args[1].equalsIgnoreCase("generate") && args[2].equalsIgnoreCase("all")) {
            String prefix = args[3].toLowerCase(Locale.ROOT);
            configManager.getAllSets().keySet().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .forEach(result::add);
            return result;
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("armor")
                && args[1].equalsIgnoreCase("generate") && args[2].equalsIgnoreCase("all")) {
            String prefix = args[4].toLowerCase(Locale.ROOT);
            configManager.getGenerationProfiles().keySet().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .forEach(result::add);
            return result;
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("generate")
                && args[2].equalsIgnoreCase("all")) {
            if (!canGenerateForOthers(sender)) {
                return result;
            }
            String prefix = args[5].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(p.getName());
                }
            }
            return result;
        }
        return result;
    }

    private static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.0001) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
