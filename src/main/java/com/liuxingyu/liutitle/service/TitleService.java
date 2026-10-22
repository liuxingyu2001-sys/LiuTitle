package com.liuxingyu.liutitle.service;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.dao.TitleDao;
import com.liuxingyu.liutitle.model.BuyType;
import com.liuxingyu.liutitle.model.Require;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.model.TitlePosition;
import com.liuxingyu.liutitle.util.Msg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** 称号定义缓存与管理。 */
public final class TitleService {

    private final Logger logger;
    private final TitleDao dao;
    private final Settings settings;
    private final Msg msg;
    private volatile Map<Integer, Title> cache = Map.of();
    private volatile Map<Integer, Integer> weights = Map.of();

    public TitleService(Logger logger, TitleDao dao, Settings settings, Msg msg) {
        this.logger = logger;
        this.dao = dao;
        this.settings = settings;
        this.msg = msg;
    }

    public void load() {
        Map<Integer, Title> loaded = new LinkedHashMap<>(dao.loadAll());
        this.cache = loaded;
        this.weights = new HashMap<>(dao.loadWeights());
        logger.info("已加载称号 " + loaded.size() + " 个");
    }

    public int count() {
        return cache.size();
    }

    public Title get(int id) {
        return cache.get(id);
    }

    public List<Title> all() {
        return new ArrayList<>(cache.values());
    }

    /** 按显示权重降序排序的全部称号（管理商城用）。 */
    public List<Title> allSorted() {
        List<Title> list = new ArrayList<>(cache.values());
        list.sort(comparator());
        return list;
    }

    /** 显示权重：越大越靠前，未设置为 0。 */
    public int weight(int titleId) {
        Integer value = weights.get(titleId);
        return value == null ? 0 : value;
    }

    public boolean setWeight(int titleId, int weight) {
        if (get(titleId) == null) {
            return false;
        }
        dao.setWeight(titleId, weight);
        Map<Integer, Integer> next = new HashMap<>(weights);
        next.put(titleId, weight);
        this.weights = next;
        return true;
    }

    public Title byName(String raw) {
        if (raw == null) {
            return null;
        }
        String needle = LiuLib.text().plain(LiuLib.text().parse(raw)).trim();
        for (Title title : cache.values()) {
            String plain = LiuLib.text().plain(LiuLib.text().parse(title.name())).trim();
            if (plain.equalsIgnoreCase(needle) || title.name().equalsIgnoreCase(raw)) {
                return title;
            }
        }
        return null;
    }

    public List<Title> shop(boolean includeHidden) {
        List<Title> list = new ArrayList<>();
        for (Title title : cache.values()) {
            if (includeHidden || !title.hidden()) {
                list.add(title);
            }
        }
        list.sort(comparator());
        return list;
    }

    private Comparator<Title> comparator() {
        Comparator<Title> secondary = switch (settings.shopSort()) {
            case "name" -> Comparator.comparing(Title::name, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparingDouble(this::priceValue).thenComparingInt(Title::id);
            default -> Comparator.comparingInt(Title::id);
        };
        // 权重优先：权重大的排前面；权重相同再按 shop.sort 的规则排。
        return Comparator.comparingInt((Title title) -> weight(title.id()))
                .reversed()
                .thenComparing(secondary);
    }

    private double priceValue(Title title) {
        double value = 0;
        for (Require require : title.effectiveRequires()) {
            if (require.buyType() == BuyType.VAULT || require.buyType() == BuyType.PLAYER_POINTS) {
                value += require.amount();
            }
        }
        return value;
    }

    public int create(String name, int day, BuyType buyType, int amount, String description) {
        Title title = new Title();
        title.name(name);
        title.day(day);
        title.description(description == null ? "" : description);
        title.position(TitlePosition.ALL);
        title.legacyBuyType(buyType);
        title.legacyAmount(amount);
        int id = dao.insert(title);
        if (id <= 0) {
            return -1;
        }
        if (buyType != BuyType.NOT && buyType != BuyType.ALL) {
            dao.insertRequire(new Require(0, id, buyType, amount, null,
                    com.liuxingyu.liutitle.model.PlayerTitle.formatTime(java.time.LocalDateTime.now())));
        }
        load();
        return id;
    }

    public boolean delete(int id) {
        if (get(id) == null) {
            return false;
        }
        dao.delete(id);
        load();
        return true;
    }

    public boolean setHidden(int id, boolean hidden) {
        Title title = get(id);
        if (title == null) {
            return false;
        }
        dao.setHidden(id, hidden);
        title.hidden(hidden);
        return true;
    }

    public boolean rename(int id, String name) {
        Title title = get(id);
        if (title == null) {
            return false;
        }
        dao.updateName(id, name);
        title.name(name);
        return true;
    }

    public boolean setDescription(int id, String description) {
        Title title = get(id);
        if (title == null) {
            return false;
        }
        dao.updateDescription(id, description);
        title.description(description);
        return true;
    }

    public String priceLabel(Title title) {
        List<Require> requires = title.effectiveRequires();
        if (requires.isEmpty()) {
            return msg.raw("shop.price-free");
        }
        String pointsName = "点券";
        String currencyName = LiuLib.economy().available()
                ? LiuLib.economy().currencyNamePlural() : "金币";
        List<String> parts = new ArrayList<>();
        for (Require require : requires) {
            parts.add(require.label(currencyName, pointsName));
        }
        return String.join("\n", parts);
    }

    public String buffLabel(Title title) {
        if (title.buffs().isEmpty()) {
            return "无";
        }
        List<String> parts = new ArrayList<>();
        for (com.liuxingyu.liutitle.model.Buff buff : title.buffs()) {
            if (buff.type().nativeSupported()) {
                parts.add(buff.display());
            }
        }
        return parts.isEmpty() ? "无" : String.join("\n", parts);
    }

    public String positionLabel(Title title) {
        TitlePosition position = title.position();
        List<String> parts = new ArrayList<>();
        if (position.supportsShow()) {
            parts.add("展示");
        }
        if (position.supportsBuff()) {
            parts.add("属性");
        }
        return parts.isEmpty() ? "无（仅粒子，已移除）" : String.join("+", parts);
    }

    public String plainName(Title title) {
        return LiuLib.text().plain(LiuLib.text().parse(title.name()));
    }

    public String lowerName(Title title) {
        return plainName(title).toLowerCase(Locale.ROOT);
    }
}
