# 开发者 API

调用方在 `plugin.yml` 中声明依赖：

```yaml
depend: [LiuTitle]
```

## 获取 API

```java
TitleApi api = Bukkit.getServicesManager().load(TitleApi.class);
if (api == null) {
    api = TitleApi.get();   // 兜底：直接从 LiuTitle 插件实例取
}
```

插件启用时通过 Bukkit `ServicesManager` 注册，停用后自动失效。

## 方法

### 称号定义

| 方法 | 返回 |
|---|---|
| `titleCount()` | 称号总数 |
| `hasTitle(int id)` | 是否存在 |
| `titleIds()` | 全部称号 id |
| `titleName(int id)` | 名称（含色码），不存在返回 `null` |
| `titleDescription(int id)` | 描述，不存在返回 `null` |
| `titleDays(int id)` | 默认有效期（天），`0` = 永久 |

### 玩家数据

| 方法 | 返回 |
|---|---|
| `ownedCount(UUID uuid)` | 拥有数量 |
| `ownedTitleIds(UUID uuid)` | 拥有的称号 id 列表（未加载时自动查库） |
| `ownedTitles(UUID uuid)` | 拥有的称号名列表 |
| `owns(UUID uuid, int titleId)` | 是否拥有 |
| `expireAt(UUID uuid, int titleId)` | `永久` 或 `yyyy-MM-dd HH:mm:ss`，未拥有返回 `null` |
| `equippedTitle(UUID uuid)` | 展示称号名，未佩戴返回 `null` |
| `equippedTitleId(UUID uuid)` | 展示称号 id，未佩戴返回 `-1` |
| `equippedBuffTitle(UUID uuid)` | 属性称号名 |
| `equippedBuffTitleId(UUID uuid)` | 属性称号 id，未佩戴返回 `-1` |

### 操作

| 方法 | 说明 |
|---|---|
| `equip(Player player, int titleId, boolean buff)` | 佩戴；`buff=false` 展示位，`true` 属性位 |
| `unequip(Player player, int titleId, boolean buff)` | 取消佩戴 |
| `grant(UUID uuid, String playerName, int titleId, int days)` | 发放/续期；`days <= 0` 永久，玩家可离线 |
| `take(UUID uuid, int titleId)` | 撤销 |

示例：

```java
TitleApi api = TitleApi.get();
if (api != null) {
    api.grant(player.getUniqueId(), player.getName(), 12, 7);
    api.equip(player, 12, false);
    int count = api.ownedCount(player.getUniqueId());
}
```

## 事件

包：`com.liuxingyu.liutitle.event`

| 事件 | 可取消 | 说明 |
|---|---|---|
| `TitleEquipEvent` | 是 | 佩戴前触发，可阻止 |
| `TitleUnequipEvent` | 否 | 取消佩戴后触发 |
| `TitlePurchaseEvent` | 是 | 购买/续期前触发，可阻止 |
| `TitleGrantEvent` | 否 | 发放称号（玩家可离线） |
| `TitleExpireEvent` | 否 | 在线玩家称号过期 |

```java
@EventHandler
public void onEquip(TitleEquipEvent event) {
    if (event.title().id() == 1 && !event.player().hasPermission("vip.title")) {
        event.setCancelled(true);
    }
}
```

## 线程约定

- `equip` / `unequip` 会修改玩家状态与属性，必须在**主线程/玩家所在线程**调用。
- `grant` / `take` 会写数据库；命令与 GUI 中均在主线程调用，第三方插件建议同样在主线程调用，避免与缓存竞争。
- 事件同样在主线程触发。

## 权重

显示权重目前只通过命令 `/liutitle admin weight <id> <权重>` 维护，`TitleApi` 未暴露读写方法；如需在
第三方插件中调整，可执行该命令或直接操作 `title_weight` 表后执行 `/liutitle admin reload`。
