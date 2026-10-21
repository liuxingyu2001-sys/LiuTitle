package com.liuxingyu.liutitle.api;

import com.liuxingyu.liutitle.LiuTitle;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 给其他插件调用的称号 API。
 *
 * <p>获取方式：{@code Bukkit.getServicesManager().load(TitleApi.class)}，
 * 或 {@code ((LiuTitle) Bukkit.getPluginManager().getPlugin("LiuTitle")).api()}。</p>
 */
public final class TitleApi {

    private final LiuTitle plugin;

    public TitleApi(LiuTitle plugin) {
        this.plugin = plugin;
    }

    public static TitleApi get() {
        TitleApi api = Bukkit.getServicesManager().load(TitleApi.class);
        if (api != null) {
            return api;
        }
        if (Bukkit.getPluginManager().getPlugin("LiuTitle") instanceof LiuTitle title) {
            return title.api();
        }
        return null;
    }

    // ==================================================================
    // 称号定义
    // ==================================================================

    public int titleCount() {
        return plugin.titles().count();
    }

    public List<Integer> titleIds() {
        List<Integer> ids = new ArrayList<>();
        plugin.titles().all().forEach(title -> ids.add(title.id()));
        return ids;
    }

    public boolean hasTitle(int id) {
        return plugin.titles().get(id) != null;
    }

    public String titleName(int id) {
        Title title = plugin.titles().get(id);
        return title == null ? null : title.name();
    }

    public String titleDescription(int id) {
        Title title = plugin.titles().get(id);
        return title == null ? null : title.description();
    }

    public int titleDays(int id) {
        Title title = plugin.titles().get(id);
        return title == null ? 0 : title.day();
    }

    // ==================================================================
    // 玩家数据
    // ==================================================================

    public int ownedCount(UUID uuid) {
        return plugin.playerTitles().count(uuid);
    }

    public List<Integer> ownedTitleIds(UUID uuid) {
        List<Integer> ids = new ArrayList<>();
        for (PlayerTitle item : plugin.playerTitles().owned(uuid)) {
            ids.add(item.titleId());
        }
        return ids;
    }

    public List<String> ownedTitles(UUID uuid) {
        List<String> names = new ArrayList<>();
        for (PlayerTitle item : plugin.playerTitles().owned(uuid)) {
            names.add(item.titleName());
        }
        return names;
    }

    public boolean owns(UUID uuid, int titleId) {
        return plugin.playerTitles().find(uuid, titleId) != null;
    }

    public String expireAt(UUID uuid, int titleId) {
        PlayerTitle owned = plugin.playerTitles().find(uuid, titleId);
        return owned == null ? null : owned.expireLabel();
    }

    public String equippedTitle(UUID uuid) {
        PlayerTitle owned = plugin.playerTitles().equipped(uuid, false);
        return owned == null ? null : owned.titleName();
    }

    public int equippedTitleId(UUID uuid) {
        PlayerTitle owned = plugin.playerTitles().equipped(uuid, false);
        return owned == null ? -1 : owned.titleId();
    }

    public String equippedBuffTitle(UUID uuid) {
        PlayerTitle owned = plugin.playerTitles().equipped(uuid, true);
        return owned == null ? null : owned.titleName();
    }

    public int equippedBuffTitleId(UUID uuid) {
        PlayerTitle owned = plugin.playerTitles().equipped(uuid, true);
        return owned == null ? -1 : owned.titleId();
    }

    // ==================================================================
    // 操作
    // ==================================================================

    public boolean equip(Player player, int titleId, boolean buff) {
        Title title = plugin.titles().get(titleId);
        return title != null && plugin.playerTitles().equip(player, title, buff);
    }

    public boolean unequip(Player player, int titleId, boolean buff) {
        Title title = plugin.titles().get(titleId);
        return title != null && plugin.playerTitles().unequip(player, title, buff);
    }

    /** days <= 0 表示永久。 */
    public boolean grant(UUID uuid, String playerName, int titleId, int days) {
        Title title = plugin.titles().get(titleId);
        return title != null && plugin.playerTitles().grant(uuid, playerName, title, days) != null;
    }

    public boolean take(UUID uuid, int titleId) {
        return plugin.playerTitles().take(uuid, titleId);
    }
}
