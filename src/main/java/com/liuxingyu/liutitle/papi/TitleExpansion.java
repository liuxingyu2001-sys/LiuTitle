package com.liuxingyu.liutitle.papi;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liutitle.LiuTitle;
import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.service.PlayerTitleService;
import com.liuxingyu.liutitle.service.TitleService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** PlaceholderAPI 变量。 */
public final class TitleExpansion extends PlaceholderExpansion {

    private final LiuTitle plugin;
    private final String identifier;

    public TitleExpansion(LiuTitle plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    private TitleService titleService() {
        return plugin.titles();
    }

    private PlayerTitleService playerTitleService() {
        return plugin.playerTitles();
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "liuxingyu2001";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        UUID uuid = player.getUniqueId();
        PlayerTitle show = playerTitleService().equipped(uuid, false);
        PlayerTitle buff = playerTitleService().equipped(uuid, true);
        switch (key) {
            case "", "title", "use", "display" -> {
                return show == null ? "" : legacy(show.titleName());
            }
            case "buff", "use_buff" -> {
                return buff == null ? "" : legacy(buff.titleName());
            }
            case "use_particle", "particle" -> {
                return "";
            }
            case "count", "owned" -> {
                return String.valueOf(playerTitleService().count(uuid));
            }
            case "id" -> {
                return show == null ? "" : String.valueOf(show.titleId());
            }
            case "buff_id" -> {
                return buff == null ? "" : String.valueOf(buff.titleId());
            }
            case "expire", "expiration" -> {
                return show == null ? "" : show.expireLabel();
            }
            case "buff_expire" -> {
                return buff == null ? "" : buff.expireLabel();
            }
            case "list" -> {
                List<PlayerTitle> owned = playerTitleService().owned(uuid);
                StringBuilder builder = new StringBuilder();
                for (PlayerTitle item : owned) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(legacy(item.titleName()));
                }
                return builder.toString();
            }
            default -> {
            }
        }
        if (key.startsWith("has_")) {
            String needle = key.substring("has_".length());
            for (PlayerTitle item : playerTitleService().owned(uuid)) {
                Title title = titleService().get(item.titleId());
                String plain = title == null ? item.titleName()
                        : LiuLib.text().plain(LiuLib.text().parse(title.name()));
                if (plain.toLowerCase(Locale.ROOT).equals(needle)
                        || item.titleName().toLowerCase(Locale.ROOT).equals(needle)) {
                    return "yes";
                }
            }
            return "no";
        }
        if (key.startsWith("expire_")) {
            String raw = key.substring("expire_".length());
            try {
                int id = Integer.parseInt(raw);
                PlayerTitle item = playerTitleService().find(uuid, id);
                return item == null ? "" : item.expireLabel();
            } catch (NumberFormatException e) {
                return "";
            }
        }
        return null;
    }

    private static String legacy(String raw) {
        if (raw == null) {
            return "";
        }
        return LiuLib.text().legacy(LiuLib.text().parse(raw));
    }
}
