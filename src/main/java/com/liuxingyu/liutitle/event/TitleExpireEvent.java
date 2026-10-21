package com.liuxingyu.liutitle.event;

import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** 称号到期被移除时触发（玩家可能离线，此时 player 为 null）。 */
public class TitleExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerTitle owned;
    private final Title title;

    public TitleExpireEvent(Player player, PlayerTitle owned, Title title) {
        this.player = player;
        this.owned = owned;
        this.title = title;
    }

    public Player player() {
        return player;
    }

    public PlayerTitle owned() {
        return owned;
    }

    public Title title() {
        return title;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
