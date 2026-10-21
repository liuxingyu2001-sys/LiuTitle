package com.liuxingyu.liutitle.event;

import com.liuxingyu.liutitle.model.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** 购买/续期称号前触发，可取消。 */
public class TitlePurchaseEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Title title;
    private final boolean renew;
    private boolean cancelled;

    public TitlePurchaseEvent(Player player, Title title, boolean renew) {
        this.player = player;
        this.title = title;
        this.renew = renew;
    }

    public Player player() {
        return player;
    }

    public Title title() {
        return title;
    }

    public boolean renew() {
        return renew;
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
