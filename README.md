# 次元机

次元机是一个使用 libxposed Modern API 102 的 Android 模块工程，当前目标应用包名为 `com.xunyou.rb`。模块会在检测到已登录后，后台加载应用原本的 `/sign` H5 页面完成每日签到，不进入“我的”页面，也不模拟控件点击。

## 本地构建

需要 JDK 17、Android SDK 37 和 Gradle 9.3.1。Xposed 入口与作用域配置位于 `app/src/main/resources/META-INF/xposed/`：

```bash
gradle assembleRelease
```

APK 位于 `app/build/outputs/apk/release/`。

## GitHub Actions

推送到 `main` 或 `master`，或者手动运行 **Build APK** workflow，即可构建并上传 `ciyuanji-release` artifact。

## 后续接入 Hook

如果目标应用更新了签到入口或 WebActivity 类名，需要重新分析 APK 并更新 `MainHook`。
