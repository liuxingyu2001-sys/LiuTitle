package com.liuxingyu.liutitle.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** gui.yml 布局。 */
public final class GuiConfig {

    public record Icon(String item, boolean enchant, String name, List<String> lore) {
        public static final Icon EMPTY = new Icon("", false, "", List.of());
    }

    public record Menu(String id, String title, int size, List<Integer> slots,
                       int prev, int next, int close, int info,
                       Icon item, Map<String, String> buttons,
                       Icon infoItem, Icon closeItem, Icon prevItem, Icon nextItem) {
    }

    private final Map<String, Menu> menus = new HashMap<>();

    public static GuiConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        GuiConfig config = new GuiConfig();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            List<Integer> slots = new ArrayList<>();
            for (int slot : section.getIntegerList("slots")) {
                if (slot >= 0) {
                    slots.add(slot);
                }
            }
            Map<String, String> buttons = new HashMap<>();
            ConfigurationSection buttonSection = section.getConfigurationSection("buttons");
            if (buttonSection != null) {
                for (String key : buttonSection.getKeys(false)) {
                    buttons.put(key, buttonSection.getString(key, ""));
                }
            }
            Menu menu = new Menu(
                    id,
                    section.getString("title", "&8" + id),
                    Math.max(9, Math.min(54, section.getInt("size", 54)) / 9 * 9),
                    slots,
                    section.getInt("page-prev", 45),
                    section.getInt("page-next", 53),
                    section.getInt("close", 49),
                    section.getInt("info", -1),
                    icon(section.getConfigurationSection("item")),
                    buttons,
                    icon(section.getConfigurationSection("info-item")),
                    icon(section.getConfigurationSection("close-item")),
                    icon(section.getConfigurationSection("prev-item")),
                    icon(section.getConfigurationSection("next-item"))
            );
            config.menus.put(id, menu);
        }
        return config;
    }

    private static Icon icon(ConfigurationSection section) {
        if (section == null) {
            return Icon.EMPTY;
        }
        return new Icon(
                section.getString("item", ""),
                section.getBoolean("enchant", false),
                section.getString("name", ""),
                section.getStringList("lore")
        );
    }

    public Menu menu(String id) {
        return menus.get(id);
    }
}
