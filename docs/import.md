# 导入 PlayerTitle 数据

## 命令

```text
/liutitle admin import <文件或目录>
```

- 权限：`liutitle.admin.import`
- 相对路径按 `plugins/LiuTitle/` 解析，绝对路径直接使用。
- 需要 `import.enabled: true`（默认开启）。

## 支持的格式

| 格式 | 说明 |
|---|---|
| `*.sql` | PlayerTitle 的 MySQL 备份，解析 `INSERT INTO \`table\` VALUES (...)` |
| `*.sql.gz` | 同上，gzip 压缩；与同名 `.sql` 同时存在时只导入一次（优先 `.sql`） |
| `*.db` | PlayerTitle 的 SQLite 数据库文件 |

目录会递归查找以上文件。单次导入多个文件时，统计值累加。

## 导入规则

- `title_list`：按 `id` 覆盖（`REPLACE` / `INSERT OR REPLACE`）。
- `title_buff`：按 `id` 覆盖，`apply_type` 缺失时默认 `wear`。
- `title_require`：按 `id` 覆盖。
- `title_player`：只导入**未过期**的行；过期行计入“跳过”数量。
- 忽略 `title_coin`、`title_reward`、`title_reward_log`、`title_particle`（对应功能已移除）。
- 时间字段支持 epoch 毫秒/秒与 `yyyy-MM-dd HH:mm:ss`，统一转成 `yyyy-MM-dd HH:mm:ss` 存储。
- 导入完成后自动重载称号缓存，并刷新在线玩家仓库/属性。

## 权重保留

`title_weight` 不参与导入：

- 已设置的权重在重新导入后保持不变；
- 导入新增的称号权重默认 `0`；
- 导入文件中被删掉但本地仍存在的称号权重行不影响使用（对应称号不存在时不会显示）。

## 注意事项

- 导入是**按 id 覆盖**：如果目标库里已有同 id 但内容不同的称号，会被备份数据覆盖。
- 导入不会删除目标库里“备份中没有”的称号或玩家记录；需要清理时用 `/liutitle admin delete`。
- 已过期的玩家记录不会被导入，但目标库里此前已存在的过期记录也不会被自动删除。
- 导入大文件在异步线程执行，完成后在聊天栏输出统计。

## 实测结果

测试服使用 `/home/liu/plugins/playertitle`：

- MySQL `.sql`：称号 129、属性 76、条件 128、未过期玩家称号 2526、跳过过期 456。
- 重复导入同一目录：计数一致（`.sql` 与 `.sql.gz` 去重生效）。
- SQLite `.db`（拷自 99 服 `PlayerTitle.db`）：称号 64、属性 48、条件 64、未过期玩家称号 1953、跳过过期 588。
- 先设置权重，再重新导入：`title_weight` 行保持不变。
