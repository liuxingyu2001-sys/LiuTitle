# 命令与权限

主命令：`/liutitle`，别名 `/lt`。

## 玩家命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/liutitle` | 打开称号仓库 | `liutitle.use` |
| `/liutitle shop` | 打开称号商城 | `liutitle.shop` |
| `/liutitle list` | 在聊天栏列出自己拥有的称号 | `liutitle.use` |
| `/liutitle set <id/名称>` | 佩戴「展示」称号 | `liutitle.use` |
| `/liutitle buff <id/名称>` | 佩戴「属性」称号 | `liutitle.use` |
| `/liutitle clear` | 取消全部佩戴（展示 + 属性） | `liutitle.use` |
| `/liutitle help` | 查看帮助 | 无 |

说明：

- `set` / `buff` 的参数既可以是称号编号，也可以是称号显示名（去除色码后匹配）。
- `set` / `buff` 佩戴切换受 `toggle-cooldown`（秒）限制，`0` 表示关闭冷却；`clear` 不受该冷却限制。
- 商城购买、续期、条件校验见 [配置说明](configuration.md) 与 [存储与数据库](storage.md)。

## 管理命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/liutitle view <玩家>` | 查看并管理任意玩家的仓库 | `liutitle.admin.view` |
| `/liutitle admin shop` | 打开商城管理 GUI | `liutitle.admin.shop` |
| `/liutitle admin create <名称> [天数] [类型] [价格] [描述...]` | 创建称号 | `liutitle.admin.create` |
| `/liutitle admin delete <id>` | 删除称号及其属性、条件、玩家记录、权重 | `liutitle.admin.delete` |
| `/liutitle admin hide <id>` | 隐藏称号（商城不可见） | `liutitle.admin.modify` |
| `/liutitle admin show <id>` | 显示称号 | `liutitle.admin.modify` |
| `/liutitle admin weight <id> <权重>` | 设置显示权重，数值越大越靠前 | `liutitle.admin.modify` |
| `/liutitle admin give <玩家> <id> [天数]` | 发放或续期称号 | `liutitle.admin.give` |
| `/liutitle admin take <玩家> <id>` | 撤销玩家称号 | `liutitle.admin.give` |
| `/liutitle admin import <文件或目录>` | 导入 PlayerTitle 数据 | `liutitle.admin.import` |
| `/liutitle admin reload` | 重载配置、称号与在线玩家数据 | `liutitle.admin.reload` |

### 参数细节

**create**：`类型` 取 `not` / `vault` / `playerPoints` / `permission` / `activity`；省略天数时使用
`default-days`，`0` 表示永久。创建后会同时写入一条购买条件。

- `itemStack` 类型需要 `buy_data` 指定物品，命令行创建不会填写该字段；建议先导入或直接写库，再用管理 GUI 查看。
- `coin` / `playerCurrency` / `all` / `other` 仅用于兼容导入数据，不作为新称号的购买方式。

**give**：`天数` 省略或 `0` 时使用称号自身的默认天数；`day > 0` 的称号重复发放按续期处理
（从“现在”与原到期时间中较晚者往后加天数）。

**weight**：

- 权重为整数，可为负数；未设置的称号按 `0` 处理。
- 只影响 GUI 顺序，不影响价格、属性或有效期。
- 排序规则：权重降序 → 权重相同再按 `shop.sort`（商城）/ 称号 id（仓库）。
- 示例：

```text
/liutitle admin weight 12 100
/liutitle admin weight 12 0
/liutitle admin weight 12 -10
```

**delete**：命令删除是即时生效的；管理 GUI 中 `Shift + 右键` 会先弹出 10 秒确认提示，再执行删除。

**import**：详见 [导入 PlayerTitle 数据](import.md)。

## 权限节点

| 权限 | 默认 | 说明 |
|---|---|---|
| `liutitle.use` | true | 仓库、佩戴、取消佩戴、列出称号 |
| `liutitle.shop` | true | 打开商城并购买 |
| `liutitle.admin` | op | 管理总权限，包含以下全部子权限 |
| `liutitle.admin.create` | op | 创建称号 |
| `liutitle.admin.delete` | op | 删除称号 |
| `liutitle.admin.modify` | op | 隐藏/显示、设置权重 |
| `liutitle.admin.give` | op | 发放/撤销称号 |
| `liutitle.admin.import` | op | 导入数据 |
| `liutitle.admin.reload` | op | 重载 |
| `liutitle.admin.view` | op | 查看/管理任意玩家仓库 |
| `liutitle.admin.shop` | op | 管理商城 GUI |

## Tab 补全

- 一级子命令：`shop`、`list`、`set`、`buff`、`clear`、`help`；有 `liutitle.admin` 时额外提供 `admin`、`view`。
- `admin` 子命令：`shop`、`create`、`delete`、`hide`、`show`、`weight`、`give`、`take`、`import`、`reload`。
- `set` / `buff` / `delete` / `hide` / `show` / `weight` / `give` / `take` 会补全称号 id；`give` / `take` / `view`
  会补全在线玩家名。
