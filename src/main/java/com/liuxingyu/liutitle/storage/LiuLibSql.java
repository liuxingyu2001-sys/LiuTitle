package com.liuxingyu.liutitle.storage;

import com.liuxingyu.liulib.storage.MySql;
import com.liuxingyu.liulib.storage.Sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 使用 LiuLib 的连接池实现，但连接池由本插件创建并持有：
 * 库名与表前缀按本插件配置隔离，连接凭据来自 LiuLib。
 */
final class LiuLibSql implements Sql {

    private final MySql mysql;
    private final Sqlite sqlite;
    private final java.util.logging.Logger logger;

    LiuLibSql(MySql mysql, Sqlite sqlite, java.util.logging.Logger logger) {
        this.mysql = mysql;
        this.sqlite = sqlite;
        this.logger = logger;
    }

    @Override
    public boolean mysql() {
        return mysql != null;
    }

    @Override
    public String table(String name) {
        return mysql != null ? mysql.table(name) : sqlite.table(name);
    }

    @Override
    public boolean ready() {
        return mysql != null ? mysql.isReady() : sqlite != null && sqlite.isReady();
    }

    @Override
    public String backend() {
        return mysql != null ? "LiuLib/MySQL" : "LiuLib/SQLite";
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        if (mysql != null) {
            return mysql.query(sql, mapper::map, params);
        }
        return sqlite.query(sql, mapper::map, params);
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int update(String sql, Object... params) {
        return mysql != null ? mysql.update(sql, params) : sqlite.update(sql, params);
    }

    @Override
    public void execute(String sql) {
        if (mysql != null) {
            mysql.execute(sql);
        } else {
            sqlite.execute(sql);
        }
    }

    @Override
    public int insert(String sql, Object... params) {
        // 自增主键必须与 INSERT 在同一连接上取：SQLite 用 RETURNING，MySQL 用 generated keys
        if (sqlite != null) {
            try {
                Long id = queryOne(sql + " RETURNING id", rs -> rs.getLong(1), params);
                return id == null ? -1 : id.intValue();
            } catch (RuntimeException e) {
                logger.warning("插入失败：" + e.getMessage() + " | " + sql);
                return -1;
            }
        }
        try (java.sql.Connection connection = mysql.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            if (statement.executeUpdate() == 0) {
                return -1;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            logger.warning("插入失败：" + e.getMessage() + " | " + sql);
            return -1;
        }
    }

    @Override
    public void close() {
        if (mysql != null) {
            mysql.close();
        } else if (sqlite != null) {
            sqlite.close();
        }
    }
}
