package com.liuxingyu.liutitle.model;

import java.util.Locale;

/** 购买/获取方式，字符串与 PlayerTitle 数据库保持一致。 */
public enum BuyType {

    NOT("not"),
    VAULT("vault"),
    PLAYER_POINTS("playerPoints"),
    COIN("coin"),
    ITEM_STACK("itemStack"),
    PERMISSION("permission"),
    ACTIVITY("activity"),
    PLAYER_CURRENCY("playerCurrency"),
    ALL("all"),
    OTHER("other");

    private final String id;

    BuyType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static BuyType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NOT;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        for (BuyType t : values()) {
            if (t.id.toLowerCase(Locale.ROOT).equals(v) || t.name().toLowerCase(Locale.ROOT).equals(v)) {
                return t;
            }
        }
        return OTHER;
    }

    public static BuyType fromOrdinal(String raw) {
        try {
            return values()[Integer.parseInt(raw.trim())];
        } catch (RuntimeException e) {
            return parse(raw);
        }
    }
}
