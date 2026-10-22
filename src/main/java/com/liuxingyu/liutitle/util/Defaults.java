package com.liuxingyu.liutitle.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 把 jar 内默认资源里“缺失的键”补进插件数据目录的 yml，保留用户已改的值。 */
public final class Defaults {

    private Defaults() {
    }

    public static void merge(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
            if (mergeSection(defaults, data, "")) {
                data.save(file);
            }
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().warning("补全 " + fileName + " 失败：" + error.getMessage());
        }
    }

    private static boolean mergeSection(ConfigurationSection defaults, ConfigurationSection data, String path) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object value = defaults.get(key);
            if (value instanceof ConfigurationSection section) {
                ConfigurationSection target = data.getConfigurationSection(key);
                if (target == null) {
                    target = data.createSection(key);
                    changed = true;
                }
                changed |= mergeSection(section, target, path.isEmpty() ? key : path + "." + key);
            } else if (!data.contains(key)) {
                data.set(key, value);
                changed = true;
            }
        }
        return changed;
    }
}
