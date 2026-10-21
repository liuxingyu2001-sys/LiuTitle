package com.liuxingyu.liutitle;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liulib.scheduler.TaskHandle;
import com.liuxingyu.liutitle.api.TitleApi;
import com.liuxingyu.liutitle.command.TitleCommand;
import com.liuxingyu.liutitle.config.GuiConfig;
import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.dao.PlayerTitleDao;
import com.liuxingyu.liutitle.dao.TitleDao;
import com.liuxingyu.liutitle.gui.MenuManager;
import com.liuxingyu.liutitle.listener.PlayerListener;
import com.liuxingyu.liutitle.papi.TitleExpansion;
import com.liuxingyu.liutitle.service.BuffService;
import com.liuxingyu.liutitle.service.ImportService;
import com.liuxingyu.liutitle.service.PlayerTitleService;
import com.liuxingyu.liutitle.service.PurchaseService;
import com.liuxingyu.liutitle.service.TitleService;
import com.liuxingyu.liutitle.storage.Schema;
import com.liuxingyu.liutitle.storage.Sql;
import com.liuxingyu.liutitle.util.Msg;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** LiuTitle 主类：称号、商城、仓库、属性、PlaceholderAPI、API。 */
public final class LiuTitle extends JavaPlugin {

    private Settings settings;
    private Msg messages;
    private GuiConfig gui;
    private Sql sql;
    private TitleDao titleDao;
    private PlayerTitleDao playerTitleDao;
    private TitleService titles;
    private BuffService buffs;
    private PlayerTitleService playerTitles;
    private PurchaseService purchases;
    private ImportService imports;
    private MenuManager menus;
    private TitleApi api;
    private TaskHandle cleanupTask;
    private final List<PlaceholderExpansion> expansions = new ArrayList<>();

    @Override
    public void onEnable() {
        settings = Settings.load(this);
        messages = new Msg(this);
        gui = GuiConfig.load(this);
        sql = Sql.create(this, getLogger());
        if (sql == null || !sql.ready()) {
            getLogger().severe("无法初始化任何存储后端，插件停用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Schema.apply(sql);
        titleDao = new TitleDao(sql);
        playerTitleDao = new PlayerTitleDao(sql);
        titles = new TitleService(getLogger(), titleDao, settings, messages);
        buffs = new BuffService(settings);
        playerTitles = new PlayerTitleService(playerTitleDao, titles, buffs, settings, messages);
        purchases = new PurchaseService(titles, playerTitles, messages);
        imports = new ImportService(sql, getLogger());
        menus = new MenuManager(this);
        titles.load();

        TitleCommand command = new TitleCommand(this);
        if (getCommand("liutitle") != null) {
            getCommand("liutitle").setExecutor(command);
            getCommand("liutitle").setTabCompleter(command);
        }
        Bukkit.getPluginManager().registerEvents(menus, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        cleanupTask = LiuLib.scheduler().globalTimer(1200L, 1200L, () -> {
            try {
                playerTitles.cleanupExpired();
            } catch (Throwable t) {
                getLogger().warning("清理过期称号失败：" + t.getMessage());
            }
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerTitles.load(player);
            playerTitles.applyEquippedBuff(player);
        }
        registerPlaceholders();
        api = new TitleApi(this);
        Bukkit.getServicesManager().register(TitleApi.class, api, this, ServicePriority.Normal);
        getLogger().info("LiuTitle 已启用：称号 " + titles.count() + " 个，玩家称号 "
                + playerTitleDao.countAll() + " 条，存储 " + sql.backend());
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        if (buffs != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                buffs.clear(player);
            }
        }
        for (PlaceholderExpansion expansion : expansions) {
            try {
                expansion.unregister();
            } catch (RuntimeException ignored) {
                // 已卸载
            }
        }
        expansions.clear();
        Bukkit.getServicesManager().unregisterAll(this);
        if (sql != null) {
            sql.close();
            sql = null;
        }
    }

    /** 重载配置并重建服务（命令与监听器会重新注册）。 */
    public void reload() {
        settings = Settings.load(this);
        messages.reload();
        gui = GuiConfig.load(this);
        buffs = new BuffService(settings);
        titles = new TitleService(getLogger(), titleDao, settings, messages);
        playerTitles = new PlayerTitleService(playerTitleDao, titles, buffs, settings, messages);
        purchases = new PurchaseService(titles, playerTitles, messages);
        imports = new ImportService(sql, getLogger());
        menus = new MenuManager(this);
        titles.load();
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(menus, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        TitleCommand command = new TitleCommand(this);
        if (getCommand("liutitle") != null) {
            getCommand("liutitle").setExecutor(command);
            getCommand("liutitle").setTabCompleter(command);
        }
        playerTitles.reloadOnline();
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        List<String> identifiers = new ArrayList<>();
        identifiers.add("liutitle");
        for (String alias : settings.placeholderAliases()) {
            if (!identifiers.contains(alias)) {
                identifiers.add(alias);
            }
        }
        for (String identifier : identifiers) {
            try {
                TitleExpansion expansion = new TitleExpansion(this, identifier);
                if (expansion.register()) {
                    expansions.add(expansion);
                    getLogger().info("已注册 PlaceholderAPI 变量：%" + identifier + "_...%");
                } else {
                    getLogger().warning("PlaceholderAPI 标识已被占用，跳过：" + identifier);
                }
            } catch (Throwable t) {
                getLogger().warning("注册 PlaceholderAPI 标识 " + identifier + " 失败：" + t.getMessage());
            }
        }
    }

    public Settings settings() {
        return settings;
    }

    public Msg messages() {
        return messages;
    }

    public GuiConfig gui() {
        return gui;
    }

    public Sql sql() {
        return sql;
    }

    public TitleService titles() {
        return titles;
    }

    public BuffService buffs() {
        return buffs;
    }

    public PlayerTitleService playerTitles() {
        return playerTitles;
    }

    public PurchaseService purchases() {
        return purchases;
    }

    public ImportService imports() {
        return imports;
    }

    public MenuManager menus() {
        return menus;
    }

    public TitleApi api() {
        return api;
    }
}
