# 壞習慣APP過濾邏輯確認

## ✅ 當前實現已正確

歷史紀錄頁面中的"壞習慣APP使用時間"計算邏輯已經正確實現，符合您的要求。

## 🔍 過濾邏輯分析

### 1. 數據來源
```kotlin
// 從資料庫載入所有習慣APP設定
val goodHabitApps by viewModel.goodHabitApps.collectAsState()

// 從資料庫載入今日使用記錄
var usage by remember { mutableStateOf(emptyList<AppUsageLog>()) }
```

### 2. 壞習慣APP過濾邏輯
```kotlin
val badHabitUsage = usage.filter { log ->
    goodHabitApps.any { habitApp -> 
        habitApp.packageName == log.packageName && habitApp.isBadHabit 
    }
}
```

### 3. 壞習慣APP使用時間計算
```kotlin
val badHabitUsageMinutes = badHabitUsage.sumOf { log -> log.usageMinutes }
```

## 🎯 邏輯確認

### 步驟1：檢查使用記錄
- 遍歷所有今日的APP使用記錄 (`usage`)
- 每個記錄包含：`packageName` 和 `usageMinutes`

### 步驟2：匹配壞習慣APP
- 對於每個使用記錄，在 `goodHabitApps` 中查找對應的APP
- 檢查條件：
  - `habitApp.packageName == log.packageName`（包名匹配）
  - `habitApp.isBadHabit == true`（標記為壞習慣）

### 步驟3：計算總時間
- 只統計符合條件的壞習慣APP的使用時間
- 忽略好習慣APP和未設定的APP

## 📱 設定頁面對應

### 壞習慣APP設定
在設定頁面的"壞習慣 App"分頁中：
```kotlin
// 當用戶開啟某個APP的開關時
viewModel.updateApp(app.copy(
    isGoodHabit = if (isOn) false else app.isGoodHabit,
    isBadHabit = isOn  // 設為壞習慣
))
```

### 顯示已加入的壞習慣APP
```kotlin
val selectedBadApps = goodHabitApps.filter { app -> app.isBadHabit }
```

## ✅ 驗證方法

### 1. 設定壞習慣APP
1. 進入設定頁面
2. 選擇"壞習慣 App"分頁
3. 開啟某些APP的開關，將其設為壞習慣
4. 查看"已加入的壞習慣 App"區域，確認APP已顯示

### 2. 使用壞習慣APP
1. 使用已設定的壞習慣APP一段時間
2. 進入歷史紀錄頁面
3. 檢查"壞習慣APP使用時間"是否正確顯示

### 3. 驗證計算邏輯
- 壞習慣APP使用時間 = 所有 `isBadHabit = true` 的APP使用時間總和
- 淨能量 = 總充能時間 - 壞習慣APP使用時間

## 🎯 預期結果

### 正確的過濾結果
- ✅ 只統計在設定頁面中已加入的壞習慣APP
- ✅ 忽略好習慣APP的使用時間
- ✅ 忽略未設定的APP使用時間
- ✅ 淨能量計算準確

### 示例場景
假設今日使用記錄：
- 抖音（壞習慣APP）：30分鐘
- 微信（壞習慣APP）：20分鐘  
- 讀書APP（好習慣APP）：15分鐘
- 系統設定（未設定）：10分鐘

計算結果：
- 總使用時間：75分鐘
- 壞習慣APP使用時間：50分鐘（抖音30 + 微信20）
- 淨能量：總充能時間 - 50分鐘

## 🔧 技術細節

### 資料庫結構
```kotlin
@Entity(tableName = "GoodHabitApp")
data class GoodHabitApp(
    @PrimaryKey val packageName: String,
    val name: String,
    val ratio: Float,
    val isGoodHabit: Boolean,
    val isBadHabit: Boolean
)
```

### 過濾邏輯特點
1. **精確匹配**：使用 `packageName` 進行精確匹配
2. **狀態檢查**：明確檢查 `isBadHabit = true`
3. **排除其他**：自動排除好習慣APP和未設定APP
4. **實時更新**：基於 `StateFlow` 實時反映設定變化

這個實現確保了"壞習慣APP使用時間"只統計在設定頁面中明確設定的壞習慣APP，完全符合您的要求。
