# AppEnergyTracker 排除修改

## 🎯 問題描述

程式本身（AppEnergyTracker）被設定為壞習慣APP，這不合理。需要將AppEnergyTracker從習慣APP設定中排除。

## ✅ 修改內容

### 1. 重新掃描時排除 AppEnergyTracker

**修改位置**: `rescanAndReload` 函數
```kotlin
// 修改前
val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    .filter { ai -> pm.getLaunchIntentForPackage(ai.packageName) != null }

// 修改後
val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    .filter { ai -> 
        pm.getLaunchIntentForPackage(ai.packageName) != null && // 只留可啟動
        ai.packageName != "com.example.appenergytracker" // 排除 AppEnergyTracker 本身
    }
```

### 2. 好習慣APP分頁排除 AppEnergyTracker

**修改位置**: `GoodHabitAppSection` 函數

#### 2.1 已加入的好習慣APP顯示
```kotlin
// 修改前
val selectedGoodApps = goodHabitApps.filter { app -> app.isGoodHabit }

// 修改後
val selectedGoodApps = goodHabitApps.filter { app -> 
    app.isGoodHabit && app.packageName != "com.example.appenergytracker" 
}
```

#### 2.2 APP列表過濾
```kotlin
// 修改前
val filteredApps = remember(goodHabitApps, searchQuery) {
    goodHabitApps.filter { app ->
        app.name.contains(searchQuery.text, ignoreCase = true) ||
                app.packageName.contains(searchQuery.text, ignoreCase = true)
    }.sortedBy { app -> app.name }
}

// 修改後
val filteredApps = remember(goodHabitApps, searchQuery) {
    goodHabitApps.filter { app ->
        app.packageName != "com.example.appenergytracker" && // 排除 AppEnergyTracker
        (app.name.contains(searchQuery.text, ignoreCase = true) ||
                app.packageName.contains(searchQuery.text, ignoreCase = true))
    }.sortedBy { app -> app.name }
}
```

### 3. 壞習慣APP分頁排除 AppEnergyTracker

**修改位置**: `BadHabitAppSection` 函數

#### 3.1 已加入的壞習慣APP顯示
```kotlin
// 修改前
val selectedBadApps = goodHabitApps.filter { app -> app.isBadHabit }

// 修改後
val selectedBadApps = goodHabitApps.filter { app -> 
    app.isBadHabit && app.packageName != "com.example.appenergytracker" 
}
```

#### 3.2 APP列表過濾
```kotlin
// 修改前
val filteredApps = remember(goodHabitApps, searchQuery) {
    goodHabitApps.filter { app ->
        app.name.contains(searchQuery.text, ignoreCase = true) ||
                app.packageName.contains(searchQuery.text, ignoreCase = true)
    }.sortedBy { app -> app.name }
}

// 修改後
val filteredApps = remember(goodHabitApps, searchQuery) {
    goodHabitApps.filter { app ->
        app.packageName != "com.example.appenergytracker" && // 排除 AppEnergyTracker
        (app.name.contains(searchQuery.text, ignoreCase = true) ||
                app.packageName.contains(searchQuery.text, ignoreCase = true))
    }.sortedBy { app -> app.name }
}
```

## 🎯 修改效果

### 1. 重新掃描時
- ✅ AppEnergyTracker 不會被加入資料庫
- ✅ 避免程式本身被誤設為壞習慣APP

### 2. 好習慣APP分頁
- ✅ AppEnergyTracker 不會出現在"已加入的好習慣 App"區域
- ✅ AppEnergyTracker 不會出現在APP列表中
- ✅ 無法將 AppEnergyTracker 設為好習慣APP

### 3. 壞習慣APP分頁
- ✅ AppEnergyTracker 不會出現在"已加入的壞習慣 App"區域
- ✅ AppEnergyTracker 不會出現在APP列表中
- ✅ 無法將 AppEnergyTracker 設為壞習慣APP

## 🔧 技術細節

### 包名識別
```kotlin
ai.packageName != "com.example.appenergytracker"
```

### 過濾邏輯
1. **重新掃描時過濾**：在掃描已安裝APP時直接排除
2. **顯示時過濾**：在顯示已加入的APP時過濾
3. **列表時過濾**：在顯示APP列表時過濾

### 多層防護
- 即使 AppEnergyTracker 已經在資料庫中，也不會顯示在設定頁面
- 確保用戶無法將程式本身設為壞習慣APP
- 保持資料庫的整潔性

## 📱 測試方法

### 1. 重新掃描測試
1. 進入設定頁面
2. 點擊"重新掃描"按鈕
3. 確認 AppEnergyTracker 不會出現在任何列表中

### 2. 搜尋測試
1. 在搜尋框中輸入"AppEnergyTracker"或"能量"
2. 確認搜尋結果中不會出現 AppEnergyTracker

### 3. 已加入APP顯示測試
1. 檢查"已加入的好習慣 App"區域
2. 檢查"已加入的壞習慣 App"區域
3. 確認 AppEnergyTracker 不會出現在這些區域

## ✅ 預期結果

- ✅ AppEnergyTracker 完全從習慣APP設定中消失
- ✅ 用戶無法將程式本身設為壞習慣APP
- ✅ 避免程式自己監控自己的不合理情況
- ✅ 保持設定頁面的邏輯一致性

這個修改確保了 AppEnergyTracker 不會被誤設為壞習慣APP，避免了程式自己監控自己的不合理情況。
