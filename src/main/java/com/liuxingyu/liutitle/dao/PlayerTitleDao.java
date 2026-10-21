package com.liuxingyu.liutitle.dao;

import com.liuxingyu.liutitle.model.PlayerTitle;
import com.liuxingyu.liutitle.storage.Sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/** 玩家称号（title_player）。 */
public final class PlayerTitleDao {

    private final Sql sql;

    public PlayerTitleDao(Sql sql) {
        this.sql = sql;
    }

    private String table() {
        return sql.table("title_player");
    }

    public List<PlayerTitle> byPlayer(UUID uuid) {
        return sql.query("SELECT * FROM " + table() + " WHERE player_uuid = ? ORDER BY title_id",
                this::map, uuid.toString());
    }

    public List<PlayerTitle> byPlayerName(String name) {
        return sql.query("SELECT * FROM " + table() + " WHERE player_name = ? ORDER BY title_id",
                this::map, name);
    }

    public List<PlayerTitle> byTitle(int titleId) {
        return sql.query("SELECT * FROM " + table() + " WHERE title_id = ? ORDER BY player_name",
                this::map, titleId);
    }

    public PlayerTitle find(UUID uuid, int titleId) {
        return sql.queryOne("SELECT * FROM " + table() + " WHERE player_uuid = ? AND title_id = ?",
                this::map, uuid.toString(), titleId);
    }

    public int count(UUID uuid) {
        Integer count = sql.queryOne("SELECT COUNT(*) FROM " + table() + " WHERE player_uuid = ?",
                rs -> rs.getInt(1), uuid.toString());
        return count == null ? 0 : count;
    }

    public int insert(PlayerTitle title) {
        int id = sql.insert("INSERT INTO " + table()
                        + " (player_name, player_uuid, title_id, title_name, expiration_time, is_use, is_use_buff, is_use_particle)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                title.playerName(), title.uuid().toString(), title.titleId(), title.titleName(),
                PlayerTitle.formatTime(title.expiration()), title.use() ? 1 : 0, title.useBuff() ? 1 : 0);
        if (id > 0) {
            title.id(id);
        }
        return id;
    }

    /** 导入用：带主键写入。 */
    public void insertWithId(PlayerTitle title) {
        sql.update("INSERT INTO " + table()
                        + " (id, player_name, player_uuid, title_id, title_name, expiration_time, is_use, is_use_buff, is_use_particle)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                title.id(), title.playerName(), title.uuid().toString(), title.titleId(), title.titleName(),
                PlayerTitle.formatTime(title.expiration()), title.use() ? 1 : 0, title.useBuff() ? 1 : 0);
    }

    public void updateExpiration(int id, String expiration) {
        sql.update("UPDATE " + table() + " SET expiration_time = ? WHERE id = ?", expiration, id);
    }

    public void updateUse(int id, boolean value) {
        sql.update("UPDATE " + table() + " SET is_use = ? WHERE id = ?", value ? 1 : 0, id);
    }

    public void updateUseBuff(int id, boolean value) {
        sql.update("UPDATE " + table() + " SET is_use_buff = ? WHERE id = ?", value ? 1 : 0, id);
    }

    public void clearUse(UUID uuid, String column) {
        String col = "use_buff".equals(column) ? "is_use_buff" : "is_use";
        sql.update("UPDATE " + table() + " SET " + col + " = 0 WHERE player_uuid = ?", uuid.toString());
    }

    public void delete(UUID uuid, int titleId) {
        sql.update("DELETE FROM " + table() + " WHERE player_uuid = ? AND title_id = ?", uuid.toString(), titleId);
    }

    public void deleteById(int id) {
        sql.update("DELETE FROM " + table() + " WHERE id = ?", id);
    }

    public int deleteExpired(String nowIso) {
        return sql.update("DELETE FROM " + table() + " WHERE expiration_time < ? AND expiration_time <> ?",
                nowIso, PlayerTitle.formatTime(com.liuxingyu.liutitle.model.Title.PERMANENT));
    }

    public int countAll() {
        Integer count = sql.queryOne("SELECT COUNT(*) FROM " + table(), rs -> rs.getInt(1));
        return count == null ? 0 : count;
    }

    private PlayerTitle map(ResultSet rs) throws SQLException {
        String uuidRaw = rs.getString("player_uuid");
        UUID uuid;
        try {
            uuid = uuidRaw == null ? new UUID(0, 0) : UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException e) {
            uuid = new UUID(0, 0);
        }
        return new PlayerTitle(
                rs.getInt("id"),
                rs.getString("player_name"),
                uuid,
                rs.getInt("title_id"),
                rs.getString("title_name"),
                PlayerTitle.parseTime(rs.getString("expiration_time")),
                rs.getInt("is_use") != 0,
                rs.getInt("is_use_buff") != 0);
    }
}
