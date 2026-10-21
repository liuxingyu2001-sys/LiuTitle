package com.liuxingyu.liutitle.model;

/** 称号购买条件（title_require 一行）。 */
public final class Require {

    private final int id;
    private final int titleId;
    private final BuyType buyType;
    private final int amount;
    private final String buyData;
    private final String createTime;

    public Require(int id, int titleId, BuyType buyType, int amount, String buyData, String createTime) {
        this.id = id;
        this.titleId = titleId;
        this.buyType = buyType == null ? BuyType.NOT : buyType;
        this.amount = amount;
        this.buyData = buyData == null ? "" : buyData;
        this.createTime = createTime == null ? "" : createTime;
    }

    public int id() { return id; }
    public int titleId() { return titleId; }
    public BuyType buyType() { return buyType; }
    public int amount() { return amount; }
    public String buyData() { return buyData; }
    public String createTime() { return createTime; }

    /** GUI 里显示的一行价格说明。 */
    public String label(String currencyName, String pointsName) {
        return switch (buyType) {
            case VAULT -> amount + " " + currencyName;
            case PLAYER_POINTS -> amount + " " + pointsName;
            case ITEM_STACK -> amount + " × " + itemLabel();
            case PERMISSION -> "权限 " + permission();
            case ACTIVITY -> "活动获取";
            case NOT -> "免费";
            default -> buyType.id() + " " + amount;
        };
    }

    public String permission() {
        return buyData.isBlank() ? "liutitle.title." + titleId : buyData.trim();
    }

    private String itemLabel() {
        if (buyData.isBlank()) {
            return "物品";
        }
        String first = buyData.replace("\r", "").split("\n")[0].trim();
        if (first.equalsIgnoreCase("item:") || first.startsWith("item:")) {
            for (String line : buyData.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("id:")) {
                    return trimmed.substring(3).trim().replace("minecraft:", "");
                }
            }
            return "物品";
        }
        return first;
    }
}
