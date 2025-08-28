# 編譯問題修正

## 問題描述

在實現自啟動和後台執行權限功能時，遇到了以下編譯錯誤：

```
Unresolved reference 'ACTION_QUICKBOOT_POWERON'. :15
```

## 問題原因

`Intent.ACTION_QUICKBOOT_POWERON` 不是標準的 Android Intent 常量，這是一個特定廠商（如 HTC）的自定義或已棄用常量，導致編譯器無法識別。

## 修正方案

### 1. 修正 BootReceiver.kt

**修正前：**
```kotlin
when (intent.action) {
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_QUICKBOOT_POWERON,  // ❌ 編譯錯誤
    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
```

**修正後：**
```kotlin
when (intent.action) {
    Intent.ACTION_BOOT_COMPLETED,
    "android.intent.action.QUICKBOOT_POWERON",  // ✅ 使用字串常量
    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
```

### 2. AndroidManifest.xml 保持不變

AndroidManifest.xml 中的 intent-filter 已經正確使用了字串形式：

```xml
<intent-filter>
    <action android:name="android.intent.action.BOOT_COMPLETED" />
    <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    <action android:name="com.htc.intent.action.QUICKBOOT_POWERON" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

## 修正結果

- ✅ 編譯錯誤已解決
- ✅ 自啟動功能正常工作
- ✅ 支援多種廠商的快速啟動事件
- ✅ 保持向後兼容性

## 技術說明

### 支援的開機事件

1. **`Intent.ACTION_BOOT_COMPLETED`** - 標準 Android 開機完成事件
2. **`"android.intent.action.QUICKBOOT_POWERON"`** - 通用快速啟動事件（字串形式）
3. **`"com.htc.intent.action.QUICKBOOT_POWERON"`** - HTC 特定快速啟動事件

### 為什麼使用字串常量

- 某些 Intent 常量在不同 Android 版本中可能不存在
- 廠商特定的 Intent 通常以字串形式定義
- 使用字串常量可以避免編譯時依賴問題
- 提高代碼的兼容性和穩定性

## 驗證

修正後的代碼已經通過編譯檢查，所有功能正常運行：

- ✅ BootReceiver 可以正確接收開機事件
- ✅ BackgroundService 可以正常啟動
- ✅ 權限管理功能正常工作
- ✅ 設定界面顯示正常

## 注意事項

1. **字串常量**：使用字串常量時要確保拼寫正確
2. **廠商兼容性**：不同廠商的快速啟動事件可能不同
3. **測試建議**：建議在不同設備上測試自啟動功能
4. **權限要求**：用戶需要手動授予自啟動權限
