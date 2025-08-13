# 歷史紀錄頁面優化

## 🎯 優化目標

優化"歷史紀錄"頁面的"今日概覽"區域，提供更詳細的統計信息，並修正淨能量的計算邏輯。

## ✅ 修改內容

### 1. 今日概覽區域重新設計

**修改前**：
- 總使用時間
- 總充能時間  
- 淨能量（計算錯誤）

**修改後**：
- **總使用時間**：所有APP的使用時間總和
- **總充能時間**：所有充能活動獲得的能量總和
- **壞習慣APP使用時間**：僅壞習慣APP的使用時間
- **淨能量**：總充能時間 - 壞習慣APP使用時間

### 2. 淨能量計算邏輯修正

**修改前**：
```kotlin
val netEnergy = totalChargeMinutes - totalUsageMinutes
```

**修改後**：
```kotlin
// 壞習慣APP使用時間
val badHabitUsageMinutes = badHabitUsage.sumOf { log -> log.usageMinutes }

// 淨能量計算：總充能時間 - 壞習慣APP使用時間
val netEnergy = totalChargeMinutes - badHabitUsageMinutes
```

### 3. UI 佈局優化

**兩行佈局設計**：
```kotlin
// 第一行：總使用時間和總充能時間
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    StatItem(
        icon = Icons.Default.Timer,
        label = "總使用時間",
        value = "${totalUsageMinutes}分鐘",
        color = MaterialTheme.colorScheme.error
    )
    StatItem(
        icon = Icons.Default.BatteryChargingFull,
        label = "總充能時間",
        value = "${totalChargeMinutes}分鐘",
        color = MaterialTheme.colorScheme.primary
    )
}

// 第二行：壞習慣APP使用時間和淨能量
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    StatItem(
        icon = Icons.Default.ThumbDown,
        label = "壞習慣APP使用時間",
        value = "${badHabitUsageMinutes}分鐘",
        color = Color(0xFFF44336)
    )
    StatItem(
        icon = Icons.Default.TrendingUp,
        label = "淨能量",
        value = "${netEnergy}分鐘",
        color = if (netEnergy >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    )
}
```

## 🎨 視覺設計

### 顏色方案
- **總使用時間**：錯誤色（紅色）
- **總充能時間**：主色（藍色）
- **壞習慣APP使用時間**：紅色（#F44336）
- **淨能量**：綠色（正值）/ 紅色（負值）

### 圖標選擇
- **總使用時間**：Timer（時鐘）
- **總充能時間**：BatteryChargingFull（充電）
- **壞習慣APP使用時間**：ThumbDown（拇指向下）
- **淨能量**：TrendingUp（趨勢上升）

## 📊 數據邏輯

### 統計數據計算
```kotlin
// 基礎統計
val totalUsageMinutes = usage.sumOf { log -> log.usageMinutes }
val totalChargeMinutes = charges.sumOf { charge -> (charge.durationMinutes * charge.ratio).toInt() }

// 分類統計
val goodHabitUsage = usage.filter { log ->
    goodHabitApps.any { habitApp -> habitApp.packageName == log.packageName && habitApp.isGoodHabit }
}
val badHabitUsage = usage.filter { log ->
    goodHabitApps.any { habitApp -> habitApp.packageName == log.packageName && habitApp.isBadHabit }
}

// 壞習慣APP使用時間
val badHabitUsageMinutes = badHabitUsage.sumOf { log -> log.usageMinutes }

// 淨能量計算
val netEnergy = totalChargeMinutes - badHabitUsageMinutes
```

## 🎯 優化效果

### 1. 更清晰的數據展示
- 分離總使用時間和壞習慣APP使用時間
- 清楚顯示充能活動的效果
- 準確的淨能量計算

### 2. 更好的用戶體驗
- 兩行佈局，避免擁擠
- 直觀的顏色區分
- 清晰的圖標識別

### 3. 正確的計算邏輯
- 淨能量 = 總充能時間 - 壞習慣APP使用時間
- 不包含好習慣APP和一般APP的使用時間
- 更符合實際的能量管理邏輯

## 📱 使用場景

### 1. 日常監控
用戶可以清楚看到：
- 今天總共使用了多長時間
- 通過充能活動獲得了多少能量
- 壞習慣APP消耗了多少時間
- 最終的淨能量狀態

### 2. 習慣改善
- 通過壞習慣APP使用時間，了解需要改善的習慣
- 通過淨能量，了解整體的能量平衡
- 通過充能時間，了解積極活動的效果

### 3. 數據分析
- 提供更詳細的數據分類
- 支持更深入的使用模式分析
- 便於制定改善計劃

這個優化讓歷史紀錄頁面更加實用和信息豐富，幫助用戶更好地了解自己的使用習慣和能量管理狀況。
