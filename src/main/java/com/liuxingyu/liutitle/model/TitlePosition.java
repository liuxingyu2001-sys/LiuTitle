package com.liuxingyu.liutitle.model;

import java.util.Locale;

/** 称号佩戴位置（对应 PlayerTitle 的 position 字段）。 */
public enum TitlePosition {

    ALL(0, true, true),
    FRONT(1, true, false),
    MIDDLE(2, false, true),
    BACK(3, false, false),
    FRONT_MIDDLE(4, true, true),
    FRONT_BACK(5, true, false),
    MIDDLE_BACK(6, false, true);

    private final int id;
    private final boolean show;
    private final boolean buff;

    TitlePosition(int id, boolean show, boolean buff) {
        this.id = id;
        this.show = show;
        this.buff = buff;
    }

    public int id() {
        return id;
    }

    public boolean supportsShow() {
        return show;
    }

    public boolean supportsBuff() {
        return buff;
    }

    public static TitlePosition byId(int id) {
        for (TitlePosition p : values()) {
            if (p.id == id) {
                return p;
            }
        }
        return ALL;
    }

    public static TitlePosition byName(String name) {
        if (name == null) {
            return ALL;
        }
        String n = name.toUpperCase(Locale.ROOT).replace('-', '_');
        for (TitlePosition p : values()) {
            if (p.name().equals(n)) {
                return p;
            }
        }
        return ALL;
    }

    public String label() {
        return name();
    }
}
