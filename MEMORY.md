# MEMORY.md - Long-term Memory

## 用户信息
- 使用中文交流
- 有 Android 开发需求
- 对 GitHub 操作不太熟悉，需要详细指导
- GitHub 用户名: ming-a1
- 急脾气，期望一次到位，不喜欢反复提醒

## 项目记录

### ScreenKeeper（屏幕常亮 Android 应用）
- **日期**：2026-05-21
- **路径**：`/root/.openclaw/workspace/ScreenKeeper/`
- **GitHub**: https://github.com/ming-a1/ScreenKeeper
- **状态**：v1.7 (versionCode 8)，持续迭代中
- **功能**：屏幕常亮、低亮度模式、亮度调节、开机自启、亮色主题、**悬浮窗后台保活**
- **编译方式**：GitHub Actions 自动编译（服务器无 JDK/Android SDK）
- **技术栈**：Java, Material Design, targetSdk 34, minSdk 24

## 教训
- **改代码必须同时升版本号**，不要让用户提醒
- **MaterialSwitch 在暗色主题下有渲染问题**，用 SwitchCompat 更可靠
- **Android 14 (targetSdk 34) 前台服务需要 FOREGROUND_SERVICE_SPECIAL_USE 权限**
- **GitHub Actions workflow 的分支名要和实际推送分支一致**（之前写的 master 但推的是 main）
- **用户设备开了暗色模式**，Light 主题需要 AppCompatDelegate.MODE_NIGHT_NO 强制生效
- **WakeLock在新版Android后台被限制**，用1x1透明悬浮窗+FLAG_KEEP_SCREEN_ON更可靠
