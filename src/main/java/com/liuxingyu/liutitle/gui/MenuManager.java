package com.liuxingyu.liutitle.gui;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.LiuTitle;
import com.liuxingyu.liutitle.config.GuiConfig;
import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Require;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.service.PlayerTitleService;
import com.liuxingyu.liutitle.service.PurchaseService;
import com.liuxingyu.liutitle.service.TitleService;
import com.liuxingyu.liutitle.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 称号 GUI：商城、仓库、管理。 */
public final class MenuManager implements Listener {

    private final LiuTitle plugin;
    private final GuiConfig gui;
    private final Settings settings;
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final PurchaseService purchaseService;
    private final Msg msg;

    private final Map<UUID, Integer> confirmTitle = new HashMap<>();
    private final Map<UUID, Long> confirmAt = new HashMap<>();

    public MenuManager(LiuTitle plugin) {
        this.plugin = plugin;
        this.gui = plugin.gui();
        this.settings = plugin.settings();
        this.titleService = plugin.titles();
        this.playerTitleService = plugin.playerTitles();
        this.purchaseService = plugin.purchases();
        this.msg = plugin.messages();
    }

    // ==================================================================
    // 打开菜单
    // ==================================================================

    public void openShop(Player player, int page) {
        GuiConfig.Menu menu = gui.menu("shop");
        if (menu == null) {
            return;
        }
        List<Title> titles = titleService.shop(settings.shopShowHidden());
        int pages = pages(menu, titles.size());
        int current = clamp(page, pages);
        MenuHolder holder = new MenuHolder(MenuHolder.Type.SHOP, current);
        Inventory inventory = create(holder, menu, menu.title());
        List<Title> pageItems = slice(titles, menu, current);
        for (int i = 0; i < pageItems.size() && i < menu.slots().size(); i++) {
            Title title = pageItems.get(i);
            int slot = menu.slots().get(i);
            inventory.setItem(slot, shopIcon(menu, player, title));
            holder.map(slot, title);
        }
        fillCommon(player, holder, inventory, menu, current, pages);
        open(player, inventory);
    }

    public void openWarehouse(Player player, int page) {
        GuiConfig.Menu menu = gui.menu("warehouse");
        if (menu == null) {
            return;
        }
        List<PlayerTitle> owned = playerTitleService.owned(player.getUniqueId());
        if (owned.isEmpty()) {
            msg.send(player, "warehouse.empty");
        }
        int pages = pages(menu, owned.size());
        int current = clamp(page, pages);
        MenuHolder holder = new MenuHolder(MenuHolder.Type.WAREHOUSE, current);
        Inventory inventory = create(holder, menu, menu.title());
        List<PlayerTitle> pageItems = slice(owned, menu, current);
        for (int i = 0; i < pageItems.size() && i < menu.slots().size(); i++) {
            PlayerTitle ownedTitle = pageItems.get(i);
            int slot = menu.slots().get(i);
            inventory.setItem(slot, warehouseIcon(menu, player, ownedTitle));
            holder.map(slot, ownedTitle);
        }
        fillCommon(player, holder, inventory, menu, current, pages);
        open(player, inventory);
    }

    public void openAdminShop(Player player, int page) {
        GuiConfig.Menu menu = gui.menu("adminShop");
        if (menu == null) {
            return;
        }
        List<Title> titles = titleService.all();
        titles.sort((a, b) -> Integer.compare(a.id(), b.id()));
        int pages = pages(menu, titles.size());
        int current = clamp(page, pages);
        MenuHolder holder = new MenuHolder(MenuHolder.Type.ADMIN_SHOP, current);
        Inventory inventory = create(holder, menu, menu.title());
        List<Title> pageItems = slice(titles, menu, current);
        for (int i = 0; i < pageItems.size() && i < menu.slots().size(); i++) {
            Title title = pageItems.get(i);
            int slot = menu.slots().get(i);
            inventory.setItem(slot, adminShopIcon(menu, title));
            holder.map(slot, title);
        }
        fillCommon(player, holder, inventory, menu, current, pages);
        open(player, inventory);
    }

