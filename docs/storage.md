# 存储与数据库

## 1. 设计原则

- **连接凭据只维护一份**：读取 LiuLib 的 `mysql` 段（host / port / username / password / use-ssl）。
- **数据按插件隔离**：LiuTitle 自己决定库名、表前缀、连接池、超时和 SQLite 文件名，不往 LiuLib 的库或
  其他插件的库里写表。
- **失败不静默**：MySQL 不可用时回退 SQLite，并在日志中给出原因。

这样可以避免两类常见问题：改一次数据库密码要同步修改每个插件；多个插件共用一个库、靠前缀“软隔离”导致的误伤。

## 2. 存储模式

```yaml
storage:
  driver: 'auto'              # auto | mysql | sqlite
  database: 'liutitle'        # MySQL 库名（本插件专用）
  table-prefix: 'liutitle_'   # 表前缀
  sqlite-file: 'liutitle.db'  # 相对 plugins/LiuTitle/
  auto-create-database: true
  pool-size: 0
  connection-timeout-ms: 0
  use-ssl: ''
```

| 模式 | 行为 |
|---|---|
| `auto` | LiuLib `mysql.enabled: true` 且初始化成功 → MySQL；否则 SQLite |
| `mysql` | 强制 MySQL；LiuLib 未启用或连接失败时记录警告并回退 SQLite |
| `sqlite` | 强制使用 `plugins/LiuTitle/<sqlite-file>` |

补充规则：

- `database` 留空时会回退到 LiuLib 的库（**不推荐**，会破坏插件隔离）。
- `auto-create-database: true` 时启动执行 `CREATE DATABASE IF NOT EXISTS`；账号无权限时只警告，不会中断启动。
- `pool-size` / `connection-timeout-ms` 为 `0` 时跟随 LiuLib 的 `mysql.pool-size` / `mysql.connection-timeout-ms`。
- `use-ssl` 留空跟随 LiuLib；可填 `true` / `false`。
- `table-prefix` 会做安全过滤；一旦有数据，**不要随意修改**，否则相当于换了一套空表。

MySQL 示例（LiuLib 侧只需配置一次）：

```yaml
# plugins/LiuLib/config.yml
mysql:
  enabled: true
  host: 127.0.0.1
  port: 3306
  username: liulib
  password: '******'
```

## 3. 表结构

表前缀默认 `liutitle_`，默认库 `liutitle`（SQLite 为 `plugins/LiuTitle/liutitle.db`）。

| 表 | 关键字段 | 说明 |
|---|---|---|
| `title_list` | `id`, `title_name`, `description`, `day`, `is_hide`, `position`, `buy_type`, `amount`, `item_stack` | 称号定义，字段与 PlayerTitle 对齐 |
| `title_buff` | `id`, `title_id`, `buff_type`, `buff_content`, `description`, `apply_type` | 属性/效果，`apply_type` 区分佩戴方式 |
| `title_require` | `id`, `title_id`, `buy_type`, `amount`, `buy_data`, `create_time` | 购买条件，可多条组合 |
| `title_player` | `id`, `player_uuid`, `player_name`, `title_id`, `title_name`, `expiration_time`, `is_use`, `is_use_buff`, `is_use_particle` | 玩家拥有/佩戴记录，`(player_uuid,title_id)` 唯一 |
| `title_weight` | `title_id`（主键）, `weight` | 显示权重，仅本插件使用 |

说明：

- 前四张表与 PlayerTitle 兼容，可直接导入其数据；`title_weight` 是新增表，PlayerTitle 没有对应概念。
- 权重单独存表，重新导入 PlayerTitle 数据（`title_list` 按 id 覆盖）**不会重置**已设置的权重。
- 删除称号时，`title_buff` / `title_require` / `title_player` / `title_weight` 中对应行会一起删除。
- 表结构在启动时自动创建（`CREATE TABLE IF NOT EXISTS`），升级不需要手动执行 SQL。
- 旧库不会自动新增 `title_weight` 以外的列；本项目目前没有“给已有表加列”的迁移需求。

## 4. 显示权重

`title_weight` 由命令维护：

```text
/liutitle admin weight <id> <权重>
```

- 权重越大越靠前；未设置按 `0`；可为负数。
- 商城与管理商城：权重降序 → `shop.sort`（id/name/price）→ id。
- 玩家仓库：权重降序 → 称号 id。
- 只影响展示顺序，不参与购买、属性或过期计算。
- 玩家数据的导入不会覆盖权重；删除称号会清理权重行。

## 5. 多服部署

- 需要共享数据时，让所有子服使用**同一个 MySQL 地址、同一个 `storage.database`、同一个 `storage.table-prefix`**。
- 各子服在内存中缓存称号定义和在线玩家数据；跨服修改不会主动推送：
  - 称号定义：执行 `/liutitle admin reload` 后刷新；
  - 玩家佩戴/拥有：玩家重新登录或重载后刷新。
- 每个子服的 SQLite 文件互相独立；不要用同步盘共享同一个 `.db` 文件。

## 6. 备份与恢复

- MySQL：备份 `liutitle` 库即可（`mysqldump liutitle > liutitle.sql`）。
- SQLite：停服后复制 `plugins/LiuTitle/liutitle.db`（以及同目录的 `-wal` / `-shm`，如有）。
- 恢复后无需额外操作，启动即加载；权重表包含在备份中。

## 7. 实测状态

- SQLite：测试服默认路径，129+ 称号导入、商城/仓库/属性/权重全流程通过。
- MySQL：使用临时 MariaDB（127.0.0.1:3307）验证，启用 LiuLib MySQL 后自动创建 `liutitle` 库并正常启动。
- 两种后端使用同一套 DAO 与 SQL，只有少量方言差异（自增主键、`INSERT ... ON DUPLICATE KEY`）。
