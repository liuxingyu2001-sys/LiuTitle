package com.liuxingyu.liutitle.service;

import com.liuxingyu.liutitle.config.Settings;
import com.liuxingyu.liutitle.model.Buff;
import com.liuxingyu.liutitle.model.BuffType;
import com.liuxingyu.liutitle.model.PotionBuff;
import com.liuxingyu.liutitle.model.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 称号属性：potion_effect 与原生 attribute。 */
public final class BuffService {

    private static final Map<String, String> LEGACY_EFFECTS = Map.ofEntries(
            Map.entry("fast_digging", "haste"),
            Map.entry("slow", "slowness"),
            Map.entry("increase_damage", "strength"),
            Map.entry("damage_resistance", "resistance"),
            Map.entry("jump", "jump_boost"),
            Map.entry("confusion", "nausea"),
            Map.entry("heal", "instant_health"),
            Map.entry("harm", "instant_damage")
    );

    private final Settings settings;
    private final Map<UUID, Set<PotionEffectType>> appliedEffects = new HashMap<>();
    private final Map<UUID, Set<NamespacedKey>> appliedAttributes = new HashMap<>();

    public BuffService(Settings settings) {
        this.settings = settings;
    }

    public boolean disabledWorld(Player player) {
        return settings.disabledBuffWorlds().contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    /** 应用称号属性（先清理旧属性）。 */
    public void apply(Player player, Title title) {
        clear(player);
        if (title == null || !settings.buffEnabled() || disabledWorld(player)) {
            return;
        }
        for (Buff buff : title.buffs()) {
            if (!buff.wear() || !buff.type().nativeSupported()) {
                continue;
            }
            if (buff.type() == BuffType.POTION_EFFECT) {
                applyPotion(player, buff);
            } else if (buff.type() == BuffType.ATTRIBUTE) {
                applyAttributes(player, title, buff);
            }
        }
    }

    private void applyPotion(Player player, Buff buff) {
        String rawName = PotionBuff.parseName(buff.content());
        if (rawName == null) {
            return;
        }
        PotionEffectType type = resolveEffect(rawName);
        if (type == null) {
            return;
        }
        int level = PotionBuff.parseLevel(buff.content());
        PotionEffect effect = new PotionEffect(type, -1, Math.max(0, level - 1),
                true, !PotionBuff.parseHidden(buff.content()), false);
        player.addPotionEffect(effect);
        appliedEffects.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(type);
    }

    private void applyAttributes(Player player, Title title, Buff buff) {
        for (String entry : split(buff.content())) {
            String[] pair = entry.split("[:=]", 2);
            if (pair.length != 2) {
                continue;
            }
            Attribute attribute = resolveAttribute(pair[0].trim());
            if (attribute == null) {
                continue;
            }
            double amount;
            try {
                amount = Double.parseDouble(pair[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            NamespacedKey key = new NamespacedKey("liutitle", "title_" + title.id() + "_" + attribute.getKey().getKey());
            try {
                instance.removeModifier(key);
            } catch (RuntimeException ignored) {
                // 不存在
            }
            instance.addTransientModifier(new AttributeModifier(key, amount,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            appliedAttributes.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(key);
        }
    }

    /** 清理本插件施加的所有属性。 */
    public void clear(Player player) {
        Set<PotionEffectType> effects = appliedEffects.remove(player.getUniqueId());
        if (effects != null) {
            for (PotionEffectType type : effects) {
                player.removePotionEffect(type);
            }
        }
        Set<NamespacedKey> keys = appliedAttributes.remove(player.getUniqueId());
        if (keys != null && !keys.isEmpty()) {
            for (Attribute attribute : Registry.ATTRIBUTE) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }
                for (NamespacedKey key : keys) {
                    try {
                        instance.removeModifier(key);
                    } catch (RuntimeException ignored) {
                        // 不存在
                    }
                }
            }
        }
    }

    public void clear(UUID uuid) {
        appliedEffects.remove(uuid);
        appliedAttributes.remove(uuid);
    }

    private static List<String> split(String content) {
        List<String> parts = new ArrayList<>();
        for (String line : content.split("[;\\n]")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private static PotionEffectType resolveEffect(String name) {
        String key = name.toLowerCase(Locale.ROOT).replace(' ', '_');
        key = LEGACY_EFFECTS.getOrDefault(key, key);
        return Registry.EFFECT.get(NamespacedKey.minecraft(key));
    }

    private static Attribute resolveAttribute(String name) {
        String key = name.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_');
        if (key.startsWith("generic_")) {
            key = key.substring("generic_".length());
        }
        if (key.startsWith("player_")) {
            key = key.substring("player_".length());
        }
        Attribute direct = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (direct != null) {
            return direct;
        }
        for (Attribute attribute : Registry.ATTRIBUTE) {
            if (attribute.getKey().getKey().equals(key)) {
                return attribute;
            }
        }
        return null;
    }
}
