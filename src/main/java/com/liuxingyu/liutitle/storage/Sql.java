package com.liuxingyu.liutitle.storage;

import com.liuxingyu.liulib.LiuLib;
import com.liuxingyu.liulib.api.LiuLibApi;
import com.liuxingyu.liulib.storage.MySql;
import com.liuxingyu.liulib.storage.SqlConfig;
import com.liuxingyu.liulib.storage.Sqlite;
import com.liuxingyu.liulib.storage.SqliteConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 存储抽象。
 *
 * <p>设计：<b>连接凭据只维护一份</b>（LiuLib 的 mysql 配置），而<b>库名与表前缀按插件隔离</b>。
 * 插件自己的 config.yml 只需 {@code storage.database} 与 {@code storage.table-prefix}。</p>
 *
 * <p>优先级：LiuLib MySQL 就绪（同凭据 + 本插件库）→ 本插件 SQLite 文件 → 内置 HikariCP SQLite。</p>
 */
public interface Sql {

    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** 是否 MySQL 方言（自增 / 分页等差异）。 */
    boolean mysql();

    /** 表名（附带本插件配置的表前缀）。 */
    String table(String name);

    <T> java.util.List<T> query(String sql, RowMapper<T> mapper, Object... params);

    <T> T queryOne(String sql, RowMapper<T> mapper, Object... params);

    int update(String sql, Object... params);

    void execute(String sql);

    /** 插入并返回自增主键，失败返回 -1。 */
    int insert(String sql, Object... params);

    void close();

    boolean ready();

    /** 后端描述，用于启动日志。 */
    String backend();

    /**
     * 创建存储。
     *
     * @return 可用存储；全部失败时返回 {@code null}
     */
    static Sql create(JavaPlugin plugin, Logger logger) {
        FileConfiguration config = plugin.getConfig();
        String driver = config.getString("storage.driver", "auto").toLowerCase(java.util.Locale.ROOT);
        String database = config.getString("storage.database", "liutitle");
        String prefix = config.getString("storage.table-prefix", "liutitle_");
        String sqliteFile = config.getString("storage.sqlite-file", "liutitle.db");
        boolean autoCreate = config.getBoolean("storage.auto-create-database", true);
        if (prefix == null || prefix.isBlank()) {
            prefix = "liutitle_";
        }
        if (sqliteFile == null || sqliteFile.isBlank()) {
            sqliteFile = "liutitle.db";
        }
        // 凭据来自 LiuLib；库名/前缀/连接池/超时由本插件配置管理（0 = 跟随 LiuLib）
        try {
            LiuLibApi api = LiuLib.apiOrNull();
            if (api != null) {
                if (!"sqlite".equals(driver)) {
                    MySql libMySql = api.mysql();
                    if (libMySql != null && libMySql.isReady()) {
                        SqlConfig base = libMySql.config();
                        String targetDatabase = database == null || database.isBlank()
                                ? base.database() : database.trim();
                        int pool = config.getInt("storage.pool-size", 0);
                        int timeout = config.getInt("storage.connection-timeout-ms", 0);
                        String sslRaw = config.getString("storage.use-ssl", null);
                        boolean ssl = sslRaw == null || sslRaw.isBlank()
                                ? base.useSsl() : Boolean.parseBoolean(sslRaw);
                        SqlConfig own = new SqlConfig(true, base.host(), base.port(), targetDatabase,
                                base.username(), base.password(), prefix,
                                pool > 0 ? pool : base.poolSize(),
                                timeout > 0 ? timeout : base.connectionTimeoutMs(),
                                ssl, autoCreate);
                        MySql mysql = new MySql(own, logger);
                        if (mysql.init() && mysql.isReady()) {
                            logger.info("使用 MySQL 存储：库 " + own.database() + "，表前缀 " + own.tablePrefix()
                                    + "，连接池 " + own.poolSize());
                            return new LiuLibSql(mysql, null, logger);
                        }
                        logger.warning("连接 MySQL 库 " + targetDatabase + " 失败，回退本插件 SQLite");
                    } else if ("mysql".equals(driver)) {
                        logger.warning("storage.driver=mysql，但 LiuLib 的 mysql 未启用或未就绪，回退本插件 SQLite");
                    }
                }
                Sqlite sqlite = new Sqlite(plugin, new SqliteConfig(true, sqliteFile, prefix), logger);
                if (sqlite.init() && sqlite.isReady()) {
                    logger.info("使用 SQLite 存储：" + sqlite.file() + "（表前缀 " + prefix + "）");
                    return new LiuLibSql(null, sqlite, logger);
                }
                logger.warning("初始化本插件 SQLite 失败，回退内置 SQLite 实现");
            }
        } catch (Throwable t) {
            logger.warning("初始化 LiuLib 存储失败，回退内置 SQLite：" + t);
        }
        LocalSql local = LocalSql.create(plugin, logger, prefix);
        if (local == null) {
            return null;
        }
        logger.info("使用内置 SQLite（plugins/LiuTitle/liutitle.db），表前缀 " + prefix);
        return local;
    }
}
