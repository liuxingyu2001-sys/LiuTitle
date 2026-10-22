# 配置说明

插件配置位于 `plugins/LiuTitle/`：

- `config.yml`：运行参数、存储、商城排序等
- `gui.yml`：仓库/商城/管理界面布局
- `messages.yml`：提示文本

修改后执行 `/liutitle admin reload` 生效；`storage.*` 与 `placeholder-aliases` 需要重启，见
[安装与升级](installation.md)。

## 1. config.yml

```yaml
# 玩家首次进服且没有任何称号时，自动发放并佩戴该名称的称号（需已存在同名称号）
default-title: "&7萌新"

# 预留：称号名前后缀（当前版本未参与发放）
prefix-brackets: ""
suffix-brackets: ""

# 预留：占位符前后缀（当前版本未参与输出）
display-prefix: ""
display-suffix: ""

# 切换称号冷却（秒），0 = 关闭
toggle-cooldown: 1

buff:
  enabled: true
  disabled-worlds: []   # 这些世界不恢复属性称号

auto-use: false         # 获得新称号后自动佩戴
login-delay: 1          # 登录后延迟多少秒恢复属性（等待其它数据加载）
default-days: 0         # 创建称号的默认有效期（天），0 = 永久

placeholder-aliases: ['playerTitle']   # 额外注册的 PAPI 标识

shop:
  sort: 'id'            # id / name / price；权重始终优先
  show-hidden: false    # 商城是否显示被隐藏的称号

import:
  enabled: true         # 是否允许 /liutitle admin import

# 存储见 docs/storage.md
storage:
  driver: 'auto'
  database: 'liutitle'
  table-prefix: 'liutitle_'
  sqlite-file: 'liutitle.db'
  auto-create-database: true
  pool-size: 0
  connection-timeout-ms: 0
  use-ssl: ''
```

逐项说明：

| 键 | 说明 |
|---|---|
| `default-title` | 玩家首次进服且拥有 0 个称号时，按名称匹配并自动发放/佩戴；留空关闭；找不到同名称号则忽略 |
| `prefix-brackets` / `suffix-brackets` | 预留字段，当前版本未参与发放逻辑 |
| `display-prefix` / `display-suffix` | 预留字段，当前版本未参与占位符输出 |
| `toggle-cooldown` | `set` / `buff` 佩戴切换的最小间隔（秒），`0` 关闭 |
| `buff.enabled` | 关闭后不再施加属性效果，但已有数据保留 |
| `buff.disabled-worlds` | 世界名不区分大小写；在这些世界不恢复属性 |
| `auto-use` | 发放/购买新称号后自动佩戴展示位 |
| `login-delay` | 玩家进服后延迟恢复属性，避免与登录插件冲突 |
| `default-days` | `create` 未写天数时使用；`0` = 永久 |
| `placeholder-aliases` | 额外 PAPI 标识（默认兼容原插件 `playerTitle`）；重启生效；被占用时跳过 |
| `shop.sort` | `id` / `name` / `price`；**权重优先**，同权重才按此排序 |
| `shop.show-hidden` | `true` 时商城也显示隐藏称号（通常只由管理 GUI 查看） |
| `import.enabled` | 预留开关，当前版本尚未接入导入命令校验 |

## 2. gui.yml

内置菜单：`shop`（商城）、`warehouse`（仓库）、`adminShop`（管理商城）、`adminOwners`（称号拥有者）、
`adminPlayer`（管理玩家仓库）。

每个菜单的通用结构：

```yaml
shop:
  title: "&f[&e称号商城&f]"
  size: 54
  slots: [10, 11, ...]     # 内容槽位，按顺序填充
  page-prev: 45
  page-next: 53
  close: 49
  info: 4                  # 信息物品槽位，-1 表示不显示
  item:
    item: 'name_tag'       # LiuLib 物品规格
    enchant: true
    name: '${titleName}'
    lore:
      - '&8▪ &7价格 &f${price}'
  buttons:
    buy: '&a点击购买'
    owned: '&7已拥有'
  info-item:
    item: 'paper'
    name: '&e称号商城'
    lore: ['&7共 ${shopCount} 个称号', '&7第 ${page}/${pages} 页']
  close-item: { item: 'barrier', name: '&c关闭' }
  prev-item: { item: 'paper', name: '&a上一页' }
  next-item: { item: 'paper', name: '&a下一页' }
```

- `item` 字段支持 LiuLib 完整物品规格（`DIAMOND`、`ce:ns:id`、`base64:...`、`lib:<id>` 等）。
- `name` / `lore` 使用 `&` 色码、`&#RRGGBB` 或 MiniMessage；`${key}` 为模板占位符。
- 物品默认关闭斜体（沿用 LiuLib 解析行为），不需要额外配置。
- 菜单加载时会自动补全 jar 内新增的**缺失键**，已有值不覆盖；升级后新增的菜单项会自动出现。

### GUI 占位符

| 菜单 | 可用占位符 |
|---|---|
| 商城 | `titleName` `id` `day` `description` `buff` `price` `buyType` `position` `weight` `buyButton` |
| 仓库 | `titleName` `titleId` `id` `expire` `position` `description` `status` |
| 管理商城 | `id` `titleName` `hidden` `day` `price` `description` `buyers` `weight` |
| 拥有者/管理玩家 | `player` `expire` `status`（管理玩家沿用仓库占位符） |
| 公共信息物品 | `count` `shopCount` `page` `pages` `player` |
| 翻页按钮 | `page` |

`buyButton` / `hidden` / `status` 的文案从对应菜单的 `buttons` 段读取；缺失时使用内置兜底文本。

`weight` 为称号当前显示权重（未设置显示 `0`），可用于在 lore 中展示排序值。

## 3. messages.yml

```yaml
prefix: '&8[&eLiuTitle&8] &r'
general: { ... }     # 通用提示（权限、用法、未知命令）
title: { ... }       # 称号不存在等
shop: { ... }        # 购买成功/失败、条件不足
warehouse: { ... }   # 仓库空、未拥有
admin: { ... }       # 创建/删除/隐藏/权重/发放/导入/重载
receive: { ... }     # 获得称号提示
help:
  command: [ ... ]   # /liutitle help
  admin: [ ... ]     # 管理帮助
```

- 文本格式：`&` 色码、`&#RRGGBB`、MiniMessage 均可。
- 所有消息前会自动拼接 `prefix`。
- 占位符形如 `{id}`、`{name}`、`{weight}`、`{player}`、`{path}` 等，按命令实际参数填充。
- 插件启动时会从 jar 内默认文件补全**缺失的消息键**（不覆盖已修改的值），因此升级后新增消息不会显示成键名。
- 修改后 `/liutitle admin reload` 立即生效。
- `help.command` / `help.admin` 是列表，若要调整帮助内容请直接编辑；列表不会自动合并新增行，升级后请对照默认文件手动补充。
