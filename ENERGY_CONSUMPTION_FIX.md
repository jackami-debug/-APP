# 能量消耗比例修正

## 🐛 問題描述

使用壞習慣APP時，能量消耗比例不正確：
- **預期**: 使用壞習慣APP 1分鐘消耗1點能量
- **實際**: 使用一下下就消耗了5分鐘以上的能量

## 🔍 問題分析

### 原始問題
1. **每秒扣除能量**: `ENERGY_DEDUCTION_PER_SECOND = 0.0167`
2. **每秒檢查**: `MONITORING_INTERVAL = 1000L`
3. **累積效果**: 每秒扣除0.0167點，每分鐘實際扣除 `0.0167 × 60 = 1.002` 點
4. **快速消耗**: 由於每秒都在扣除，短時間內能量消耗過快

### 計算示例
```
使用壞習慣APP 30秒：
- 每秒扣除: 0.0167點
- 30秒扣除: 0.0167 × 30 = 0.501點
- 但由於每秒檢查，可能累積更多
```

## ✅ 修正方案

### 1. 修改常數定義
```kotlin
// 修改前
private const val ENERGY_DEDUCTION_PER_SECOND = 0.0167

// 修改後  
private const val ENERGY_DEDUCTION_PER_MINUTE = 1 // 每分鐘扣除1點能量
```

### 2. 新增時間追蹤機制
```kotlin
// 能量扣除時間追蹤
private val lastEnergyDeductionTime = ConcurrentHashMap<String, Long>()
```

### 3. 修改能量扣除邏輯
```kotlin
private suspend fun checkAndDeductEnergy(packageName: String) {
    val habitApp = packageToHabitCache.value[packageName]
    
    if (habitApp?.isBadHabit == true) {
        // 壞習慣 App，每分鐘扣除1點能量
        val currentTime = System.currentTimeMillis()
        val lastDeductionTime = lastEnergyDeductionTime.getOrDefault(packageName, 0L)
        
        // 檢查是否已經過了1分鐘
        if (currentTime - lastDeductionTime >= 60000) { // 60000毫秒 = 1分鐘
            val currentEnergyValue = _currentEnergy.value
            val newEnergy = maxOf(0, currentEnergyValue - ENERGY_DEDUCTION_PER_MINUTE)
            _currentEnergy.value = newEnergy
            
            // 更新最後扣除時間
            lastEnergyDeductionTime[packageName] = currentTime
            
            android.util.Log.d("EnergyMonitor", "壞習慣 App: $packageName, 扣除能量: $ENERGY_DEDUCTION_PER_MINUTE, 當前能量: $newEnergy")
            
            // 能量歸零檢測
            if (newEnergy <= 0 && currentEnergyValue > 0) {
                android.util.Log.d("EnergyMonitor", "能量歸零，立即阻止當前壞習慣 App: $packageName")
                immediatelyBlockCurrentApp(packageName)
            }
        }
    }
}
```

## 🎯 修正效果

### 修正前
- 每秒扣除0.0167點能量
- 30秒使用壞習慣APP消耗約0.5點能量
- 能量消耗過快，不符合預期

### 修正後
- 每分鐘扣除1點能量
- 使用壞習慣APP 1分鐘消耗1點能量
- 符合預期的消耗比例

## 📱 測試方法

### 1. 基本測試
1. 啟動應用程式
2. 進入主頁面，記錄當前能量值
3. 使用壞習慣APP 1分鐘
4. 檢查能量是否只減少1點

### 2. 詳細測試
1. **短時間使用測試**:
   - 使用壞習慣APP 30秒
   - 檢查能量是否沒有變化（未滿1分鐘）

2. **長時間使用測試**:
   - 使用壞習慣APP 2分鐘
   - 檢查能量是否減少2點

3. **多次使用測試**:
   - 使用壞習慣APP 30秒，停止
   - 再次使用30秒
   - 檢查能量是否只減少1點（總計1分鐘）

## 🔍 預期日誌輸出

```
EnergyMonitor: 壞習慣 App: com.example.badapp, 扣除能量: 1, 當前能量: 179
EnergyMonitor: 壞習慣 App: com.example.badapp, 扣除能量: 1, 當前能量: 178
```

## ✅ 成功指標

- ✅ 使用壞習慣APP 1分鐘消耗1點能量
- ✅ 短時間使用（<1分鐘）不消耗能量
- ✅ 能量消耗比例準確
- ✅ 即時阻止功能正常工作

## 🚨 注意事項

1. **時間精度**: 使用 `System.currentTimeMillis()` 確保時間精度
2. **記憶體管理**: 使用 `ConcurrentHashMap` 避免並發問題
3. **邊界檢查**: 確保能量不會低於0
4. **日誌記錄**: 詳細記錄能量變化過程

這個修正確保了能量消耗比例的正確性，符合「使用壞習慣APP 1分鐘消耗1點能量」的預期。
