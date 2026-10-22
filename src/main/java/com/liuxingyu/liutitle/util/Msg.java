package com.liuxingyu.liutitle.util;

import com.liuxingyu.liulib.LiuLib;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/** messages.yml 读取与发送。 */
public final class Msg {

    private final JavaPlugin plugin;
    private YamlConfiguration yaml = new YamlConfiguration();
    private String prefix = "";

    public Msg(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        Defaults.merge(plugin, "messages.yml");
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        String rawPrefix = yaml.getString("prefix", "");
        prefix = LiuLib.text().legacy(rawPrefix);
    }

    public String prefix() {
        return prefix;
    }

    public String raw(String key) {
        return yaml.getString(key, key);
    }

    public List<String> list(String key) {
        return yaml.getStringList(key);
    }

    /** 替换 {key} 占位符（键可写成 "{key}" 或 "key"）。 */
    public String format(String key, Object... pairs) {
        return LiuLib.text().fill(raw(key), normalize(pairs));
    }

    public String formatRaw(String raw, Object... pairs) {
        return LiuLib.text().fill(raw, normalize(pairs));
    }

    private static Object[] normalize(Object... pairs) {
        if (pairs == null) {
            return null;
        }
        Object[] out = pairs.clone();
        for (int i = 0; i + 1 < out.length; i += 2) {
            if (out[i] instanceof String key && key.length() > 2
                    && key.startsWith("{") && key.endsWith("}")) {
                out[i] = key.substring(1, key.length() - 1);
            }
        }
        return out;
    }

    /** 替换 GUI 模板中的 ${key} 占位符。 */
    public static String fillDollar(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String out = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            out = out.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return out;
    }

    public void send(CommandSender sender, String key, Object... pairs) {
        if (sender == null) {
            return;
        }
        String body = format(key, pairs);
        String text = prefix + body;
        sender.sendMessage(LiuLib.text().parse(text));
    }

    public void sendRaw(CommandSender sender, String raw) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(LiuLib.text().parse(raw));
    }

    public void sendList(CommandSender sender, String key) {
        for (String line : list(key)) {
            sender.sendMessage(LiuLib.text().parse(line));
        }
    }
}
