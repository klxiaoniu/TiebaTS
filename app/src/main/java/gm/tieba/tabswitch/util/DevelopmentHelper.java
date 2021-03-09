package gm.tieba.tabswitch.util;

import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;

public class DevelopmentHelper extends Hook {
    public static void hookAllMethods(ClassLoader classLoader, String clazz) throws Throwable {
        Method[] methods = classLoader.loadClass(clazz).getDeclaredMethods();
        for (Method method : methods) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Log.e("gm.tieba.tabswitch", clazz);
                    Log.e("gm.tieba.tabswitch", method.getName());
                }
            });
        }
    }
}