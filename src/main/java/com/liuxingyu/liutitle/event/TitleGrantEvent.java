package com.liuxingyu.liutitle.event;

import com.liuxingyu.liutitle.model.Title;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** 发放称号后触发（玩家可能离线）。 */
public class TitleGrantEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final String playerName;
    private final Title title;
    private final int days;

    public TitleGrantEvent(UUID player, String playerName, Title title, int days) {
        this.player = player;
        this.playerName = playerName;
        this.title = title;
        this.days = days;
    }

    public UUID player() {
        return player;
    }

    public String playerName() {
        return playerName;
    }

    public Title title() {
        return title;
    }

    /** 0 表示永久。 */
    public int days() {
        return days;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
