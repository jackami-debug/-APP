# 充電功能調試指南

## 🔍 問題分析

充電活動後能量沒有增加，可能的原因：

1. **狀態同步衝突**: `EnergyViewModel` 和 `EnergyMonitorService` 之間的狀態更新衝突
2. **資料流問題**: 多個資料源同時更新能量狀態
3. **時序問題**: 更新順序不正確

## ✅ 新的修復策略

### 1. 直接更新 EnergyMonitorService

**修改前**:
```kotlin
// 在 EnergyViewModel 中直接更新 _currentEnergyMinutes
_currentEnergyMinutes.value = newEnergy
// 然後重新載入 EnergyMonitorService
energyMonitorService.loadCurrentEnergy()
```

**修改後**:
```kotlin
// 直接更新 EnergyMonitorService 的能量狀態
energyMonitorService.setCurrentEnergy(newEnergy)
```

### 2. 新增 setCurrentEnergy 方法

```kotlin
// 在 EnergyMonitorService 中
fun setCurrentEnergy(energy: Int) {
    val clampedEnergy = maxOf(0, minOf(MAX_ENERGY, energy))
    _currentEnergy.value = clampedEnergy
    android.util.Log.d("EnergyMonitor", "設置當前能量為: $clampedEnergy")
}
```

## 🧪 調試步驟

### 1. 檢查日誌輸出

運行應用程式後，查看 Logcat 中的日誌：

```
EnergyViewModel: 已記錄充電活動: 免費充能, 時長: 1分鐘, 比例: 1.0
EnergyViewModel: 直接更新 EnergyMonitorService 能量: 50 + 1 = 51
EnergyMonitor: 設置當前能量為: 51
```

### 2. 測試流程

1. **啟動應用程式**
2. **進入主頁面**，記錄當前能量值
3. **進入充電活動頁面**
4. **點擊「獲得1分鐘能量」**
5. **立即返回主頁面**，檢查能量變化

### 3. 預期結果

- ✅ 日誌顯示正確的能量計算
- ✅ 主頁面能量立即增加1
- ✅ 沒有狀態衝突的錯誤訊息

## 🔧 如果問題仍然存在

### 檢查點 1: 資料庫插入
```kotlin
// 檢查 ChargeLog 是否正確插入資料庫
appDao.insertChargeLog(log)
```

### 檢查點 2: 能量計算
```kotlin
// 檢查能量計算是否正確
val energyToAdd = (durationMinutes * ratio).toInt()
val newEnergy = min(_maxEnergyMinutes.value, currentEnergy + energyToAdd)
```

### 檢查點 3: 狀態更新
```kotlin
// 檢查 EnergyMonitorService 狀態是否更新
energyMonitorService.setCurrentEnergy(newEnergy)
```

### 檢查點 4: UI 響應
```kotlin
// 檢查主頁面是否正確監聽能量變化
val currentEnergy by energyViewModel.currentEnergyMinutes.collectAsState()
```

## 🚨 常見問題

### 問題 1: 能量沒有變化
**可能原因**: `EnergyMonitorService` 沒有正確更新
**解決方案**: 檢查 `setCurrentEnergy` 方法是否被調用

### 問題 2: 能量變化但立即恢復
**可能原因**: 其他服務重新載入覆蓋了更新
**解決方案**: 確保更新順序正確

### 問題 3: 能量計算錯誤
**可能原因**: 計算邏輯有問題
**解決方案**: 檢查 `durationMinutes * ratio` 的計算

## 📱 測試命令

在測試畫面中可以使用以下按鈕進行調試：

1. **測試充電功能 (+10能量)**: 測試基本的充電功能
2. **重置能量為最大值**: 重置能量到180
3. **模擬消耗能量**: 快速消耗能量進行測試

## 🎯 成功指標

- ✅ 點擊充電按鈕後，主頁面能量立即增加
- ✅ 日誌顯示正確的能量變化過程
- ✅ 沒有錯誤或警告訊息
- ✅ 多次充電累積正確

如果問題仍然存在，請提供具體的日誌輸出，以便進一步診斷。
