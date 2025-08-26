# 即時監控機制說明

## 🎯 問題解決

### 原有問題
- 只有在 App 切換時才會檢查和阻止壞習慣 App
- 用戶可以一直使用同一個壞習慣 App，不會被阻止
- 能量歸零後需要切換 App 才會觸發阻止機制

### 解決方案
- **即時監控**: 每1秒檢查一次當前 App 使用情況
- **立即阻止**: 能量歸零的瞬間立即阻止當前正在使用的壞習慣 App
- **持續追蹤**: 追蹤當前正在使用的 App，不需要等待切換

## 🔧 核心改進

### 1. EnergyMonitorService 改進

#### 監控頻率提升
```kotlin
// 從3秒改為1秒，提高即時性
private const val MONITORING_INTERVAL = 1000L // 每1秒檢查一次
private const val ENERGY_DEDUCTION_PER_SECOND = 0.0167 // 每1秒扣除0.0167點
```

#### 當前 App 追蹤
```kotlin
// 新增當前正在使用的 App 追蹤
private var currentForegroundApp: String? = null

// 在 checkCurrentAppUsage() 中持續追蹤
if (detectedApp != null && detectedApp != currentForegroundApp) {
    // App 切換了，記錄之前的 App 使用時間
    currentForegroundApp?.let { previousApp ->
        recordAppSwitch(previousApp)
    }
    currentForegroundApp = detectedApp
}
```

#### 立即阻止機制
```kotlin
// 能量歸零檢測 - 關鍵改進！
if (newEnergyInt <= 0 && currentEnergyValue > 0) {
    android.util.Log.d("EnergyMonitor", "能量歸零，立即阻止當前壞習慣 App: $packageName")
    // 立即阻止當前正在使用的壞習慣 App
    immediatelyBlockCurrentApp(packageName)
}
```

### 2. AppLockAccessibilityService 改進

#### 新增立即阻止方法
```kotlin
// 立即阻止當前正在使用的壞習慣 App（關鍵新功能）
fun immediatelyBlockCurrentApp(packageName: String) {
    // 檢查是否為壞習慣 App
    val isBadHabitApp = isBadHabitApp(packageName)
    
    if (isBadHabitApp) {
        // 立即顯示阻止訊息
        showImmediateBlockMessage()
        
        // 延遲一小段時間後回到主畫面，確保訊息顯示完成
        Handler(Looper.getMainLooper()).postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, BLOCK_DELAY)
    }
}
```

#### 專用阻止訊息
```kotlin
private fun showImmediateBlockMessage() {
    Toast.makeText(
        this,
        "⚡ 能量歸零！\n立即阻止壞習慣 App 使用",
        Toast.LENGTH_LONG
    ).show()
}
```

## 🚀 工作流程

### 1. 即時監控流程
```
每1秒執行一次：
├── 獲取當前使用事件
├── 更新當前正在使用的 App
├── 如果是壞習慣 App：
│   ├── 扣除能量 (0.0167點/秒)
│   ├── 檢查能量是否歸零
│   └── 如果歸零 → 立即阻止
└── 記錄使用時間
```

### 2. 立即阻止流程
```
能量歸零檢測：
├── 檢查當前 App 是否為壞習慣
├── 顯示立即阻止訊息
├── 延遲500ms確保訊息顯示
└── 執行回到主畫面操作
```

### 3. 狀態同步
```
各服務狀態同步：
├── EnergyMonitorService: 追蹤當前 App 和能量
├── AppLockService: 管理鎖定狀態
└── AppLockAccessibilityService: 執行阻止操作
```

## 📱 測試方法

### 1. 使用測試畫面
- 進入主畫面，點擊「🧪 即時監控測試」
- 在設定中將某個 App 設為壞習慣
- 使用「模擬消耗能量」快速測試
- 觀察能量歸零時的立即阻止效果

### 2. 實際測試步驟
1. **啟用無障礙服務**
   - 進入設定 → 無障礙 → App Energy Tracker
   - 啟用服務

2. **設定壞習慣 App**
   - 進入設定畫面
   - 選擇一個 App 設為壞習慣

3. **測試即時監控**
   - 打開壞習慣 App
   - 使用測試功能快速消耗能量
   - 觀察能量歸零時的立即阻止效果

### 3. 預期效果
- 能量歸零的瞬間，壞習慣 App 會被立即阻止
- 顯示「⚡ 能量歸零！立即阻止壞習慣 App 使用」訊息
- 自動回到主畫面
- 不需要切換 App 就能觸發阻止

## ⚡ 效能優化

### 1. 監控頻率平衡
- **1秒間隔**: 平衡即時性和效能
- **快取機制**: 減少資料庫查詢
- **狀態追蹤**: 避免重複檢查

### 2. 記憶體管理
- **ConcurrentHashMap**: 線程安全的使用時間追蹤
- **快取有效期**: 30秒快取，平衡效能和一致性
- **資源清理**: 服務銷毀時清理資源

### 3. 錯誤處理
- **異常捕獲**: 完整的錯誤處理機制
- **重試機制**: 服務未運行時的重試邏輯
- **日誌記錄**: 詳細的調試日誌

## 🎯 使用場景

### 1. 日常使用
- 用戶使用壞習慣 App 時，能量會持續消耗
- 能量歸零時立即被阻止，無法繼續使用
- 需要充電或使用好習慣 App 來恢復能量

### 2. 開發測試
- 使用測試畫面快速驗證功能
- 模擬各種使用場景
- 調試和優化監控邏輯

### 3. 故障排除
- 檢查無障礙服務是否正常運行
- 驗證習慣 App 配置是否正確
- 確認能量計算是否準確

## 🔮 後續改進

### 1. 功能增強
- 新增使用時間限制功能
- 支援自定義能量計算規則
- 新增使用統計和分析

### 2. 使用者體驗
- 改善阻止訊息的顯示效果
- 新增震動和聲音提醒
- 支援自定義阻止延遲時間

### 3. 穩定性提升
- 新增服務健康檢查機制
- 改善服務重啟邏輯
- 增強資料一致性檢查

這個即時監控機制現在能夠真正實現「能量歸零的瞬間立即阻止壞習慣 App」的功能，解決了原有設計的根本問題。
