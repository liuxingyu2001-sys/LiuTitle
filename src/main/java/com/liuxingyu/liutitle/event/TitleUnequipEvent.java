package com.liuxingyu.liutitle.event;

import com.liuxingyu.liutitle.model.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** 玩家取消佩戴称号后触发。 */
public class TitleUnequipEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Title title;
    private final boolean buff;

    public TitleUnequipEvent(Player player, Title title, boolean buff) {
        this.player = player;
        this.title = title;
        this.buff = buff;
    }

    public Player player() {
        return player;
    }

    public Title title() {
        return title;
    }

    public boolean buff() {
        return buff;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
