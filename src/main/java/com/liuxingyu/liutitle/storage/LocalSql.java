package com.liuxingyu.liutitle.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 内置 SQLite 兜底：仅当 LiuLib 的连接池与 SQLite 都初始化失败时使用。
 */
final class LocalSql implements Sql {

    private final Logger logger;
    private final String prefix;
    private HikariDataSource dataSource;

    private LocalSql(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }

    static LocalSql create(JavaPlugin plugin, Logger logger, String prefix) {
        LocalSql sql = new LocalSql(logger, prefix);
        return sql.init(plugin) ? sql : null;
    }

    private boolean init(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("无法创建插件数据目录：" + dataFolder);
            return false;
        }
        File file = new File(dataFolder, "liutitle.db");
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("LiuTitle-SQLite");
        hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setConnectionTimeout(10_000L);
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setAutoCommit(true);
        try {
            this.dataSource = new HikariDataSource(hikari);
            try (Connection ignored = dataSource.getConnection()) {
                return true;
            }
        } catch (Exception e) {
            logger.severe("本地 SQLite 初始化失败：" + e.getMessage());
            close();
            return false;
        }
    }

    @Override
    public boolean mysql() {
        return false;
    }

    @Override
    public String table(String name) {
        return prefix + name;
    }

    @Override
    public boolean ready() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public String backend() {
        return "内置 SQLite";
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("查询失败：" + e.getMessage() + " | " + sql);
        }
        return out;
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("更新失败：" + e.getMessage() + " | " + sql);
            return -1;
        }
    }

    @Override
    public void execute(String sql) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            logger.warning("执行失败：" + e.getMessage() + " | " + sql);
        }
    }

    @Override
    public int insert(String sql, Object... params) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            if (ps.executeUpdate() == 0) {
                return -1;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            logger.warning("插入失败：" + e.getMessage() + " | " + sql);
            return -1;
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
