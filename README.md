# 屏幕常亮 (Screen Keeper)

一款低功耗的屏幕常亮 Android 应用。

## 功能

- **屏幕常亮**：阻止屏幕自动息屏，前后台均生效
- **低亮度模式**：降低屏幕亮度以节省电量（后台也生效）
- **亮度可调**：滑块控制亮度 1%-100%，实时生效
- **开机自启**：支持重启后自动恢复常亮状态
- **亮色主题**：清爽的浅色 UI
- **前台服务**：稳定的后台保活机制
- **悬浮窗保活**：1×1 像素透明悬浮窗实现后台常亮
- **广泛兼容**：适配多品牌 Android 设备

## 下载

前往 [Releases](https://github.com/ming-a1/ScreenKeeper/releases) 页面下载最新 APK。

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

### GitHub Actions 自动编译

推送 tag 后 GitHub Actions 会自动编译并发布 Release：

```bash
git tag v1.9
git push origin v1.9
```

## 系统要求

- Android 7.0 (API 24) 及以上
- targetSdk: 34 (Android 14)

## 权限说明

| 权限 | 用途 |
|------|------|
| `FOREGROUND_SERVICE` | 前台服务保活 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14 前台服务类型 |
| `POST_NOTIFICATIONS` | 显示服务运行通知 |
| `SYSTEM_ALERT_WINDOW` | 悬浮窗常亮（核心功能） |
| `WRITE_SETTINGS` | 调节系统亮度 |

## 更新日志

### v1.9 (最新)
- 低亮度模式后台生效：亮度控制迁移到前台服务悬浮窗
- 拖动滑块实时更新亮度，无需重新开关
- 通知栏显示当前亮度百分比

### v1.8
- 修复其他品牌手机打开常亮闪退的问题
- 悬浮窗权限检查提前到启动服务之前
- 全面增加异常保护，提升多设备兼容性

### v1.7
- 改用 1×1 像素透明悬浮窗替代 WakeLock 实现后台常亮

### v1.6
- WakeLock 方案（已被 v1.7 替代）

### v1.5
- 修复 Android 14 前台服务崩溃

### v1.4
- MaterialSwitch 替换为 SwitchCompat，修复开关不可见

### v1.3
- 强制亮色模式，忽略系统暗色设置

### v1.2
- 切换为亮色主题

### v1.1
- 修复 Switch 颜色和 GitHub Actions 分支配置

### v1.0
- 初始版本
