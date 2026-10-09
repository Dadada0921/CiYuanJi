package com.ciyuanji.lsposed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** API 102 entry point for the automatic sign-in hook. */
public final class MainHook extends XposedModule {
    private static final String MODULE_TAG = "[次元机]";
    private static final String TARGET_PACKAGE = "com.xunyou.rb";
    private static final String PREFS_NAME = "ciyuanji_auto_sign_in";
    private static final String PREF_LAST_RUN = "last_run_date";
    private static final String SIGN_PATH = "/sign";
    private static final String HOME_ACTIVITY = "com.xunyou.apphome.ui.activity.HomeActivity";
    private static final String WEB_ACTIVITY = "com.xunyou.libservice.component.web.WebActivity";

    private static volatile boolean signPageRunning;
    private final Set<Activity> scheduledSignPages =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        try {
            Class<?> homeActivity = classLoader.loadClass(HOME_ACTIVITY);
            Method homeOnResume = homeActivity.getDeclaredMethod("onResume");
            hook(homeOnResume).setId("ciyuanji.home.onResume").intercept(chain -> {
                Object result = chain.proceed();
                startHiddenSignPage((Activity) chain.getThisObject(), classLoader);
                return result;
            });

            Class<?> webActivity = classLoader.loadClass(WEB_ACTIVITY);
            Method webOnCreate = webActivity.getDeclaredMethod("onCreate", Bundle.class);
            hook(webOnCreate).setId("ciyuanji.sign.onCreate").intercept(chain -> {
                Object result = chain.proceed();
                Activity activity = (Activity) chain.getThisObject();
                if (isSignPage(activity)) {
                    hideAndScheduleFinish(activity);
                }
                return result;
            });

            Method webOnResume = webActivity.getDeclaredMethod("onResume");
            hook(webOnResume).setId("ciyuanji.sign.onResume").intercept(chain -> {
                Object result = chain.proceed();
                Activity activity = (Activity) chain.getThisObject();
                if (isSignPage(activity)) {
                    hideAndScheduleFinish(activity);
                }
                return result;
            });

            log(Log.INFO, MODULE_TAG, "API 102 hooks installed for " + TARGET_PACKAGE);
        } catch (Throwable error) {
            log(Log.ERROR, MODULE_TAG, "Failed to install sign-in hooks", error);
        }
    }

    private static boolean isSignPage(Activity activity) {
        Intent intent = activity.getIntent();
        return intent != null && SIGN_PATH.equals(intent.getStringExtra("url"));
    }

    private void hideAndScheduleFinish(Activity activity) {
        synchronized (scheduledSignPages) {
            if (!scheduledSignPages.add(activity)) {
                return;
            }
        }

        WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        attrs.alpha = 0.0f;
        activity.getWindow().setAttributes(attrs);
        activity.getWindow().getDecorView().setAlpha(0.0f);
        activity.overridePendingTransition(0, 0);

        activity.getWindow().getDecorView().postDelayed(() -> {
            markRun(activity);
            signPageRunning = false;
            if (!activity.isFinishing()) {
                activity.finish();
                activity.overridePendingTransition(0, 0);
                activity.getWindow().getDecorView().postDelayed(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        activity.finishAndRemoveTask();
                    }
                }, 1500L);
            }
        }, 3500L);
    }

    private void startHiddenSignPage(Activity activity, ClassLoader classLoader) {
        if (signPageRunning || activity.isFinishing() || !isLoggedIn(classLoader)
                || today().equals(activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .getString(PREF_LAST_RUN, ""))) {
            return;
        }

        try {
            Class<?> webActivity = classLoader.loadClass(WEB_ACTIVITY);
            Intent intent = new Intent(activity, webActivity);
            intent.putExtra("url", SIGN_PATH);
            intent.putExtra("show", false);
            signPageRunning = true;
            activity.startActivity(intent);
            log(Log.INFO, MODULE_TAG, "Started hidden /sign WebActivity");
        } catch (Throwable error) {
            signPageRunning = false;
            log(Log.ERROR, MODULE_TAG, "Failed to start /sign", error);
        }
    }

    private static boolean isLoggedIn(ClassLoader classLoader) {
        try {
            Class<?> sessionClass = classLoader.loadClass("d3.d");
            Method getSession = sessionClass.getDeclaredMethod("c");
            Object session = getSession.invoke(null);
            Method isLoggedIn = session.getClass().getDeclaredMethod("h");
            return Boolean.TRUE.equals(isLoggedIn.invoke(session));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void markRun(Activity activity) {
        activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_RUN, today())
                .apply();
        Log.i(MODULE_TAG, "Hidden /sign page finished");
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }
}
