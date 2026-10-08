# 次元机

次元机是一个用于 LSPosed 的 Android 模块工程。当前版本只包含稳定的模块入口，不会对任何应用执行 Hook；等确定目标软件、进程和功能后，再在 `MainHook` 中加入对应逻辑。

## 本地构建

需要 JDK 17、Android SDK 35 和 Gradle 8.10：

```bash
gradle assembleRelease
```

APK 位于 `app/build/outputs/apk/release/`。

## GitHub Actions

推送到 `main` 或 `master`，或者手动运行 **Build APK** workflow，即可构建并上传 `ciyuanji-release` artifact。

## 后续接入 Hook

提供目标应用的包名、需要修改的功能、Android 版本/架构，以及必要的类名或日志后，在 `app/src/main/java/com/ciyuanji/lsposed/MainHook.java` 中实现目标进程判断和 Hook。
