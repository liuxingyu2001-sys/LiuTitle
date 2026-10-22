package com.liuxingyu.liutitle.command;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.LiuTitle;
import com.liuxingyu.liutitle.model.BuyType;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.service.ImportService;
import com.liuxingyu.liutitle.service.PlayerTitleService;
import com.liuxingyu.liutitle.service.TitleService;
import com.liuxingyu.liutitle.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /liutitle 命令。 */
public final class TitleCommand implements CommandExecutor, TabCompleter {

    private final LiuTitle plugin;
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final ImportService importService;
    private final Msg msg;

    public TitleCommand(LiuTitle plugin) {
        this.plugin = plugin;
        this.titleService = plugin.titles();
        this.playerTitleService = plugin.playerTitles();
        this.importService = plugin.imports();
        this.msg = plugin.messages();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                msg.send(sender, "general.player-only");
                return true;
            }
            if (!player.hasPermission("liutitle.use")) {
                msg.send(sender, "general.no-permission");
                return true;
            }
            plugin.menus().openWarehouse(player, 0);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                msg.sendList(sender, "help.command");
                return true;
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player-only");
                    return true;
                }
                if (!player.hasPermission("liutitle.shop")) {
                    msg.send(sender, "general.no-permission");
                    return true;
                }
                plugin.menus().openShop(player, 0);
                return true;
            }
            case "list" -> {
                if (sender instanceof Player player) {
                    listOwned(player);
                } else {
                    msg.send(sender, "general.player-only");
                }
                return true;
            }
            case "set" -> {
                return equip(sender, args, false);
            }
            case "buff" -> {
                return equip(sender, args, true);
            }
            case "clear" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player-only");
                    return true;
                }
                PlayerTitle show = playerTitleService.equipped(player.getUniqueId(), false);
                PlayerTitle buff = playerTitleService.equipped(player.getUniqueId(), true);
                if (show == null && buff == null) {
                    msg.send(sender, "warehouse.none-equipped");
                    return true;
                }
                if (show != null) {
                    Title title = titleService.get(show.titleId());
                    if (title != null) {
                        playerTitleService.unequip(player, title, false);
                    }
                }
                if (buff != null) {
                    Title title = titleService.get(buff.titleId());
                    if (title != null) {
                        playerTitleService.unequip(player, title, true);
                    }
                }
                return true;
            }
            case "view" -> {
                if (!sender.hasPermission("liutitle.admin.view")) {
                    msg.send(sender, "general.no-permission");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player-only");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle view <玩家>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                plugin.menus().openAdminPlayer(player, target.getUniqueId(),
                        target.getName() == null ? args[1] : target.getName(), 0);
                return true;
            }
            case "admin" -> {
                return admin(sender, args);
            }
            default -> {
                msg.send(sender, "general.unknown", "{input}", sub);
                return true;
            }
        }
    }

    private boolean equip(CommandSender sender, String[] args, boolean buff) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("liutitle.use")) {
            msg.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            msg.send(sender, "general.usage", "{usage}", buff ? "/liutitle buff <称号>" : "/liutitle set <称号>");
            return true;
        }
        Title title = resolve(args[1]);
        if (title == null) {
            msg.send(sender, "title.not-found", "{id}", args[1]);
            return true;
        }
        playerTitleService.equip(player, title, buff);
        return true;
    }

    private void listOwned(Player player) {
        List<PlayerTitle> owned = playerTitleService.owned(player.getUniqueId());
        if (owned.isEmpty()) {
            msg.send(player, "warehouse.empty");
            return;
        }
        msg.send(player, "warehouse.list-header", "{count}", owned.size());
        for (PlayerTitle item : owned) {
            String status = item.use() ? msg.raw("warehouse.status-show")
                    : item.useBuff() ? msg.raw("warehouse.status-buff") : "";
            msg.send(player, "warehouse.list-line",
                    "{name}", item.titleName(),
                    "{expire}", item.expireLabel(),
                    "{status}", status);
        }
    }

    // ==================================================================
    // 管理
    // ==================================================================

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("liutitle.admin")) {
            msg.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            msg.sendList(sender, "help.admin");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player-only");
                    return true;
                }
                plugin.menus().openAdminShop(player, 0);
                return true;
            }
            case "create" -> {
                if (!require(sender, "liutitle.admin.create")) {
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "general.usage", "{usage}",
                            "/liutitle admin create <显示名> [天数] [货币] [价格] [描述]");
                    return true;
                }
                String name = args[2];
                int day = args.length > 3 ? parseInt(args[3], 0) : 0;
                BuyType buyType = args.length > 4 ? BuyType.parse(args[4]) : BuyType.NOT;
                int amount = args.length > 5 ? parseInt(args[5], 0) : 0;
                String description = args.length > 6
                        ? String.join(" ", Arrays.copyOfRange(args, 6, args.length)) : "";
                int id = titleService.create(name, Math.max(0, day), buyType, Math.max(0, amount), description);
                if (id <= 0) {
                    msg.send(sender, "admin.create-fail");
                } else {
                    msg.send(sender, "admin.created", "{id}", id, "{name}", name);
                }
                return true;
            }
            case "delete" -> {
                if (!require(sender, "liutitle.admin.delete")) {
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin delete <ID>");
                    return true;
                }
                int id = parseInt(args[2], -1);
                if (id > 0 && titleService.delete(id)) {
                    playerTitleService.reloadOnline();
                    msg.send(sender, "admin.deleted", "{id}", id);
                } else {
                    msg.send(sender, "title.not-found", "{id}", args[2]);
                }
                return true;
            }
            case "hide", "show" -> {
                if (!require(sender, "liutitle.admin.modify")) {
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin " + action + " <ID>");
                    return true;
                }
                Title title = resolve(args[2]);
                if (title == null) {
                    msg.send(sender, "title.not-found", "{id}", args[2]);
                    return true;
                }
                boolean hidden = "hide".equals(action);
                titleService.setHidden(title.id(), hidden);
                msg.send(sender, hidden ? "admin.hidden" : "admin.shown",
                        "{id}", title.id(), "{name}", title.name());
                return true;
            }
            case "weight" -> {
                if (!require(sender, "liutitle.admin.modify")) {
                    return true;
                }
                if (args.length < 4) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin weight <ID> <权重>");
                    return true;
                }
                Title title = resolve(args[2]);
                if (title == null) {
                    msg.send(sender, "title.not-found", "{id}", args[2]);
                    return true;
                }
                String raw = args[3].trim();
                if (!raw.matches("-?\\d{1,9}")) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin weight <ID> <权重>");
                    return true;
                }
                int weight = parseInt(raw, 0);
                titleService.setWeight(title.id(), weight);
                msg.send(sender, "admin.weight-set", "{id}", title.id(),
                        "{name}", title.name(), "{weight}", weight);
                return true;
            }
            case "give" -> {
                if (!require(sender, "liutitle.admin.give")) {
                    return true;
                }
                if (args.length < 4) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin give <玩家> <ID> [天数]");
                    return true;
                }
                Title title = resolve(args[3]);
                if (title == null) {
                    msg.send(sender, "title.not-found", "{id}", args[3]);
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String name = target.getName() == null ? args[2] : target.getName();
                int days = args.length > 4 ? parseInt(args[4], title.day()) : title.day();
                PlayerTitle granted = playerTitleService.grant(target.getUniqueId(), name, title, days);
                if (granted == null) {
                    msg.send(sender, "admin.give-fail");
                } else {
                    msg.send(sender, "admin.gave", "{player}", name, "{name}", title.name(),
                            "{expire}", granted.expireLabel());
                }
                return true;
            }
            case "take" -> {
                if (!require(sender, "liutitle.admin.give")) {
                    return true;
                }
                if (args.length < 4) {
                    msg.send(sender, "general.usage", "{usage}", "/liutitle admin take <玩家> <ID>");
                    return true;
                }
                Title title = resolve(args[3]);
                if (title == null) {
                    msg.send(sender, "title.not-found", "{id}", args[3]);
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                if (playerTitleService.take(target.getUniqueId(), title.id())) {
                    msg.send(sender, "admin.took", "{player}", args[2], "{name}", title.name());
                } else {
                    msg.send(sender, "warehouse.not-owned");
                }
                return true;
            }
            case "import" -> {
                if (!require(sender, "liutitle.admin.import")) {
                    return true;
                }
                String raw = args.length > 2 ? args[2] : "import";
                File file = new File(raw);
                if (!file.isAbsolute()) {
                    file = new File(plugin.getDataFolder(), raw);
                }
                File target = file;
                msg.send(sender, "admin.import-start", "{path}", target.getAbsolutePath());
                // 注意：强制用 Runnable 重载，避开 LiuLib 1.0.5 async(Supplier) 的递归缺陷
                LiuLib.scheduler().async((Runnable) () -> {
                    ImportService.Result result = importService.importAll(target.toPath());
                    LiuLib.scheduler().global(() -> {
                        titleService.load();
                        playerTitleService.reloadOnline();
                        msg.send(sender, "admin.import-done",
                                "{titles}", result.titles(),
                                "{owned}", result.owned(),
                                "{expired}", result.expired(),
                                "{buffs}", result.buffs(),
                                "{requires}", result.requires());
                    });
                });
                return true;
            }
            case "reload" -> {
                if (!require(sender, "liutitle.admin.reload")) {
                    return true;
                }
                plugin.reload();
                msg.send(sender, "admin.reloaded");
                return true;
            }
            default -> {
                msg.send(sender, "general.unknown", "{input}", action);
                return true;
            }
        }
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("liutitle.admin")) {
            return true;
        }
        msg.send(sender, "general.no-permission");
        return false;
    }

    private Title resolve(String raw) {
        int id = parseInt(raw, -1);
        if (id > 0) {
            Title title = titleService.get(id);
            if (title != null) {
                return title;
            }
        }
        return titleService.byName(raw);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ==================================================================
    // Tab
    // ==================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("shop", "list", "set", "buff", "clear", "help"));
            if (sender.hasPermission("liutitle.admin")) {
                out.addAll(List.of("admin", "view"));
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set", "buff" -> titleService.all().forEach(t -> out.add(String.valueOf(t.id())));
                case "admin" -> out.addAll(List.of("shop", "create", "delete", "hide", "show",
                        "weight", "give", "take", "import", "reload"));
                case "view" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 3 && "admin".equalsIgnoreCase(args[0])) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "delete", "hide", "show", "weight" -> titleService.all().forEach(t -> out.add(String.valueOf(t.id())));
                case "give", "take", "view" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 4 && "admin".equalsIgnoreCase(args[0])) {
            if ("give".equalsIgnoreCase(args[1]) || "take".equalsIgnoreCase(args[1])) {
                titleService.all().forEach(t -> out.add(String.valueOf(t.id())));
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
