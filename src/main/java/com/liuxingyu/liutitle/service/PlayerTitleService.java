package com.liuxingyu.liutitle.service;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.dao.PlayerTitleDao;
import com.liuxingyu.liutitle.event.TitleEquipEvent;
import com.liuxingyu.liutitle.event.TitleExpireEvent;
import com.liuxingyu.liutitle.event.TitleGrantEvent;
import com.liuxingyu.liutitle.event.TitleUnequipEvent;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** 玩家称号：拥有、佩戴、发放、过期清理。 */
public final class PlayerTitleService {

    private final PlayerTitleDao dao;
    private final TitleService titleService;
    private final BuffService buffService;
    private final Settings settings;
    private final Msg msg;
    private final Map<UUID, List<PlayerTitle>> cache = new ConcurrentHashMap<>();

    public PlayerTitleService(PlayerTitleDao dao, TitleService titleService, BuffService buffService,
                              Settings settings, Msg msg) {
        this.dao = dao;
        this.titleService = titleService;
        this.buffService = buffService;
        this.settings = settings;
        this.msg = msg;
    }

    public void load(Player player) {
        List<PlayerTitle> owned = new ArrayList<>(dao.byPlayer(player.getUniqueId()));
        List<PlayerTitle> valid = new ArrayList<>(owned.size());
        List<String> expiredNames = new ArrayList<>();
        for (PlayerTitle item : owned) {
            if (item.expired()) {
                dao.deleteById(item.id());
                expiredNames.add(item.titleName());
                continue;
            }
            valid.add(item);
        }
        cache.put(player.getUniqueId(), valid);
        for (String name : expiredNames) {
            msg.send(player, "warehouse.expired-removed", "{name}", name);
        }
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public List<PlayerTitle> owned(UUID uuid) {
        List<PlayerTitle> cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        List<PlayerTitle> loaded = dao.byPlayer(uuid);
        cache.put(uuid, loaded);
        return loaded;
    }

    public PlayerTitle find(UUID uuid, int titleId) {
        for (PlayerTitle item : owned(uuid)) {
            if (item.titleId() == titleId) {
                return item;
            }
        }
        return null;
    }

    public int count(UUID uuid) {
        return owned(uuid).size();
    }

    public List<PlayerTitle> owners(int titleId) {
        return dao.byTitle(titleId);
    }

    public int ownerCount(int titleId) {
        return dao.byTitle(titleId).size();
    }

    public PlayerTitle equipped(UUID uuid, boolean buff) {
        for (PlayerTitle item : owned(uuid)) {
            if (buff ? item.useBuff() : item.use()) {
                return item;
            }
        }
        return null;
    }

    public boolean equip(Player player, Title title, boolean buff) {
        PlayerTitle owned = title == null ? null : find(player.getUniqueId(), title.id());
        if (owned == null) {
            msg.send(player, "warehouse.not-owned");
            return false;
        }
        if (title == null) {
            msg.send(player, "title.not-found", "{id}", owned.titleId());
            return false;
        }
        boolean allowed = buff ? title.position().supportsBuff() : title.position().supportsShow();
        if (!allowed) {
            msg.send(player, "warehouse.position-mismatch");
            return false;
        }
        if (buff && !settings.buffEnabled()) {
            msg.send(player, "warehouse.buff-disabled");
            return false;
        }
        if (settings.toggleCooldown() > 0
                && LiuLib.cooldowns().isCooling("title-switch", player.getUniqueId())) {
            msg.send(player, "warehouse.cooldown", "{seconds}",
                    LiuLib.cooldowns().remainingSeconds("title-switch", player.getUniqueId()));
            return false;
        }
        TitleEquipEvent event = new TitleEquipEvent(player, title, buff);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        String column = buff ? "use_buff" : "use";
        dao.clearUse(player.getUniqueId(), column);
        if (buff) {
            dao.updateUseBuff(owned.id(), true);
        } else {
            dao.updateUse(owned.id(), true);
        }
        for (PlayerTitle item : owned(player.getUniqueId())) {
            if (buff) {
                item.useBuff(item.titleId() == title.id());
            } else {
                item.use(item.titleId() == title.id());
            }
        }
        if (buff) {
            buffService.apply(player, title);
        }
        if (settings.toggleCooldown() > 0) {
            LiuLib.cooldowns().set("title-switch", player.getUniqueId(), settings.toggleCooldown(), TimeUnit.SECONDS);
        }
        msg.send(player, buff ? "warehouse.equipped-buff" : "warehouse.equipped",
                "{name}", titleService.plainName(title));
        return true;
    }

    public boolean unequip(Player player, Title title, boolean buff) {
        PlayerTitle owned = title == null ? null : find(player.getUniqueId(), title.id());
        if (owned == null) {
            msg.send(player, "warehouse.not-owned");
            return false;
        }
        if (buff) {
            dao.updateUseBuff(owned.id(), false);
            owned.useBuff(false);
            buffService.clear(player);
        } else {
            dao.updateUse(owned.id(), false);
            owned.use(false);
        }
        Bukkit.getPluginManager().callEvent(new TitleUnequipEvent(player, title, buff));
        msg.send(player, "warehouse.unequipped", "{name}", titleService.plainName(title));
        return true;
    }

    /** 发放/续期称号。days <= 0 表示永久。 */
    public PlayerTitle grant(UUID uuid, String playerName, Title title, int days) {
        PlayerTitle existing = find(uuid, title.id());
        LocalDateTime expiration;
        if (existing != null) {
            expiration = days <= 0 ? Title.PERMANENT : basePlus(existing.expiration(), days);
            dao.updateExpiration(existing.id(), PlayerTitle.formatTime(expiration));
            existing.expiration(expiration);
            existing.playerName(playerName);
            existing.titleName(title.name());
        } else {
            expiration = days <= 0 ? Title.PERMANENT : LocalDateTime.now().plusDays(days);
            PlayerTitle created = new PlayerTitle(0, playerName, uuid, title.id(), title.name(), expiration, false, false);
            int id = dao.insert(created);
            if (id <= 0) {
                return null;
            }
            created.id(id);
            owned(uuid).add(created);
            existing = created;
        }
        Bukkit.getPluginManager().callEvent(new TitleGrantEvent(uuid, playerName, title, days));
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            msg.send(online, "receive.title", "{name}", titleService.plainName(title));
            if (settings.autoUse() && existing != null && !existing.use() && title.position().supportsShow()) {
                equip(online, title, false);
            }
        }
        return existing;
    }

