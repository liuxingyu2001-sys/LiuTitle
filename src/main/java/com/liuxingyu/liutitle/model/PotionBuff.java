package com.liuxingyu.liutitle.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** potion_effect 类型的 buff 内容解析。 */
public final class PotionBuff {

    private static final Pattern NAME = Pattern.compile("\"potionName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LEVEL = Pattern.compile("\"potionLevel\"\\s*:\\s*(\\d+)");
    private static final Pattern HIDE = Pattern.compile("\"potionHide\"\\s*:\\s*(true|false)");

    private PotionBuff() {
    }

    public static String parseName(String content) {
        return match(NAME, content);
    }

    public static int parseLevel(String content) {
        String raw = match(LEVEL, content);
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static boolean parseHidden(String content) {
        String raw = match(HIDE, content);
        return "true".equalsIgnoreCase(raw);
    }

    private static String match(Pattern pattern, String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
