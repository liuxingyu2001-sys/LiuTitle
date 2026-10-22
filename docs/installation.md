# 安装与升级

## 1. 环境要求

| 项 | 要求 |
|---|---|
| 服务端 | Paper / Leaf 1.21+（`api-version: 1.21`，Folia 可用） |
| Java | 21 及以上 |
| 硬依赖 | LiuLib 1.0.5+ |
| 软依赖 | PlaceholderAPI、Vault、PlayerPoints（缺失时对应功能降级） |

`plugin.yml` 声明了以下库，由服务端库加载器在首次启动时下载：

- `com.zaxxer:HikariCP:5.1.0`
- `com.mysql:mysql-connector-j:8.0.33`
- `org.xerial:sqlite-jdbc:3.49.1.0`

没有外网时，需要提前把这些库放进服务端的 `libraries/` 缓存，否则启动阶段会下载失败。

## 2. 安装步骤

1. 确认 `plugins/` 中已有 **LiuLib**，并至少启动过一次。
2. 把 `Liu-LiuTitle-x.y.z.jar` 放入 `plugins/`。
3. 启动服务器，插件会生成：
   - `plugins/LiuTitle/config.yml`
   - `plugins/LiuTitle/gui.yml`
   - `plugins/LiuTitle/messages.yml`
4. 按 [配置说明](configuration.md) 修改配置。
5. 执行 `/liutitle admin reload` 让配置生效。

## 3. 存储选择

默认 `storage.driver: 'auto'`：

- LiuLib 的 `mysql.enabled: true` 且连接成功 → 使用 MySQL，库名默认 `liutitle`；
- 否则 → 使用本插件 SQLite 文件 `plugins/LiuTitle/liutitle.db`。

连接凭据只从 LiuLib 的 `mysql` 段读取，**不需要在 LiuTitle 里重复填账号密码**。库名、表前缀、连接池、
超时、SQLite 文件名由 LiuTitle 自己管理，详见 [存储与数据库](storage.md)。

数据库不可用时插件不会静默丢数据：启动日志会给出警告，并回退到 SQLite。

## 4. 重载与重启

`/liutitle admin reload` 会重新读取：

- `config.yml`（`Settings` 全量重建）
- `messages.yml`
- `gui.yml`
- 称号定义与在线玩家称号缓存

以下改动需要**重启服务器**才会生效：

| 改动 | 原因 |
|---|---|
| `storage.*`（driver / database / table-prefix / sqlite-file / pool-size / timeout / ssl） | 连接池与表前缀在 `onEnable` 时初始化 |
| `placeholder-aliases` | PlaceholderAPI expansion 在 `onEnable` 时注册 |
| 新增/移除软依赖插件（Vault、PlayerPoints、PlaceholderAPI） | 依赖探测在启动阶段完成 |

修改 `gui.yml`、`messages.yml` 后执行 reload 即可；`title_weight` 等数据改动实时生效。

## 5. 升级

1. 停服或直接替换 `plugins/LiuTitle-旧版本.jar` 为新 jar（不要同时保留两个版本）。
2. 启动服务器。
3. 插件会自动：
   - 创建缺失的表（包括 1.0.0 起新增的 `title_weight`）；
   - 把 jar 内新增的 `messages.yml` / `gui.yml` 默认键补进现有文件（已有值不覆盖）。
4. 建议升级前备份 `plugins/LiuTitle/` 与对应数据库。

从 1.0.0 之前的构建升级时，`title_weight` 表会在首次启动时自动创建，老称号权重按 `0` 处理。

## 6. 与 PlayerTitle 共存

- 两者命令不同（`/liutitle` 与 `/playertitle`），可以同时安装。
- LiuTitle 会尝试注册 `liutitle` 与 `placeholder-aliases` 中的标识（默认 `playerTitle`）；若标识已被
  PlayerTitle 占用，会记录警告并跳过，不会导致启动失败。
- 两个称号插件同时运行会重复处理佩戴/属性逻辑，**迁移完成后建议移除 PlayerTitle**。

## 7. 权限

默认权限见 [命令与权限](commands.md)。最小可用配置：

- 普通玩家：`liutitle.use`、`liutitle.shop`（默认均为 `true`）
- 管理员：`liutitle.admin`（默认 `op`，包含所有子权限）
