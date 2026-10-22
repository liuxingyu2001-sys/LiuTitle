# 开发与验证

## 构建

```bash
cd /home/liu/plugins/LiuTitle
mvn -o clean package
# 产物：target/Liu-LiuTitle-<version>.jar
```

- Java 21，Paper API `1.21.11-R0.1-SNAPSHOT`。
- 依赖全部 `provided`，jar 内不 shade LiuLib / 驱动；驱动由 `plugin.yml: libraries` 提供。
- 修改资源文件后确认 jar 内含 `plugin.yml`、`config.yml`、`gui.yml`、`messages.yml`。

## 目录结构

```text
src/main/java/com/liuxingyu/liutitle/
├── LiuTitle.java          # 启动、装配、reload、PAPI 注册
├── api/                   # TitleApi
├── command/               # /liutitle 与 Tab 补全
├── config/                # Settings、GuiConfig
├── dao/                   # TitleDao、PlayerTitleDao
├── event/                 # 5 个 Bukkit 事件
├── gui/                   # MenuManager、菜单 Holder
├── listener/              # 进服/换世界/重生恢复属性
├── model/                 # Title、PlayerTitle、Buff、Require 等
├── papi/                  # TitleExpansion
├── service/               # TitleService、PlayerTitleService、PurchaseService、ImportService、BuffService
├── storage/               # Sql 抽象、MySQL/SQLite 实现、Schema
└── util/                  # Msg、Defaults、文本/时间工具
src/main/resources/
├── plugin.yml
├── config.yml
├── gui.yml
└── messages.yml
```

## 开发约定

- 玩家可见文本统一走 `LiuLib.text()`，支持 `&`、`&#RRGGBB`、MiniMessage。
- GUI 物品统一走 LiuLib 物品规格；`gui.yml` 的 `${key}` 由 `MenuManager` 填充。
- 表名统一 `sql.table("...")`，不要写死前缀；SQL 方言差异放在 `storage` 层处理。
- 新增配置键必须有代码内默认值；新增消息键要同时写入 `messages.yml`。
- 新增 `messages.yml` / `gui.yml` 的顶层键会被 `Defaults.merge` 自动补进旧数据文件；列表（如帮助文本）不会逐行合并。
- 不要恢复 particles / coin / reward / custom 相关代码或配置。
- 称号删除、隐藏、权重修改后注意刷新缓存：`TitleService` 即时更新，玩家数据用 `playerTitleService.reloadOnline()`。

## 实测记录

测试环境：Leaf 1.21.11、Java 25、LiuLib 1.0.5。

- 启动：`LiuLib/SQLite` 后端，`plugins/LiuTitle/liutitle.db`，表前缀 `liutitle_`。
- 导入 MySQL 备份：称号 129、属性 76、条件 128、未过期玩家称号 2526、跳过过期 456；重复导入结果一致。
- 导入 SQLite：称号 64、属性 48、条件 64、未过期玩家称号 1953、跳过过期 588。
- 命令：help / list / set / buff / clear / view / admin create / delete / hide / show / weight / give / take / import / reload。
- GUI：商城分页、仓库、管理商城（左键隐藏切换）、玩家仓库查看与移除；`weight` 在管理商城 lore 中正确显示。
- 购买：Vault 金币、PlayerPoints 点券、itemStack 物品、activity 拒绝、已拥有永久称号拒绝。
- 续期：1 天称号二次购买，到期时间从 `2026-10-23` 延到 `2026-10-24`。
- 属性：`potion_effect` 施加 6 个无限时长效果，`/liutitle clear` 后全部移除。
- 过期：把到期时间改到昨天后重载，记录被删除并提示“已过期并被移除”。
- PAPI：`%liutitle_use%`、`%playerTitle_use%`、`%liutitle_use_buff%`、`%playerTitle_use_buff%` 均返回正确称号。
- 缓存：删除称号后在线玩家仓库不再显示该称号。
- 权重：设置 `4=200`、`25=150`、`2=100` 后商城/管理商城顺序为 4 → 25 → 2；`-10` 会排到 0 权重之后；
  非法权重提示用法，不存在的 ID 提示称号不存在。
- 权重持久化：重新导入 PlayerTitle 数据后权重保持不变；删除称号时权重行一并清理。
- MySQL 分支：临时 MariaDB（127.0.0.1:3307）下自动创建 `liutitle` 库并启动成功；日常测试服仍用 SQLite。

## 已知限制

- 仅支持 Paper / Leaf 1.21+。
- 多服共用 MySQL 时没有跨服实时推送，依赖重载/重新登录刷新缓存。
- 导入为“按 id 覆盖”，不会清理目标库中备份里不存在的称号。
- 显示权重没有 API，只能通过命令或直接改表后 reload。
