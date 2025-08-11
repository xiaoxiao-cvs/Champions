# 刷怪笼精英怪概率控制功能

## 功能说明

本功能为Champions mod新增了一个配置项，用于精确控制刷怪笼生成精英怪的概率。

## 配置项详情

### 配置文件位置
`config/champions-common.toml`

### 新增配置项
```toml
# 刷怪笼生成精英怪的概率 (0.0-1.0)
# 仅在 championSpawners = true 时生效
spawnerChampionChance = 0.5
```

### 配置说明
- **配置名称**: `spawnerChampionChance`
- **数据类型**: Double (双精度浮点数)
- **取值范围**: 0.0 - 1.0
- **默认值**: 0.5 (50%概率)
- **前置条件**: `championSpawners` 必须设置为 `true`

## 工作原理

1. 当 `championSpawners = false` 时，刷怪笼永远不会生成精英怪
2. 当 `championSpawners = true` 时：
   - 系统会根据 `spawnerChampionChance` 的值进行随机判断
   - 如果随机数小于设定值，则生成精英怪
   - 否则生成普通怪物

## 使用示例

### 示例1：高概率生成精英怪
```toml
championSpawners = true
spawnerChampionChance = 0.8  # 80%概率生成精英怪
```

### 示例2：低概率生成精英怪
```toml
championSpawners = true
spawnerChampionChance = 0.1  # 10%概率生成精英怪
```

### 示例3：完全禁用刷怪笼精英怪
```toml
championSpawners = false
# spawnerChampionChance 在此情况下不生效
```

### 示例4：刷怪笼必定生成精英怪
```toml
championSpawners = true
spawnerChampionChance = 1.0  # 100%概率生成精英怪
```

## 代码修改说明

### 修改的文件
1. `ChampionsConfig.java` - 添加新的配置项定义
2. `CapabilityEventHandler.java` - 修改刷怪笼生成逻辑

### 核心逻辑
```java
if (evt.getSpawnType() == MobSpawnType.SPAWNER) {
    if (!ChampionsConfig.championSpawners) {
        serverChampion.setRank(RankManager.getLowestRank());
    } else {
        // 使用概率配置来决定是否生成精英怪
        RandomSource random = evt.getLevel().getRandom();
        if (random.nextDouble() < ChampionsConfig.spawnerChampionChance) {
            ChampionBuilder.spawn(entity, evt.getLevel(), evt.getSpawnType());
        } else {
            serverChampion.setRank(RankManager.getLowestRank());
        }
    }
}
```

## 注意事项

1. 此功能仅影响刷怪笼生成的怪物，不影响自然生成的怪物
2. 配置修改后需要重启游戏才能生效
3. 概率计算基于游戏世界的随机数生成器，确保随机性
4. 当设置为0.0时，刷怪笼永远不会生成精英怪（等同于championSpawners=false）
5. 当设置为1.0时，刷怪笼总是生成精英怪（前提是championSpawners=true）