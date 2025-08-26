# 密碼驗證功能實現

## 功能概述

在程式啟動時加入密碼驗證畫面，要求使用者輸入6位數字密碼來保護應用程式。

## 實現的檔案

### 1. 密碼畫面 (PasswordScreen.kt)
- 位置：`app/src/main/java/com/example/appenergytracker/ui/screens/PasswordScreen.kt`
- 功能：
  - 類似 iPhone 密碼輸入介面的設計
  - 支援設定密碼和驗證密碼兩種模式
  - 包含數字鍵盤和密碼點顯示
  - 漸層背景設計（藍、綠、橙色）

### 2. 密碼管理器 (PasswordManager.kt)
- 位置：`app/src/main/java/com/example/appenergytracker/utils/PasswordManager.kt`
- 功能：
  - 密碼的儲存和讀取
  - 密碼驗證
  - 使用 SharedPreferences 進行本地儲存

### 3. 導航更新 (NavGraph.kt)
- 位置：`app/src/main/java/com/example/appenergytracker/ui/navigation/NavGraph.kt`
- 更新：
  - 新增密碼畫面路由
  - 將密碼畫面設為起始畫面

### 4. 測試檔案 (PasswordManagerTest.kt)
- 位置：`app/src/test/java/com/example/appenergytracker/PasswordManagerTest.kt`
- 功能：驗證密碼管理器的各項功能

## 使用流程

### 首次使用
1. 程式啟動時顯示密碼設定畫面
2. 使用者輸入6位數字密碼
3. 再次輸入確認密碼
4. 密碼儲存後進入主畫面

### 後續使用
1. 程式啟動時顯示密碼驗證畫面
2. 使用者輸入正確的6位數字密碼
3. 驗證成功後進入主畫面

## 畫面特色

- **視覺設計**：採用漸層背景，類似 iPhone 鎖定畫面
- **密碼顯示**：使用圓點顯示密碼長度，不顯示實際數字
- **數字鍵盤**：自定義數字鍵盤，包含刪除功能
- **響應式設計**：支援不同螢幕尺寸

## 安全性

- 密碼使用 SharedPreferences 本地儲存
- 支援密碼清除功能
- 可擴展為加密儲存（目前使用簡化版本）

## 測試

執行測試命令：
```bash
./gradlew test
```

測試涵蓋：
- 密碼設定功能
- 密碼驗證功能
- 密碼清除功能
- 初始狀態檢查

## 未來改進

1. 加入密碼加密儲存
2. 支援指紋辨識
3. 密碼重設功能
4. 密碼強度檢查
5. 登入失敗次數限制
