package com.liuxingyu.liutitle.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** 玩家拥有的称号（title_player 一行）。 */
public final class PlayerTitle {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int id;
    private String playerName;
    private UUID uuid;
    private int titleId;
    private String titleName;
    private LocalDateTime expiration;
    private boolean use;
    private boolean useBuff;

    public PlayerTitle(int id, String playerName, UUID uuid, int titleId, String titleName,
                       LocalDateTime expiration, boolean use, boolean useBuff) {
        this.id = id;
        this.playerName = playerName;
        this.uuid = uuid;
        this.titleId = titleId;
        this.titleName = titleName;
        this.expiration = expiration == null ? Title.PERMANENT : expiration;
        this.use = use;
        this.useBuff = useBuff;
    }

    public int id() { return id; }
    public void id(int id) { this.id = id; }
    public String playerName() { return playerName; }
    public void playerName(String name) { this.playerName = name; }
    public UUID uuid() { return uuid; }
    public int titleId() { return titleId; }
    public String titleName() { return titleName; }
    public void titleName(String name) { this.titleName = name; }
    public LocalDateTime expiration() { return expiration; }
    public void expiration(LocalDateTime time) { this.expiration = time == null ? Title.PERMANENT : time; }
    public boolean use() { return use; }
    public void use(boolean value) { this.use = value; }
    public boolean useBuff() { return useBuff; }
    public void useBuff(boolean value) { this.useBuff = value; }

    public boolean expired() {
        return expiration.isBefore(LocalDateTime.now());
    }

    public String expireLabel() {
        if (expiration.isAfter(Title.PERMANENT.minusYears(50))) {
            return "永久";
        }
        return expiration.format(FORMAT);
    }

    public static LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return Title.PERMANENT;
        }
        String value = raw.trim();
        try {
            long epoch = Long.parseLong(value);
            if (epoch > 100_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
            }
            if (epoch > 1_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
        } catch (NumberFormatException ignored) {
            // 非时间戳
        }
        try {
            if (value.length() == 10) {
                return LocalDateTime.parse(value + " 00:00:00", FORMAT);
            }
            return LocalDateTime.parse(value, FORMAT);
        } catch (RuntimeException e) {
            return Title.PERMANENT;
        }
    }

    public static String formatTime(LocalDateTime time) {
        return (time == null ? Title.PERMANENT : time).format(FORMAT);
    }
}
