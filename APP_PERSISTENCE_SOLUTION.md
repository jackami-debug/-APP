# 應用持久性解決方案

## 問題描述

用戶反映即使設定了自啟動權限和後台執行權限，當手動滑掉應用時，監控功能仍然會失效。這是因為：

1. **手動關閉應用**：當用戶手動滑掉應用時，Android 系統會強制停止所有相關服務
2. **無障礙服務限制**：即使開啟了「自啟動不受限制」，手動關閉應用仍會終止服務
3. **後台服務被殺死**：BackgroundService 可能被系統回收

## 解決方案

### 1. 增強 BackgroundService

**修改內容：**
- 添加了定期刷新 WakeLock 的機制
- 每5分鐘重新獲取 WakeLock，確保服務持續運行
- 改進了服務狀態檢查邏輯

**技術實現：**
```kotlin
// 定期重新獲取 WakeLock
scope.launch {
    while (true) {
        delay(5 * 60 * 1000) // 每5分鐘重新獲取 WakeLock
        refreshWakeLock()
    }
}
```

### 2. 新增 AppRestartService

**功能說明：**
- 專門負責監控和重啟其他服務
- 每分鐘檢查一次所有關鍵服務的狀態
- 自動重啟被殺死的服務

**主要功能：**
1. **服務監控**：檢查 BackgroundService 是否運行
2. **自動重啟**：如果服務被殺死，自動重新啟動
3. **狀態檢查**：檢查 AppLockService 和無障礙服務狀態
4. **錯誤恢復**：重新初始化異常的服務

**技術實現：**
```kotlin
private suspend fun checkAndRestartServices() {
    // 檢查 BackgroundService 是否運行
    if (!isServiceRunning(BackgroundService::class.java)) {
        BackgroundService.startService(this)
    }
    
    // 檢查 AppLockService 是否正常
    val appLockService = AppLockService.getInstance(this)
    val status = appLockService.getServiceStatus()
    if (status == "未運行") {
        appLockService.resetLockingState()
    }
}
```

### 3. 修改啟動流程

**MainActivity.kt：**
- 同時啟動 BackgroundService 和 AppRestartService
- 確保兩個服務協同工作

**BootReceiver.kt：**
- 開機時同時啟動兩個服務
- 確保應用重啟後服務正常運行

## 使用說明

### 權限設定

1. **自啟動權限**：
   - 在設定頁面點擊「設定自啟動權限」
   - 在系統設定中開啟自啟動權限
   - 對於不同廠商，可能需要額外設定

2. **後台執行權限**：
   - 在設定頁面點擊「設定後台執行權限」
   - 在電池優化設定中選擇「忽略」此應用

3. **無障礙服務**：
   - 確保無障礙服務已開啟
   - 開啟「自啟動不受限制」選項

### 服務管理

1. **自動啟動**：
   - 應用啟動時會自動啟動所有必要服務
   - 開機時會自動啟動服務

2. **手動啟動**：
   - 在設定頁面可以手動啟動後台服務
   - 會同時啟動 BackgroundService 和 AppRestartService

3. **自動恢復**：
   - AppRestartService 會定期檢查服務狀態
   - 如果發現服務被殺死，會自動重啟

## 技術細節

### 服務架構

```
AppRestartService (監控服務)
    ↓
BackgroundService (後台服務)
    ↓
AppLockService (核心功能)
    ↓
AppLockAccessibilityService (無障礙服務)
```

### 持久性機制

1. **多層保護**：
   - AppRestartService 作為監控層
   - BackgroundService 作為執行層
   - 兩者相互備援

2. **定期檢查**：
   - AppRestartService 每分鐘檢查一次
   - BackgroundService 每30秒檢查一次
   - WakeLock 每5分鐘刷新一次

3. **自動恢復**：
   - 檢測到服務異常時自動重啟
   - 使用 START_STICKY 確保服務被殺死後自動重啟

## 注意事項

### 系統限制

1. **Android 系統限制**：
   - 某些系統可能會限制後台應用運行
   - 手動關閉應用仍可能終止服務
   - 需要用戶配合設定權限

2. **廠商差異**：
   - 不同廠商的權限管理機制不同
   - 可能需要額外的權限設定
   - 建議在不同設備上測試

### 用戶操作

1. **避免手動關閉**：
   - 建議不要手動滑掉應用
   - 使用返回鍵退出應用
   - 讓應用在背景中運行

2. **定期檢查**：
   - 定期檢查無障礙服務狀態
   - 確保所有權限都已正確設定
   - 如果發現問題，重新啟動服務

## 測試建議

1. **功能測試**：
   - 測試手動關閉應用後服務是否自動重啟
   - 測試開機後服務是否正常啟動
   - 測試長時間運行是否穩定

2. **權限測試**：
   - 測試不同權限設定下的行為
   - 測試不同廠商設備的兼容性
   - 測試系統更新後的穩定性

3. **性能測試**：
   - 監控服務的資源使用情況
   - 確保不會過度消耗電池
   - 檢查記憶體使用是否合理

## 後續改進

1. **通知提醒**：
   - 當服務被殺死時發送通知提醒用戶
   - 提供快速重啟服務的選項

2. **智能檢測**：
   - 更智能地檢測服務狀態
   - 減少不必要的重啟操作

3. **用戶引導**：
   - 提供更詳細的權限設定引導
   - 添加故障排除指南
