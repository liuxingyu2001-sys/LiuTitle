package com.liuxingyu.liutitle.event;

import com.liuxingyu.liutitle.model.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** 玩家佩戴称号前触发，可取消。 */
public class TitleEquipEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Title title;
    private final boolean buff;
    private boolean cancelled;

    public TitleEquipEvent(Player player, Title title, boolean buff) {
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

    /** true = 属性称号，false = 展示称号。 */
    public boolean buff() {
        return buff;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
