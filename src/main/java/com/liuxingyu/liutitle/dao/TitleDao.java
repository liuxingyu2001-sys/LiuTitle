package com.liuxingyu.liutitle.dao;

import com.liuxingyu.liutitle.model.Buff;
import com.liuxingyu.liutitle.model.BuffType;
import com.liuxingyu.liutitle.model.BuyType;
import com.liuxingyu.liutitle.model.Require;
import com.liuxingyu.liutitle.model.Title;
import com.liuxingyu.liutitle.model.TitlePosition;
import com.liuxingyu.liutitle.storage.Sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 称号定义（title_list / title_buff / title_require）。 */
public final class TitleDao {

    private final Sql sql;

    public TitleDao(Sql sql) {
        this.sql = sql;
    }

    private String list() {
        return sql.table("title_list");
    }

    private String buffs() {
        return sql.table("title_buff");
    }

    private String requires() {
        return sql.table("title_require");
    }

    public Map<Integer, Title> loadAll() {
        Map<Integer, Title> titles = new LinkedHashMap<>();
        for (Title title : sql.query("SELECT * FROM " + list() + " ORDER BY id", this::mapTitle)) {
            titles.put(title.id(), title);
        }
        for (Buff buff : sql.query("SELECT * FROM " + buffs() + " ORDER BY id", this::mapBuff)) {
            Title title = titles.get(buff.titleId());
            if (title != null) {
                title.buffs().add(buff);
            }
        }
        for (Require require : sql.query("SELECT * FROM " + requires() + " ORDER BY id", this::mapRequire)) {
            Title title = titles.get(require.titleId());
            if (title != null) {
                title.requires().add(require);
            }
        }
        return titles;
    }

    public Title find(int id) {
        Title title = sql.queryOne("SELECT * FROM " + list() + " WHERE id = ?", this::mapTitle, id);
        if (title == null) {
            return null;
        }
        for (Buff buff : sql.query("SELECT * FROM " + buffs() + " WHERE title_id = ? ORDER BY id", this::mapBuff, id)) {
            title.buffs().add(buff);
        }
        for (Require require : sql.query("SELECT * FROM " + requires() + " WHERE title_id = ? ORDER BY id", this::mapRequire, id)) {
            title.requires().add(require);
        }
        return title;
    }

    public int count() {
        Integer count = sql.queryOne("SELECT COUNT(*) FROM " + list(), rs -> rs.getInt(1));
        return count == null ? 0 : count;
    }

    public int insert(Title title) {
        int id = sql.insert("INSERT INTO " + list()
                        + " (title_name, description, day, is_hide, position, buy_type, amount, item_stack)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                title.name(), title.description(), title.day(), title.hidden() ? 1 : 0,
                title.position().id(), title.legacyBuyType().id(), title.legacyAmount(), title.itemStack());
        if (id > 0) {
            title.id(id);
        }
        return id;
    }

    public void insertWithId(Title title) {
        sql.update("INSERT INTO " + list()
                        + " (id, title_name, description, day, is_hide, position, buy_type, amount, item_stack)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                title.id(), title.name(), title.description(), title.day(), title.hidden() ? 1 : 0,
                title.position().id(), title.legacyBuyType().id(), title.legacyAmount(), title.itemStack());
    }

    public void updateTitle(Title title) {
        sql.update("UPDATE " + list()
                        + " SET title_name = ?, description = ?, day = ?, is_hide = ?, position = ? WHERE id = ?",
                title.name(), title.description(), title.day(), title.hidden() ? 1 : 0,
                title.position().id(), title.id());
    }

    public void setHidden(int id, boolean hidden) {
        sql.update("UPDATE " + list() + " SET is_hide = ? WHERE id = ?", hidden ? 1 : 0, id);
    }

    public void updateDescription(int id, String description) {
        sql.update("UPDATE " + list() + " SET description = ? WHERE id = ?", description, id);
    }

    public void updateName(int id, String name) {
        sql.update("UPDATE " + list() + " SET title_name = ? WHERE id = ?", name, id);
    }

    public void delete(int id) {
        sql.update("DELETE FROM " + list() + " WHERE id = ?", id);
        sql.update("DELETE FROM " + buffs() + " WHERE title_id = ?", id);
        sql.update("DELETE FROM " + requires() + " WHERE title_id = ?", id);
        sql.update("DELETE FROM " + sql.table("title_player") + " WHERE title_id = ?", id);
    }

    public void insertBuff(Buff buff) {
        sql.update("INSERT INTO " + buffs() + " (title_id, buff_type, buff_content, description, apply_type)"
                        + " VALUES (?, ?, ?, ?, ?)",
                buff.titleId(), buff.type().id(), buff.content(), buff.description(), buff.applyType());
    }

    public void insertRequire(Require require) {
        sql.update("INSERT INTO " + requires() + " (title_id, buy_type, amount, buy_data, create_time)"
                        + " VALUES (?, ?, ?, ?, ?)",
                require.titleId(), require.buyType().id(), require.amount(), require.buyData(), require.createTime());
    }

    public void deleteBuffs(int titleId) {
        sql.update("DELETE FROM " + buffs() + " WHERE title_id = ?", titleId);
    }

    public void deleteRequires(int titleId) {
        sql.update("DELETE FROM " + requires() + " WHERE title_id = ?", titleId);
    }

    public int countOwners(int titleId) {
        Integer count = sql.queryOne("SELECT COUNT(*) FROM " + sql.table("title_player") + " WHERE title_id = ?",
                rs -> rs.getInt(1), titleId);
        return count == null ? 0 : count;
    }

    private Title mapTitle(ResultSet rs) throws SQLException {
        Title title = new Title();
        title.id(rs.getInt("id"));
        title.name(rs.getString("title_name"));
        title.description(rs.getString("description"));
        title.day(rs.getInt("day"));
        title.hidden(rs.getInt("is_hide") != 0);
        title.position(TitlePosition.byId(rs.getInt("position")));
        title.legacyBuyType(BuyType.parse(rs.getString("buy_type")));
        int amount = rs.getInt("amount");
        title.legacyAmount(rs.wasNull() ? null : amount);
        title.itemStack(rs.getString("item_stack"));
        return title;
    }

    private Buff mapBuff(ResultSet rs) throws SQLException {
        return new Buff(
                rs.getInt("id"),
                rs.getInt("title_id"),
                BuffType.parse(rs.getString("buff_type")),
                rs.getString("buff_content"),
                rs.getString("description"),
                rs.getString("apply_type"));
    }

    private Require mapRequire(ResultSet rs) throws SQLException {
        return new Require(
                rs.getInt("id"),
                rs.getInt("title_id"),
                BuyType.parse(rs.getString("buy_type")),
                rs.getInt("amount"),
                rs.getString("buy_data"),
                rs.getString("create_time"));
    }
}
