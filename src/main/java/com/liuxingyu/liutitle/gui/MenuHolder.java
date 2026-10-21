package com.liuxingyu.liutitle.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** GUI 标识与槽位数据。 */
public final class MenuHolder implements InventoryHolder {

    public enum Type {
        SHOP, WAREHOUSE, ADMIN_SHOP, ADMIN_OWNERS, ADMIN_PLAYER
    }

    private final Type type;
    private final int page;
    private final int titleId;
    private final UUID target;
    private final String targetName;
    private final Map<Integer, Object> mapping = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(Type type, int page) {
        this(type, page, 0, null, "");
    }

    public MenuHolder(Type type, int page, int titleId, UUID target, String targetName) {
        this.type = type;
        this.page = page;
        this.titleId = titleId;
        this.target = target;
        this.targetName = targetName == null ? "" : targetName;
    }

    public Type type() {
        return type;
    }

    public int page() {
        return page;
    }

    public int titleId() {
        return titleId;
    }

    public UUID target() {
        return target;
    }

    public String targetName() {
        return targetName;
    }

    public void map(int slot, Object value) {
        mapping.put(slot, value);
    }

    public Object at(int slot) {
        return mapping.get(slot);
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
