package com.liuxingyu.liutitle.model;

import java.util.Locale;

/** 属性类型（对应 PlayerTitle 的 buff_type 字段）。 */
public enum BuffType {

    POTION_EFFECT("potion_effect"),
    ATTRIBUTE("attribute"),
    ATTRIBUTE_PLUS("attribute_plus"),
    SX_ATTRIBUTE("sx_attribute"),
    ATTRIBUTE_SYSTEM("attribute_system"),
    MYTHIC_LIB("mythiclib"),
    CUSTOM("custom"),
    OTHER("other");

    private final String id;

    BuffType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean nativeSupported() {
        return this == POTION_EFFECT || this == ATTRIBUTE;
    }

    public static BuffType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (BuffType t : values()) {
            if (t.id.equals(v) || t.name().toLowerCase(Locale.ROOT).equals(v)) {
                return t;
            }
        }
        if (v.contains("attribute_plus") || v.contains("attributeplus")) {
            return ATTRIBUTE_PLUS;
        }
        if (v.contains("sx")) {
            return SX_ATTRIBUTE;
        }
        if (v.contains("attributesystem") || v.contains("attribute_system")) {
            return ATTRIBUTE_SYSTEM;
        }
        if (v.contains("mythic")) {
            return MYTHIC_LIB;
        }
        return OTHER;
    }
}
