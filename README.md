# LiuTitle

称号插件（Paper / Leaf 1.21+）：玩家仓库、称号商城、显示权重、属性称号、有效期、管理 GUI、PlaceholderAPI、开发者 API，并支持直接导入 **PlayerTitle** 的 MySQL / SQLite 数据。

粒子效果、称号币、数量奖励、自定义称号卡、聊天接管均按需求移除，不在本插件范围内。

## 功能一览

- **仓库 / 商城**：分页 GUI，展示与属性两个佩戴位，永久或限时称号。
- **显示权重**：`/liutitle admin weight <id> <权重>`，商城、管理商城、仓库按权重降序排列。
- **购买条件**：Vault 金币、PlayerPoints 点券、物品、权限、活动称号，多个条件组合校验。
- **属性**：`potion_effect` 无限时长药水效果、`attribute` 临时属性修饰符，进服/换世界/重生自动恢复。
- **导入 PlayerTitle**：`.sql` / `.sql.gz` / `.db`，批量导入称号、属性、条件与未过期玩家数据。
- **存储隔离**：连接凭据读 LiuLib，库名/表前缀/SQLite 文件由本插件独立管理。
- **升级友好**：缺少的 `messages.yml` / `gui.yml` 默认键会自动补全，不覆盖已有值。

## 环境与依赖

| 项 | 要求 |
|---|---|
| 服务端 | Paper / Leaf 1.21+（`api-version: 1.21`，Folia 可用） |
| Java | 21+ |
| 硬依赖 | [LiuLib](../LiuLib) 1.0.5+（存储、文本、冷却、经济、调度、物品规格） |
| 软依赖 | PlaceholderAPI（变量）、Vault（金币）、PlayerPoints（点券） |

`plugin.yml` 通过服务端库加载器自带 HikariCP / mysql-connector-j / sqlite-jdbc，无需手动放驱动。

## 快速开始

1. 把 `Liu-LiuTitle-x.y.z.jar` 放入 `plugins/`。
2. 启动一次服务器，生成 `plugins/LiuTitle/{config.yml,gui.yml,messages.yml}`。
3. 按需修改配置后执行 `/liutitle admin reload`。

存储默认是 `auto`：LiuLib 的 MySQL 已启用就使用 MySQL（库名 `liutitle`），否则使用本插件 SQLite 文件
`plugins/LiuTitle/liutitle.db`。

## 文档

| 文档 | 内容 |
|---|---|
| [安装与升级](docs/installation.md) | 依赖、首次安装、升级、重载与重启边界 |
| [命令与权限](docs/commands.md) | 全部命令、权限节点、Tab 补全、示例 |
| [配置说明](docs/configuration.md) | `config.yml` / `gui.yml` / `messages.yml` 逐项说明与 GUI 占位符 |
| [存储与数据库](docs/storage.md) | 存储模式、库名/前缀、表结构、权重表、多服部署 |
| [导入 PlayerTitle](docs/import.md) | 支持格式、导入规则、重复导入与权重保留 |
| [PlaceholderAPI](docs/placeholders.md) | 变量列表、别名与原插件兼容 |
| [开发者 API](docs/api.md) | `TitleApi`、事件、线程约定 |
| [开发与验证](docs/development.md) | 构建、目录结构、开发约定、实测记录 |

## 已知边界

- 仅支持 Paper / Leaf 1.21+；Java 21 起步。
- 已移除粒子、称号币、数量奖励、自定义称号卡；相关表在导入时会被忽略。
- 多服共用 MySQL 时，每个子服各自缓存称号与玩家数据，跨服修改不会实时推送（重载或重新登录后可见）。

完整的设计取舍与验证记录见 [开发与验证](docs/development.md)。
