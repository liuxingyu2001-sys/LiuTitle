package com.liuxingyu.liutitle.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** 称号定义。 */
public final class Title {

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final LocalDateTime PERMANENT = LocalDateTime.of(2126, 1, 1, 0, 0, 0);

    private int id;
    private String name;
    private String description = "";
    private int day;
    private boolean hidden;
    private TitlePosition position = TitlePosition.ALL;
    private BuyType legacyBuyType = BuyType.NOT;
    private Integer legacyAmount;
    private String itemStack;
    private final List<Require> requires = new ArrayList<>();
    private final List<Buff> buffs = new ArrayList<>();

    public int id() { return id; }
    public void id(int id) { this.id = id; }

    public String name() { return name; }
    public void name(String name) { this.name = name; }

    public String description() { return description; }
    public void description(String description) { this.description = description == null ? "" : description; }

    public int day() { return day; }
    public void day(int day) { this.day = Math.max(0, day); }

    public boolean hidden() { return hidden; }
    public void hidden(boolean hidden) { this.hidden = hidden; }

    public TitlePosition position() { return position; }
    public void position(TitlePosition position) { this.position = position == null ? TitlePosition.ALL : position; }

    public BuyType legacyBuyType() { return legacyBuyType; }
    public void legacyBuyType(BuyType type) { this.legacyBuyType = type == null ? BuyType.NOT : type; }

    public Integer legacyAmount() { return legacyAmount; }
    public void legacyAmount(Integer amount) { this.legacyAmount = amount; }

    public String itemStack() { return itemStack; }
    public void itemStack(String itemStack) { this.itemStack = itemStack; }

    public List<Require> requires() { return requires; }
    public List<Buff> buffs() { return buffs; }

    /** 购买条件：优先 title_require，其次 title_list 的旧字段。 */
    public List<Require> effectiveRequires() {
        if (!requires.isEmpty()) {
            return requires;
        }
        if (legacyBuyType != BuyType.NOT && legacyBuyType != BuyType.ALL) {
            return List.of(new Require(0, id, legacyBuyType, legacyAmount == null ? 0 : legacyAmount, null, null));
        }
        return List.of();
    }

    public List<Buff> buffsOf(String applyType) {
        List<Buff> out = new ArrayList<>();
        for (Buff b : buffs) {
            if (b.applyType().equalsIgnoreCase(applyType)) {
                out.add(b);
            }
        }
        return out;
    }

    public boolean hasBuff(String applyType) {
        return !buffsOf(applyType).isEmpty();
    }

    public String daysLabel() {
        return day <= 0 ? "永久" : day + " 天";
    }
}
