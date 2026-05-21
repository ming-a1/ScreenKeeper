# 屏幕常亮 (Screen Keeper)

一款低功耗的屏幕常亮 Android 应用。

## 功能

- **屏幕常亮**：阻止屏幕自动息屏
- **低亮度模式**：降低屏幕亮度以节省电量
- **亮度可调**：滑块控制亮度 1%-100%
- **开机自启**：支持重启后自动恢复常亮状态
- **暗色主题**：深色 UI 进一步降低 OLED 屏幕功耗
- **前台服务**：稳定的后台保活机制

## 编译方法

### 使用 Android Studio（推荐）

1. 打开 Android Studio
2. 选择 `File → Open`，选择 `ScreenKeeper` 文件夹
3. 等待 Gradle 同步完成
4. 点击 `Run` 或 `Build → Build Bundle(s) / APK(s) → Build APK(s)`

### 使用命令行

```bash
cd ScreenKeeper
./gradlew assembleDebug
# APK 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 系统要求

- Android 7.0 (API 24) 及以上
- targetSdk: 34 (Android 14)

## 权限说明

| 权限 | 用途 |
|------|------|
| `FOREGROUND_SERVICE` | 前台服务保活 |
| `POST_NOTIFICATIONS` | 显示服务运行通知 |
| `SYSTEM_ALERT_WINDOW` | 低亮度模式悬浮窗 |
| `WRITE_SETTINGS` | 调节系统亮度 |

## 低功耗设计

- 深色 UI 主题（OLED 屏幕黑色像素不发光）
- 低亮度模式可将亮度降至 1%
- 前台服务使用低优先级通知
- 最小化 CPU 唤醒频率（30秒一次）
