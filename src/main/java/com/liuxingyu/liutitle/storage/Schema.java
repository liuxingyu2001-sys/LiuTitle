package com.liuxingyu.liutitle.storage;

/**
 * 表结构。表名/字段与 PlayerTitle 保持一致，便于直接导入其 MySQL 备份。
 *
 * <p>只保留 LiuTitle 实际使用的四张表：没有称号币、数量奖励、粒子。</p>
 */
public final class Schema {

    private Schema() {
    }

    public static void apply(Sql sql) {
        boolean mysql = sql.mysql();
        String pk = mysql ? "INT NOT NULL AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String suffix = mysql ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : ")";
        sql.execute("CREATE TABLE IF NOT EXISTS " + sql.table("title_list") + " ("
                + "id " + pk + ","
                + "title_name VARCHAR(300) NOT NULL,"
                + "description VARCHAR(1000),"
                + "day INT NOT NULL DEFAULT 0,"
                + "is_hide INT NOT NULL DEFAULT 0,"
                + "position INT NOT NULL DEFAULT 0,"
                + "buy_type VARCHAR(64),"
                + "amount INT,"
                + "item_stack VARCHAR(10000)"
                + suffix);
        sql.execute("CREATE TABLE IF NOT EXISTS " + sql.table("title_buff") + " ("
                + "id " + pk + ","
                + "title_id INT NOT NULL,"
                + "buff_type VARCHAR(64) NOT NULL,"
                + "buff_content VARCHAR(10000),"
                + "description VARCHAR(1000),"
                + "apply_type VARCHAR(64) NOT NULL DEFAULT 'wear'"
                + suffix);
        sql.execute("CREATE TABLE IF NOT EXISTS " + sql.table("title_require") + " ("
                + "id " + pk + ","
                + "title_id INT NOT NULL,"
                + "buy_type VARCHAR(64) NOT NULL,"
                + "amount INT,"
                + "buy_data VARCHAR(10000),"
                + "create_time VARCHAR(32)"
                + suffix);
        sql.execute("CREATE TABLE IF NOT EXISTS " + sql.table("title_player") + " ("
                + "id " + pk + ","
                + "player_name VARCHAR(64) NOT NULL,"
                + "player_uuid VARCHAR(64),"
                + "title_id INT,"
                + "title_name VARCHAR(300) NOT NULL,"
                + "expiration_time VARCHAR(32) NOT NULL,"
                + "is_use INT NOT NULL DEFAULT 0,"
                + "is_use_buff INT NOT NULL DEFAULT 0,"
                + "is_use_particle INT NOT NULL DEFAULT 0,"
                + "UNIQUE(player_uuid,title_id)"
                + suffix);
    }
}
