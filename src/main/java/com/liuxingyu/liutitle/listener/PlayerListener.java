package com.liuxingyu.liutitle.listener;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.LiuTitle;
import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.service.BuffService;
import com.liuxingyu.liutitle.service.PlayerTitleService;
import com.liuxingyu.liutitle.service.TitleService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** 玩家生命周期：加载/卸载称号、恢复属性。 */
public final class PlayerListener implements Listener {

    private final LiuTitle plugin;
    private final Settings settings;
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final BuffService buffService;

    public PlayerListener(LiuTitle plugin) {
        this.plugin = plugin;
        this.settings = plugin.settings();
        this.titleService = plugin.titles();
        this.playerTitleService = plugin.playerTitles();
        this.buffService = plugin.buffs();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerTitleService.load(player);
        grantDefault(player);
        long delay = Math.max(0, settings.loginDelay()) * 20L;
        if (delay <= 0) {
            playerTitleService.applyEquippedBuff(player);
        } else {
            LiuLib.scheduler().globalLater(delay, () -> {
                if (player.isOnline()) {
                    playerTitleService.applyEquippedBuff(player);
                }
            });
        }
    }

    private void grantDefault(Player player) {
        if (settings.defaultTitle().isBlank() || playerTitleService.count(player.getUniqueId()) > 0) {
            return;
        }
        Title title = titleService.byName(settings.defaultTitle());
        if (title == null) {
            return;
        }
        playerTitleService.grant(player.getUniqueId(), player.getName(), title, settings.defaultDays());
        if (title.position().supportsShow()) {
            playerTitleService.equip(player, title, false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        buffService.clear(player);
        playerTitleService.unload(player.getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        buffService.clear(player);
        LiuLib.scheduler().globalLater(1L, () -> {
            if (player.isOnline()) {
                playerTitleService.applyEquippedBuff(player);
            }
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        LiuLib.scheduler().globalLater(2L, () -> {
            if (player.isOnline()) {
                playerTitleService.applyEquippedBuff(player);
            }
        });
    }
}
