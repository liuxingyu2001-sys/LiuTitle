# PlaceholderAPI

需要安装 PlaceholderAPI（软依赖），否则不会注册任何变量。

## 标识

- 主标识：`liutitle`
- 默认额外别名：`playerTitle`（兼容原插件写法，可在 `config.yml: placeholder-aliases` 调整）
- 别名在启动时注册；若该标识已被其他插件占用，会记录警告并跳过，不会导致启动失败。
- 修改 `placeholder-aliases` 需要**重启服务器**。

## 变量

| 变量 | 说明 |
|---|---|
| `%liutitle_use%` / `%liutitle_title%` | 当前展示称号（含色码）；未佩戴时返回空串 |
| `%liutitle_use_buff%` / `%liutitle_buff%` | 当前属性称号 |
| `%liutitle_use_particle%` | 恒为空（粒子已移除，仅保留兼容） |
| `%liutitle_count%` | 拥有的称号数量 |
| `%liutitle_id%` | 展示称号 id，未佩戴返回空串 |
| `%liutitle_buff_id%` | 属性称号 id，未佩戴返回空串 |
| `%liutitle_expire%` | 展示称号到期时间（`永久` 或 `yyyy-MM-dd HH:mm:ss`） |
| `%liutitle_buff_expire%` | 属性称号到期时间 |
| `%liutitle_list%` | 全部称号名（逗号分隔） |
| `%liutitle_has_<称号名>%` | `yes` / `no` |
| `%liutitle_expire_<id>%` | 指定称号的到期时间；未拥有返回空 |

把前缀 `liutitle` 换成 `playerTitle` 即为原插件写法。

## 示例

TAB：

```yaml
- '&b称号 &7| &6%playerTitle_use%'
```

计分板 / 聊天：

```text
%liutitle_count% 个称号，当前佩戴 %liutitle_use%
```

## 注意

- 未佩戴展示称号时 `%liutitle_use%` 返回空串；`default-title` 只用于首次进服自动发放称号。
- 变量读取的是本服内存缓存，跨服修改需要玩家重新登录或执行 `/liutitle admin reload` 后才会刷新。
- 与 `liutitle` / `playerTitle` 无关的变量（如 `%player_name%`）需要其他插件提供。
