package com.liuxingyu.liutitle.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/** 主配置快照。 */
public final class Settings {

    private final String defaultTitle;
    private final String prefixBrackets;
    private final String suffixBrackets;
    private final String displayPrefix;
    private final String displaySuffix;
    private final int toggleCooldown;
    private final boolean buffEnabled;
    private final List<String> disabledBuffWorlds;
    private final boolean autoUse;
    private final int loginDelay;
    private final int defaultDays;
    private final List<String> placeholderAliases;
    private final String shopSort;
    private final boolean shopShowHidden;
    private final boolean importEnabled;

    private Settings(FileConfiguration c) {
        this.defaultTitle = c.getString("default-title", "&7萌新");
        this.prefixBrackets = c.getString("prefix-brackets", "");
        this.suffixBrackets = c.getString("suffix-brackets", "");
        this.displayPrefix = c.getString("display-prefix", "&f[ ");
        this.displaySuffix = c.getString("display-suffix", " &f]");
        this.toggleCooldown = Math.max(0, c.getInt("toggle-cooldown", 1));
        this.buffEnabled = c.getBoolean("buff.enabled", true);
        this.disabledBuffWorlds = c.getStringList("buff.disabled-worlds").stream()
                .map(s -> s.toLowerCase(Locale.ROOT)).toList();
        this.autoUse = c.getBoolean("auto-use", false);
        this.loginDelay = Math.max(0, c.getInt("login-delay", 1));
        this.defaultDays = Math.max(0, c.getInt("default-days", 0));
        this.placeholderAliases = c.getStringList("placeholder-aliases").stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.shopSort = c.getString("shop.sort", "id").toLowerCase(Locale.ROOT);
        this.shopShowHidden = c.getBoolean("shop.show-hidden", false);
        this.importEnabled = c.getBoolean("import.enabled", true);
    }

    public static Settings load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        return new Settings(plugin.getConfig());
    }

    public String defaultTitle() { return defaultTitle; }
    public String prefixBrackets() { return prefixBrackets; }
    public String suffixBrackets() { return suffixBrackets; }
    public String displayPrefix() { return displayPrefix; }
    public String displaySuffix() { return displaySuffix; }
    public int toggleCooldown() { return toggleCooldown; }
    public boolean buffEnabled() { return buffEnabled; }
    public List<String> disabledBuffWorlds() { return disabledBuffWorlds; }
    public boolean autoUse() { return autoUse; }
    public int loginDelay() { return loginDelay; }
    public int defaultDays() { return defaultDays; }
    public List<String> placeholderAliases() { return placeholderAliases; }
    public String shopSort() { return shopSort; }
    public boolean shopShowHidden() { return shopShowHidden; }
    public boolean importEnabled() { return importEnabled; }
}
