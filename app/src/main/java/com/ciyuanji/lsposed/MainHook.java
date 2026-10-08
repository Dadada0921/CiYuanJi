package com.ciyuanji.lsposed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Automatic sign-in for com.xunyou.rb.
 *
 * <p>The APK's sign-in entry is an H5 page at /sign, loaded by the app's own
 * WebActivity. This hook starts that original page in a transparent Activity,
 * so the app supplies its normal login token and JS bridge. No UI navigation
 * to the Mine page and no hand-built API signature are needed.</p>
 */
public final class MainHook implements IXposedHookLoadPackage {
    private static final String MODULE_TAG = "[次元机]";
    private static final String TARGET_PACKAGE = "com.xunyou.rb";
    private static final String PREFS_NAME = "ciyuanji_auto_sign_in";
    private static final String PREF_LAST_RUN = "last_run_date";
    private static final String SIGN_PATH = "/sign";
    private static final String WEB_ACTIVITY = "com.xunyou.libservice.component.web.WebActivity";

    private static volatile boolean signPageRunning;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(MODULE_TAG + " target process loaded");

        XposedHelpers.findAndHookMethod(
                "com.xunyou.apphome.ui.activity.HomeActivity",
                lpparam.classLoader,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        startHiddenSignPage(activity, lpparam.classLoader);
                    }
                });

        XposedHelpers.findAndHookMethod(
                WEB_ACTIVITY,
                lpparam.classLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (!SIGN_PATH.equals(activity.getIntent().getStringExtra("url"))) {
                            return;
                        }

                        // Keep the original WebActivity and JS bridge alive,
                        // but do not show its window to the user.
                        WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
                        attrs.alpha = 0.0f;
                        activity.getWindow().setAttributes(attrs);
                        activity.getWindow().getDecorView().setAlpha(0.0f);

                        activity.getWindow().getDecorView().postDelayed(() -> {
                            markRun(activity);
                            signPageRunning = false;
                            if (!activity.isFinishing()) {
                                activity.finish();
                            }
                        }, 8000L);
                    }
                });
    }

    private static void startHiddenSignPage(Activity activity, ClassLoader classLoader) {
        if (signPageRunning || activity.isFinishing() || !isLoggedIn(classLoader)
                || today().equals(activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .getString(PREF_LAST_RUN, ""))) {
            return;
        }

        try {
            Class<?> webActivity = Class.forName(WEB_ACTIVITY, false, classLoader);
            Intent intent = new Intent(activity, webActivity);
            intent.putExtra("url", SIGN_PATH);
            intent.putExtra("show", false);
            signPageRunning = true;
            activity.startActivity(intent);
            XposedBridge.log(MODULE_TAG + " started hidden /sign WebActivity");
        } catch (Throwable error) {
            signPageRunning = false;
            XposedBridge.log(MODULE_TAG + " failed to start /sign: " + error);
        }
    }

    private static boolean isLoggedIn(ClassLoader classLoader) {
        try {
            Object session = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("d3.d", classLoader), "c");
            Object loggedIn = XposedHelpers.callMethod(session, "h");
            return Boolean.TRUE.equals(loggedIn);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void markRun(Activity activity) {
        activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_RUN, today())
                .apply();
        XposedBridge.log(MODULE_TAG + " hidden /sign page finished");
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }
}