    public void openAdminOwners(Player player, int titleId, int page) {
        GuiConfig.Menu menu = gui.menu("adminOwners");
        Title title = titleService.get(titleId);
        if (menu == null || title == null) {
            return;
        }
        List<PlayerTitle> owners = playerTitleService.owners(titleId);
        int pages = pages(menu, owners.size());
        int current = clamp(page, pages);
        MenuHolder holder = new MenuHolder(MenuHolder.Type.ADMIN_OWNERS, current, titleId, null, "");
        String titleText = Msg.fillDollar(menu.title(), Map.of("titleName", title.name()));
        Inventory inventory = create(holder, menu, titleText);
        List<PlayerTitle> pageItems = slice(owners, menu, current);
        for (int i = 0; i < pageItems.size() && i < menu.slots().size(); i++) {
            PlayerTitle owned = pageItems.get(i);
            int slot = menu.slots().get(i);
            inventory.setItem(slot, ownerIcon(menu, owned));
            holder.map(slot, owned);
        }
        fillCommon(player, holder, inventory, menu, current, pages);
        open(player, inventory);
    }

    public void openAdminPlayer(Player player, UUID target, String targetName, int page) {
        GuiConfig.Menu menu = gui.menu("adminPlayer");
        if (menu == null) {
            return;
        }
        List<PlayerTitle> owned = playerTitleService.owned(target);
        int pages = pages(menu, owned.size());
        int current = clamp(page, pages);
        MenuHolder holder = new MenuHolder(MenuHolder.Type.ADMIN_PLAYER, current, 0, target, targetName);
        String titleText = Msg.fillDollar(menu.title(), Map.of("player", targetName));
        Inventory inventory = create(holder, menu, titleText);
        List<PlayerTitle> pageItems = slice(owned, menu, current);
        for (int i = 0; i < pageItems.size() && i < menu.slots().size(); i++) {
            PlayerTitle ownedTitle = pageItems.get(i);
            int slot = menu.slots().get(i);
            inventory.setItem(slot, warehouseIcon(menu, null, ownedTitle));
            holder.map(slot, ownedTitle);
        }
        fillCommon(player, holder, inventory, menu, current, pages);
        open(player, inventory);
    }

    // ==================================================================
    // 图标
    // ==================================================================

    private ItemStack shopIcon(GuiConfig.Menu menu, Player player, Title title) {
        Map<String, String> ph = new HashMap<>();
        ph.put("titleName", title.name());
        ph.put("id", String.valueOf(title.id()));
        ph.put("day", title.daysLabel());
        ph.put("description", title.description());
        ph.put("buff", titleService.buffLabel(title));
        ph.put("price", titleService.priceLabel(title));
        ph.put("buyType", title.legacyBuyType().id());
        ph.put("position", titleService.positionLabel(title));

        PlayerTitle owned = playerTitleService.find(player.getUniqueId(), title.id());
        String button;
        if (owned != null) {
            button = title.day() > 0 ? menu.buttons().getOrDefault("renew", "") : menu.buttons().getOrDefault("owned", "");
        } else if (hasActivity(title)) {
            button = menu.buttons().getOrDefault("activity", "");
        } else if (purchaseService.canBuy(player, title)) {
            button = menu.buttons().getOrDefault("buy", "");
        } else {
            button = menu.buttons().getOrDefault("unable", "");
        }
        ph.put("buyButton", button);
        return build(menu.item(), menu.item().item(), ph);
    }

    private ItemStack warehouseIcon(GuiConfig.Menu menu, Player viewer, PlayerTitle owned) {
        Title title = titleService.get(owned.titleId());
        Map<String, String> ph = new HashMap<>();
        ph.put("titleName", owned.titleName());
        ph.put("titleId", String.valueOf(owned.titleId()));
        ph.put("id", String.valueOf(owned.titleId()));
        ph.put("expire", owned.expireLabel());
        ph.put("position", title == null ? "-" : titleService.positionLabel(title));
        ph.put("description", title == null ? "" : title.description());
        String status = owned.use() ? menu.buttons().getOrDefault("equipped", "已佩戴")
                : owned.useBuff() ? menu.buttons().getOrDefault("equipped", "已佩戴") + "(属性)"
                : menu.buttons().getOrDefault("unequipped", "未佩戴");
        ph.put("status", status);
        return build(menu.item(), menu.item().item(), ph);
    }

