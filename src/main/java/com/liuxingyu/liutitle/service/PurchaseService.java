package com.liuxingyu.liutitle.service;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.event.TitlePurchaseEvent;
import com.liuxingyu.liutitle.model.BuyType;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Require;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/** 称号购买 / 续期：金币、点券、物品、权限、活动，多条件组合。 */
public final class PurchaseService {

    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final Msg msg;

    public PurchaseService(TitleService titleService, PlayerTitleService playerTitleService, Msg msg) {
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.msg = msg;
    }

    public boolean buy(Player player, Title title) {
        if (title == null) {
            return false;
        }
        PlayerTitle owned = playerTitleService.find(player.getUniqueId(), title.id());
        boolean renew = owned != null && title.day() > 0;
        if (owned != null && title.day() <= 0) {
            msg.send(player, "shop.already-owned", "{name}", titleService.plainName(title));
            return false;
        }
        List<Require> requires = title.effectiveRequires();
        if (!check(player, requires)) {
            return false;
        }
        TitlePurchaseEvent event = new TitlePurchaseEvent(player, title, renew);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        if (!take(player, requires)) {
            return false;
        }
        PlayerTitle granted = playerTitleService.grant(player.getUniqueId(), player.getName(), title, title.day());
        if (granted == null) {
            msg.send(player, "shop.buy-fail-unknown", "{type}", "database");
            return false;
        }
        if (renew) {
            msg.send(player, "shop.renew-success", "{name}",
                    titleService.plainName(title), "{date}", granted.expireLabel());
        } else {
            msg.send(player, "shop.buy-success", "{name}", titleService.plainName(title));
        }
        return true;
    }

    /** 检查是否满足全部条件（不发消息）。 */
    public boolean canBuy(Player player, Title title) {
        PlayerTitle owned = playerTitleService.find(player.getUniqueId(), title.id());
        if (owned != null && title.day() <= 0) {
            return false;
        }
        for (Require require : title.effectiveRequires()) {
            if (!check(player, List.of(require), false)) {
                return false;
            }
        }
        return true;
    }

    public boolean isOwned(Player player, Title title) {
        return playerTitleService.find(player.getUniqueId(), title.id()) != null;
    }

    private boolean check(Player player, List<Require> requires) {
        return check(player, requires, true);
    }

    private boolean check(Player player, List<Require> requires, boolean notify) {
        for (Require require : requires) {
            switch (require.buyType()) {
                case VAULT -> {
                    double amount = require.amount();
                    if (!LiuLib.economy().available() || !LiuLib.economy().has(player, amount)) {
                        if (notify) {
                            msg.send(player, "shop.buy-fail-vault", "{amount}", trim(amount));
                        }
                        return false;
                    }
                }
                case PLAYER_POINTS -> {
                    int points = playerPoints(player.getUniqueId());
                    if (points < require.amount()) {
                        if (notify) {
                            msg.send(player, "shop.buy-fail-playerPoints", "{amount}", require.amount());
                        }
                        return false;
                    }
                }
                case ITEM_STACK -> {
                    ItemStack stack = parseItem(require.buyData());
                    if (stack == null || count(player, stack) < require.amount()) {
                        if (notify) {
                            msg.send(player, "shop.buy-fail-itemStack", "{amount}", require.amount(),
                                    "{item}", stack == null ? "物品" : LiuLib.items().label(stack));
                        }
                        return false;
                    }
                }
                case PERMISSION -> {
                    if (!player.hasPermission(require.permission())) {
                        if (notify) {
                            msg.send(player, "shop.buy-fail-permission", "{perm}", require.permission());
                        }
                        return false;
                    }
                }
                case ACTIVITY -> {
                    if (notify) {
                        msg.send(player, "shop.buy-fail-activity");
                    }
                    return false;
                }
                case NOT -> {
                }
                default -> {
                    if (notify) {
                        msg.send(player, "shop.buy-fail-unknown", "{type}", require.buyType().id());
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean take(Player player, List<Require> requires) {
        for (Require require : requires) {
            switch (require.buyType()) {
                case VAULT -> {
                    if (!LiuLib.economy().withdraw(player, require.amount()).success()) {
                        msg.send(player, "shop.no-money");
                        return false;
                    }
                }
                case PLAYER_POINTS -> {
                    if (!takePoints(player.getUniqueId(), require.amount())) {
                        msg.send(player, "shop.no-money");
                        return false;
                    }
                }
                case ITEM_STACK -> {
                    ItemStack stack = parseItem(require.buyData());
                    if (stack == null || !remove(player, stack, require.amount())) {
                        msg.send(player, "shop.inventory-full");
                        return false;
                    }
                }
                default -> {
                }
            }
        }
        return true;
    }

    public static String trim(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public static ItemStack parseItem(String buyData) {
        if (buyData == null || buyData.isBlank()) {
            return null;
        }
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(buyData.contains("item:") ? buyData : "item: " + buyData);
            ItemStack stack = yaml.getItemStack("item");
            if (stack != null) {
                return stack;
            }
        } catch (Exception ignored) {
            // 继续按物品规格解析
        }
        return LiuLib.items().buildOrNull(buyData.trim());
    }

    private static int count(Player player, ItemStack target) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(target)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static boolean remove(Player player, ItemStack target, int amount) {
        if (count(player, target) < amount) {
            return false;
        }
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.isSimilar(target)) {
                continue;
            }
            int take = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - take);
            left -= take;
            if (item.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
        return left <= 0;
    }

    // ==================================================================
    // PlayerPoints（反射，避免编译依赖）
    // ==================================================================

    private static Object pointsApi() {
        try {
            Class<?> clazz = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object plugin = clazz.getMethod("getInstance").invoke(null);
            if (plugin == null) {
                return null;
            }
            return clazz.getMethod("getAPI").invoke(plugin);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int playerPoints(UUID uuid) {
        Object api = pointsApi();
        if (api == null) {
            return 0;
        }
        try {
            Method look = api.getClass().getMethod("look", UUID.class);
            Object value = look.invoke(api, uuid);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean takePoints(UUID uuid, int amount) {
        Object api = pointsApi();
        if (api == null) {
            return false;
        }
        try {
            Method take = api.getClass().getMethod("take", UUID.class, int.class);
            Object ok = take.invoke(api, uuid, amount);
            return !(ok instanceof Boolean b) || b;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean givePoints(UUID uuid, int amount) {
        Object api = pointsApi();
        if (api == null) {
            return false;
        }
        try {
            Method give = api.getClass().getMethod("give", UUID.class, int.class);
            Object ok = give.invoke(api, uuid, amount);
            return !(ok instanceof Boolean b) || b;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean pointsAvailable() {
        return pointsApi() != null;
    }
}
