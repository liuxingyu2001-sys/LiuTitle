package com.liuxingyu.liutitle.model;

/** 称号属性。 */
public final class Buff {

    private final int id;
    private final int titleId;
    private final BuffType type;
    private final String content;
    private final String description;
    private final String applyType;

    public Buff(int id, int titleId, BuffType type, String content, String description, String applyType) {
        this.id = id;
        this.titleId = titleId;
        this.type = type;
        this.content = content == null ? "" : content;
        this.description = description == null ? "" : description;
        this.applyType = applyType == null || applyType.isBlank() ? "wear" : applyType;
    }

    public int id() { return id; }
    public int titleId() { return titleId; }
    public BuffType type() { return type; }
    public String content() { return content; }
    public String description() { return description; }
    public String applyType() { return applyType; }

    public boolean wear() { return "wear".equalsIgnoreCase(applyType); }
    public boolean collect() { return "collect".equalsIgnoreCase(applyType); }

    /** 供 GUI 显示的一行说明。 */
    public String display() {
        if (!description.isBlank()) {
            return description;
        }
        if (type == BuffType.POTION_EFFECT) {
            String name = PotionBuff.parseName(content);
            int level = PotionBuff.parseLevel(content);
            return (name == null ? content : name) + (level > 0 ? " " + level + "级" : "");
        }
        if (type == BuffType.ATTRIBUTE) {
            return content;
        }
        return type.id() + ": " + content;
    }
}
