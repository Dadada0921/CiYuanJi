package com.ciyuanji.lsposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Entry point for the 次元机 LSPosed module.
 *
 * <p>Target packages and hooks will be added here after the target application
 * is provided. Keeping the module inactive by default makes the initial APK
 * safe to install and easy to verify in LSPosed.</p>
 */
public final class MainHook implements IXposedHookLoadPackage {
    private static final String MODULE_TAG = "[次元机]";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // Intentionally no-op until a target package is configured.
        // Example for a future target:
        // if ("com.example.target".equals(lpparam.packageName)) {
        //     hookTarget(lpparam.classLoader);
        // }
    }

    /**
     * Example hook helper kept here as a reference for future work.
     * Remove or adapt it when the target app and method are known.
     */
    @SuppressWarnings("unused")
    private static void hookExample(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(
                "com.example.target.TargetClass",
                classLoader,
                "targetMethod",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(MODULE_TAG + " example hook called");
                    }
                });
    }
}
