package com.liuxingyu.liutitle.service;

import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.storage.Sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 导入 PlayerTitle 数据：
 * <ul>
 *   <li>{@code *.sql} / {@code *.sql.gz}：MySQL 备份（INSERT INTO `table` VALUES ...）</li>
 *   <li>{@code *.db}：PlayerTitle 的 SQLite 数据文件（时间字段为 epoch 毫秒）</li>
 * </ul>
 * 规则：称号定义（title_list/buff/require）全量导入；玩家称号只导入未过期的。
 */
public final class ImportService {

    private static final Pattern INSERT = Pattern.compile(
            "INSERT\\s+INTO\\s+`?([a-zA-Z_]+)`?\\s+VALUES\\s+(.+?);", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public record Result(int titles, int buffs, int requires, int owned, int expired, String source) {
        public static Result empty(String source) {
            return new Result(0, 0, 0, 0, 0, source);
        }
    }

    private final Sql sql;
    private final Logger logger;

    public ImportService(Sql sql, Logger logger) {
        this.sql = sql;
        this.logger = logger;
    }

    public List<Path> candidates(Path path) {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.filter(Files::isRegularFile).forEach(files::add);
            } catch (IOException e) {
                logger.warning("扫描导入目录失败：" + e.getMessage());
            }
        } else if (Files.isRegularFile(path)) {
            files.add(path);
        }
        files.sort(Path::compareTo);
        // 同一份数据同时有 .sql 和 .sql.gz 时只导入一次
        List<Path> unique = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".sql.gz")) {
                String plain = name.substring(0, name.length() - 3);
                boolean hasPlain = files.stream()
                        .anyMatch(f -> f.getFileName().toString().equalsIgnoreCase(plain));
                if (hasPlain) {
                    continue;
                }
            }
            unique.add(file);
        }
        return unique;
    }

    public Result importAll(Path path) {
        int titles = 0;
        int buffs = 0;
        int requires = 0;
        int owned = 0;
        int expired = 0;
        List<Path> files = candidates(path);
        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            try {
                if (name.endsWith(".sql") || name.endsWith(".sql.gz")) {
                    Counts counts = importSql(file);
                    titles += counts.titles;
                    buffs += counts.buffs;
                    requires += counts.requires;
                    owned += counts.owned;
                    expired += counts.expired;
                } else if (name.endsWith(".db")) {
                    Counts counts = importSqlite(file);
                    titles += counts.titles;
                    buffs += counts.buffs;
                    requires += counts.requires;
                    owned += counts.owned;
                    expired += counts.expired;
                }
            } catch (Exception e) {
                logger.warning("导入 " + file.getFileName() + " 失败：" + e.getMessage());
                return new Result(titles, buffs, requires, owned, expired, e.getMessage());
            }
        }
        return new Result(titles, buffs, requires, owned, expired, path.toString());
    }

    private record Counts(int titles, int buffs, int requires, int owned, int expired) {
    }

    // ==================================================================
    // MySQL dump
    // ==================================================================

    private Counts importSql(Path file) throws Exception {
        int titles = 0;
        int buffs = 0;
        int requires = 0;
        int owned = 0;
        int expired = 0;
        try (BufferedReader reader = open(file)) {
            StringBuilder pending = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("INSERT INTO")) {
                    continue;
                }
                pending.setLength(0);
                pending.append(line);
                while (!line.trim().endsWith(";") && (line = reader.readLine()) != null) {
                    pending.append('\n').append(line);
                }
                Matcher matcher = INSERT.matcher(pending.toString());
                if (!matcher.find()) {
                    continue;
                }
                String table = matcher.group(1).toLowerCase(Locale.ROOT);
                for (String tuple : tuples(matcher.group(2))) {
                    List<String> values = values(tuple);
                    switch (table) {
                        case "title_list" -> {
                            if (values.size() >= 9 && insertTitle(values)) {
                                titles++;
                            }
                        }
                        case "title_buff" -> {
                            if (values.size() >= 6 && insertBuff(values)) {
                                buffs++;
                            }
                        }
                        case "title_require" -> {
                            if (values.size() >= 6 && insertRequire(values)) {
                                requires++;
                            }
                        }
                        case "title_player" -> {
                            if (values.size() >= 9) {
                                if (expired(values.get(5))) {
                                    expired++;
                                } else if (insertPlayer(values)) {
                                    owned++;
                                }
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
        }
        return new Counts(titles, buffs, requires, owned, expired);
    }

    private BufferedReader open(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)),
                    StandardCharsets.UTF_8));
        }
        return Files.newBufferedReader(file, StandardCharsets.UTF_8);
    }

    // ==================================================================
    // PlayerTitle SQLite
    // ==================================================================

    private Counts importSqlite(Path file) throws Exception {
        int titles = 0;
        int buffs = 0;
        int requires = 0;
        int owned = 0;
        int expired = 0;
        String url = "jdbc:sqlite:file:" + file.toAbsolutePath() + "?mode=ro";
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("SELECT * FROM title_list")) {
                while (rs.next()) {
                    String[] v = {
                            str(rs, "id"), rs.getString("title_name"), rs.getString("description"),
                            str(rs, "day"), str(rs, "is_hide"), str(rs, "position"),
                            rs.getString("buy_type"), str(rs, "amount"), rs.getString("item_stack")
                    };
                    if (insertTitle(java.util.Arrays.asList(v))) {
                        titles++;
                    }
                }
            }
            try (ResultSet rs = statement.executeQuery("SELECT * FROM title_buff")) {
                while (rs.next()) {
                    String[] v = {
                            str(rs, "id"), str(rs, "title_id"), rs.getString("buff_type"),
                            rs.getString("buff_content"), rs.getString("description"), rs.getString("apply_type")
                    };
                    if (insertBuff(java.util.Arrays.asList(v))) {
                        buffs++;
                    }
                }
            }
            try (ResultSet rs = statement.executeQuery("SELECT * FROM title_require")) {
                while (rs.next()) {
                    String[] v = {
                            str(rs, "id"), str(rs, "title_id"), rs.getString("buy_type"),
                            str(rs, "amount"), rs.getString("buy_data"), rs.getString("create_time")
                    };
                    if (insertRequire(java.util.Arrays.asList(v))) {
                        requires++;
                    }
                }
            }
            try (ResultSet rs = statement.executeQuery("SELECT * FROM title_player")) {
                while (rs.next()) {
                    String[] v = {
                            str(rs, "id"), rs.getString("player_name"), rs.getString("player_uuid"),
                            str(rs, "title_id"), rs.getString("title_name"),
                            str(rs, "expiration_time"), str(rs, "is_use"), str(rs, "is_use_buff"), "0"
                    };
                    if (expired(v[5])) {
                        expired++;
                    } else if (insertPlayer(java.util.Arrays.asList(v))) {
                        owned++;
                    }
                }
            }
        }
        return new Counts(titles, buffs, requires, owned, expired);
    }

    private static String str(ResultSet rs, String column) {
        try {
            Object value = rs.getObject(column);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================================================================
    // 写入
    // ==================================================================

    private String replace() {
        return sql.mysql() ? "REPLACE INTO " : "INSERT OR REPLACE INTO ";
    }

    private boolean insertTitle(List<String> v) {
        int id = num(v.get(0), -1);
        if (id <= 0 || v.get(1) == null) {
            return false;
        }
        sql.update(replace() + sql.table("title_list")
                        + " (id, title_name, description, day, is_hide, position, buy_type, amount, item_stack)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, v.get(1), v.get(2), num(v.get(3), 0), num(v.get(4), 0), num(v.get(5), 0),
                v.get(6), v.get(7) == null ? null : num(v.get(7), 0), v.get(8));
        return true;
    }

    private boolean insertBuff(List<String> v) {
        int id = num(v.get(0), -1);
        if (id <= 0) {
            return false;
        }
        sql.update(replace() + sql.table("title_buff")
                        + " (id, title_id, buff_type, buff_content, description, apply_type)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                id, num(v.get(1), 0), v.get(2), v.get(3), v.get(4), v.get(5) == null ? "wear" : v.get(5));
        return true;
    }

    private boolean insertRequire(List<String> v) {
        int id = num(v.get(0), -1);
        if (id <= 0) {
            return false;
        }
        sql.update(replace() + sql.table("title_require")
                        + " (id, title_id, buy_type, amount, buy_data, create_time)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                id, num(v.get(1), 0), v.get(2), v.get(3) == null ? 0 : num(v.get(3), 0), v.get(4), v.get(5));
        return true;
    }

    private boolean insertPlayer(List<String> v) {
        int id = num(v.get(0), -1);
        if (id <= 0 || v.get(2) == null || v.get(3) == null) {
            return false;
        }
        LocalDateTime time = PlayerTitle.parseTime(v.get(5));
        sql.update(replace() + sql.table("title_player")
                        + " (id, player_name, player_uuid, title_id, title_name, expiration_time,"
                        + " is_use, is_use_buff, is_use_particle) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, v.get(1), v.get(2), num(v.get(3), 0), v.get(4),
                PlayerTitle.formatTime(time), num(v.get(6), 0), num(v.get(7), 0), num(v.get(8), 0));
        return true;
    }

    private static boolean expired(String raw) {
        LocalDateTime time = PlayerTitle.parseTime(raw);
        return time.isBefore(LocalDateTime.now());
    }

    private static int num(String raw, int fallback) {
        if (raw == null || raw.isBlank() || "NULL".equalsIgnoreCase(raw)) {
            return fallback;
        }
        try {
            return (int) Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ==================================================================
    // SQL 值解析
    // ==================================================================

    private static List<String> tuples(String body) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quoted) {
                current.append(c);
                if (c == '\\' && i + 1 < body.length()) {
                    current.append(body.charAt(++i));
                } else if (c == '\'') {
                    quoted = false;
                }
                continue;
            }
            if (c == '\'') {
                quoted = true;
                current.append(c);
            } else if (c == '(') {
                depth++;
                if (depth == 1) {
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    out.add(current.toString());
                } else {
                    current.append(c);
                }
            } else if (depth > 0) {
                current.append(c);
            }
        }
        return out;
    }

    private static List<String> values(String tuple) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < tuple.length(); i++) {
            char c = tuple.charAt(i);
            if (quoted) {
                if (c == '\\' && i + 1 < tuple.length()) {
                    current.append(unescape(tuple.charAt(++i)));
                } else if (c == '\'') {
                    quoted = false;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (c == '\'') {
                quoted = true;
            } else if (c == ',') {
                out.add(finish(current));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(finish(current));
        return out;
    }

    private static String finish(StringBuilder builder) {
        String value = builder.toString().trim();
        if (value.isEmpty() || "NULL".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private static char unescape(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> c;
        };
    }
}