    private static LocalDateTime basePlus(LocalDateTime base, int days) {
        LocalDateTime start = base.isBefore(LocalDateTime.now()) ? LocalDateTime.now() : base;
        if (start.isAfter(Title.PERMANENT.minusYears(50))) {
            return Title.PERMANENT;
        }
        return start.plusDays(days);
    }

    public boolean take(UUID uuid, int titleId) {
        PlayerTitle owned = find(uuid, titleId);
        if (owned == null) {
            return false;
        }
        dao.delete(uuid, titleId);
        owned(uuid).remove(owned);
        if (owned.use() || owned.useBuff()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                buffService.clear(online);
            }
        }
        return true;
    }

    /** 玩家上线后应用已佩戴的属性称号。 */
    public void applyEquippedBuff(Player player) {
        PlayerTitle owned = equipped(player.getUniqueId(), true);
        if (owned == null) {
            return;
        }
        Title title = titleService.get(owned.titleId());
        if (title != null) {
            buffService.apply(player, title);
        }
    }

    /** 定期清理过期称号，返回清理数量。 */
    public int cleanupExpired() {
        int removed = 0;
        for (Map.Entry<UUID, List<PlayerTitle>> entry : cache.entrySet()) {
            List<PlayerTitle> list = entry.getValue();
            List<PlayerTitle> expired = new ArrayList<>();
            for (PlayerTitle item : list) {
                if (item.expired()) {
                    expired.add(item);
                }
            }
            if (expired.isEmpty()) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            for (PlayerTitle item : expired) {
                dao.deleteById(item.id());
                list.remove(item);
                removed++;
                Title title = titleService.get(item.titleId());
                if (player != null) {
                    if (item.useBuff()) {
                        buffService.clear(player);
                    }
                    msg.send(player, "warehouse.expired-removed", "{name}",
                            title == null ? item.titleName() : titleService.plainName(title));
                }
                Bukkit.getPluginManager().callEvent(new TitleExpireEvent(player, item, title));
            }
        }
        return removed;
    }

    public int cachedPlayers() {
        return cache.size();
    }

    public void reloadOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            load(player);
            applyEquippedBuff(player);
        }
    }
}