    private ItemStack adminShopIcon(GuiConfig.Menu menu, Title title) {
        Map<String, String> ph = new HashMap<>();
        ph.put("id", String.valueOf(title.id()));
        ph.put("titleName", title.name());
        ph.put("hidden", title.hidden() ? menu.buttons().getOrDefault("hidden", "隐藏") : menu.buttons().getOrDefault("visible", "展示"));
        ph.put("day", title.daysLabel());
        ph.put("price", titleService.priceLabel(title));
        ph.put("description", title.description());
        ph.put("buyers", String.valueOf(playerTitleService.owners(title.id()).size()));
        return build(menu.item(), menu.item().item(), ph);
    }

    private ItemStack ownerIcon(GuiConfig.Menu menu, PlayerTitle owned) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", owned.playerName());
        ph.put("expire", owned.expireLabel());
        ph.put("status", owned.use() ? menu.buttons().getOrDefault("equipped", "佩戴中")
                : menu.buttons().getOrDefault("unequipped", "未佩戴"));
        return build(menu.item(), menu.item().item(), ph);
    }

    private ItemStack build(GuiConfig.Icon def, String spec, Map<String, String> ph) {
        ItemStack base = null;
        if (spec != null && !spec.isBlank()) {
            base = LiuLib.items().buildOrNull(spec);
        }
        if (base == null) {
            base = new ItemStack(Material.NAME_TAG);
        }
        ItemMeta meta = base.getItemMeta();
        if (meta == null) {
            return base;
        }
        String name = Msg.fillDollar(def.name() == null ? "" : def.name(), ph);
        if (name.isBlank()) {
            name = ph.getOrDefault("titleName", "");
        }
        if (!name.isBlank()) {
            meta.displayName(LiuLib.text().parse(name));
        }
        List<String> lore = renderLore(def.lore(), ph);
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(LiuLib.text().parse(line));
            }
            meta.lore(lines);
        }
        if (def.enchant()) {
            meta.setEnchantmentGlintOverride(true);
        }
        base.setItemMeta(meta);
        return base;
    }

    private List<String> renderLore(List<String> template, Map<String, String> ph) {
        List<String> out = new ArrayList<>();
        if (template == null) {
            return out;
        }
        for (String raw : template) {
            String filled = Msg.fillDollar(raw, ph);
            for (String line : filled.split("\n", -1)) {
                out.add(line);
            }
        }
        return out;
    }

    // ==================================================================
    // 通用填充
    // ==================================================================

    private void fillCommon(Player player, MenuHolder holder, Inventory inventory, GuiConfig.Menu menu,
                            int page, int pages) {
        if (menu.info() >= 0 && !menu.infoItem().item().isBlank()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("count", String.valueOf(playerTitleService.count(player.getUniqueId())));
            ph.put("shopCount", String.valueOf(titleService.shop(false).size()));
            ph.put("page", String.valueOf(page + 1));
            ph.put("pages", String.valueOf(pages));
            ph.put("player", player.getName());
            inventory.setItem(menu.info(), build(menu.infoItem(), menu.infoItem().item(), ph));
        }
        if (!menu.closeItem().item().isBlank()) {
            inventory.setItem(menu.close(), build(menu.closeItem(), menu.closeItem().item(), Map.of()));
        }
        if (page > 0 && !menu.prevItem().item().isBlank()) {
            inventory.setItem(menu.prev(), build(menu.prevItem(), menu.prevItem().item(),
                    Map.of("page", String.valueOf(page))));
        }
        if (page + 1 < pages && !menu.nextItem().item().isBlank()) {
            inventory.setItem(menu.next(), build(menu.nextItem(), menu.nextItem().item(),
                    Map.of("page", String.valueOf(page + 2))));
        }
    }

    private Inventory create(MenuHolder holder, GuiConfig.Menu menu, String title) {
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), LiuLib.text().parse(title));
        holder.attach(inventory);
        return inventory;
    }

    private void open(Player player, Inventory inventory) {
        player.openInventory(inventory);
    }

    private static int pages(GuiConfig.Menu menu, int size) {
        int per = Math.max(1, menu.slots().size());
        return Math.max(1, (size + per - 1) / per);
    }

    private static int clamp(int page, int pages) {
        return Math.max(0, Math.min(page, pages - 1));
    }

    private static <T> List<T> slice(List<T> list, GuiConfig.Menu menu, int page) {
        int per = Math.max(1, menu.slots().size());
        int from = page * per;
        if (from >= list.size()) {
            return List.of();
        }
        return list.subList(from, Math.min(list.size(), from + per));
    }

    private static boolean hasActivity(Title title) {
        for (Require require : title.effectiveRequires()) {
            if (require.buyType() == com.liuxingyu.liutitle.model.BuyType.ACTIVITY) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // 点击
    // ==================================================================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        Object value = holder.at(slot);
        if (value == null) {
            handleNavigation(player, holder, slot);
            return;
        }
        switch (holder.type()) {
            case SHOP -> {
                if (value instanceof Title title) {
                    purchaseService.buy(player, title);
                    openShop(player, holder.page());
                }
            }
            case WAREHOUSE -> {
                if (value instanceof PlayerTitle owned) {
                    Title title = titleService.get(owned.titleId());
                    if (title == null) {
                        return;
                    }
                    if (event.isShiftClick() && event.isRightClick()) {
                        purchaseService.buy(player, title);
                    } else if (event.isRightClick()) {
                        playerTitleService.equip(player, title, true);
                    } else {
                        playerTitleService.equip(player, title, false);
                    }
                    openWarehouse(player, holder.page());
                }
            }
            case ADMIN_SHOP -> {
                if (value instanceof Title title) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        confirmDelete(player, title);
                    } else if (event.isRightClick()) {
                        openAdminOwners(player, title.id(), 0);
                    } else {
                        titleService.setHidden(title.id(), !title.hidden());
                        openAdminShop(player, holder.page());
                    }
                }
            }
            case ADMIN_OWNERS -> {
                if (value instanceof PlayerTitle owned) {
                    UUID target = owned.uuid();
                    playerTitleService.take(target, owned.titleId());
                    msg.send(player, "admin.removed-player-title",
                            "{player}", owned.playerName(),
                            "{name}", owned.titleName());
                    openAdminOwners(player, holder.titleId(), holder.page());
                }
            }
            case ADMIN_PLAYER -> {
                if (value instanceof PlayerTitle owned) {
                    playerTitleService.take(holder.target(), owned.titleId());
                    msg.send(player, "admin.removed-player-title",
                            "{player}", holder.targetName(),
                            "{name}", owned.titleName());
                    openAdminPlayer(player, holder.target(), holder.targetName(), holder.page());
                }
            }
        }
    }

    private void handleNavigation(Player player, MenuHolder holder, int slot) {
        GuiConfig.Menu menu = gui.menu(idOf(holder.type()));
        if (menu == null) {
            return;
        }
        if (slot == menu.close()) {
            player.closeInventory();
            return;
        }
        if (slot == menu.prev()) {
            reopen(player, holder, holder.page() - 1);
            return;
        }
        if (slot == menu.next()) {
            reopen(player, holder, holder.page() + 1);
        }
    }

    private void reopen(Player player, MenuHolder holder, int page) {
        switch (holder.type()) {
            case SHOP -> openShop(player, page);
            case WAREHOUSE -> openWarehouse(player, page);
            case ADMIN_SHOP -> openAdminShop(player, page);
            case ADMIN_OWNERS -> openAdminOwners(player, holder.titleId(), page);
            case ADMIN_PLAYER -> openAdminPlayer(player, holder.target(), holder.targetName(), page);
        }
    }

    private void confirmDelete(Player player, Title title) {
        long now = System.currentTimeMillis();
        Long last = confirmAt.get(player.getUniqueId());
        Integer id = confirmTitle.get(player.getUniqueId());
        if (last != null && id != null && id == title.id() && now - last < 10_000L) {
            titleService.delete(title.id());
            confirmAt.remove(player.getUniqueId());
            confirmTitle.remove(player.getUniqueId());
            openAdminShop(player, 0);
            return;
        }
        confirmAt.put(player.getUniqueId(), now);
        confirmTitle.put(player.getUniqueId(), title.id());
        msg.send(player, "admin.confirm-delete", "{id}", title.id(), "{name}", title.name());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static String idOf(MenuHolder.Type type) {
        return switch (type) {
            case SHOP -> "shop";
            case WAREHOUSE -> "warehouse";
            case ADMIN_SHOP -> "adminShop";
            case ADMIN_OWNERS -> "adminOwners";
            case ADMIN_PLAYER -> "adminPlayer";
        };
    }
}
