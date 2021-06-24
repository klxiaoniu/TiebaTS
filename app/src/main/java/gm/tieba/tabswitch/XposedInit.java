package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Adp;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.AntiConfusion;
import gm.tieba.tabswitch.hooker.AntiConfusionHelper;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.widget.TbDialog;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static String sPath;
    private Resources mRes;

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
        AssetManager assetManager = AssetManager.class.newInstance();
        XposedHelpers.callMethod(assetManager, "addAssetPath", sPath);
        mRes = new Resources(assetManager, null, null);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        ClassLoader classLoader = lpparam.classLoader;
        if (lpparam.packageName.equals("com.baidu.tieba") || XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) != null) {
            XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!(param.args[0] instanceof Application)) return;
                    Context context = ((Application) param.args[0]).getApplicationContext();
                    Preferences.init(context);
                    AntiConfusionHelper.initMatchers(mRes);
                    try {
                        AcRules.init(context);
                        List<String> lostList = AntiConfusionHelper.getRulesLost();
                        if (!lostList.isEmpty()) {
                            throw new SQLiteException(String.format(Locale.getDefault(),
                                    "rules incomplete, tbversion: %s, module version: %d, lost %d rule(s): %s",
                                    AntiConfusionHelper.getTbVersion(context), BuildConfig.VERSION_CODE, lostList.size(), lostList.toString()));
                        }
                    } catch (SQLiteException e) {
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log(e.toString());
                                if (!Preferences.getIsEULAAccepted()) return;
                                Activity activity = (Activity) param.thisObject;
                                String message = mRes.getString(R.string.rules_incomplete) + "\n" + e.getMessage();
                                if (AcRules.isRuleFound(mRes.getString(R.string.TbDialog))) {
                                    TbDialog bdAlert = new TbDialog(activity, "警告", message, false, null);
                                    bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
                                    bdAlert.setOnYesButtonClickListener(v -> AntiConfusionHelper
                                            .saveAndRestart(activity, "unknown", null, mRes));
                                    bdAlert.show();
                                } else {
                                    AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                            .setTitle("警告").setMessage(message).setCancelable(false)
                                            .setNegativeButton(activity.getString(android.R.string.cancel), (dialogInterface, i) -> {
                                            }).setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> AntiConfusionHelper
                                                    .saveAndRestart(activity, "unknown", null, mRes)).create();
                                    alertDialog.show();
                                }
                            }
                        });
                    }
                    if (AntiConfusionHelper.isVersionChanged(context)) {
                        new AntiConfusion(classLoader, context, mRes).hook();
                        return;
                    }

                    new Adp(classLoader, context, mRes);
                    new TSPreference(classLoader, context, mRes).hook();
                    for (Map.Entry<String, ?> entry : Preferences.getAll().entrySet()) {
                        BaseHooker.init(entry);
                    }
                }
            });
        } else switch (lpparam.packageName) {
            case "com.baidu.netdisk":
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.Navigate", classLoader,
                        "initFlashFragment", XC_MethodReplacement.returnConstant(null));
                // XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.advertise.FlashAdvertiseActivity", classLoader,
                //         "initFlashFragment", XC_MethodReplacement.returnConstant(null));
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.transfer.TransferListTabActivity", classLoader,
                        "initYouaGuideView", XC_MethodReplacement.returnConstant(null));
                // "show or close "
                for (Method method : XposedHelpers.findClass("com.baidu.netdisk.homepage.ui.card.____", classLoader).getDeclaredMethods()) {
                    if (Arrays.toString(method.getParameterTypes()).equals("[boolean]")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
                    }
                }

                XposedHelpers.findAndHookMethod("com.baidu.netdisk.media.video.source.NormalVideoSource", classLoader,
                        "getAdTime", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.preview.video.model._", classLoader,
                        "getAdTime", XC_MethodReplacement.returnConstant(0));

                for (Method method : XposedHelpers.findClass("com.baidu.netdisk.media.speedup.SpeedUpModle", classLoader).getDeclaredMethods()) {
                    if (method.getReturnType().equals(boolean.class)) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                    }
                }

                // "performInitVideoInfo"
                break;
            case "com.coolapk.market":
                XposedHelpers.findAndHookMethod(String.class, "format", String.class, Object[].class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].equals("https://%s%s")) {
                            Object[] objects = (Object[]) param.args[1];
                            if (objects.length == 2 && objects[1].equals("/api/ad/union/sdk/get_ads/")) {
                                param.setResult(null);
                            }
                        }
                    }
                });
                XposedHelpers.findAndHookConstructor(File.class, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Objects.equals(param.args[1], "gdt_plugin.jar")) {
                            XposedHelpers.setObjectField(param.thisObject, "path", null);
                        }
                    }
                });
                try {
                    XposedHelpers.findAndHookMethod("com.stub.StubApp", classLoader, "a", Context.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Context context = (Context) param.args[0];
                            ClassLoader classLoader = context.getClassLoader();
                            XposedHelpers.findAndHookMethod("com.coolapk.market.model.$$AutoValue_Feed", classLoader,
                                    "getDetailSponsorCard", XC_MethodReplacement.returnConstant(null));
                            XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                    "com.coolapk.market.model.AutoValue_Feed", classLoader), new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.args[142] = null;
                                }
                            });
                        }
                    });
                } catch (XposedHelpers.ClassNotFoundError ignored) {
                }
                break;
            case "com.jianshu.haruki":
                XposedHelpers.findAndHookMethod("com.baiji.jianshu.core.http.models.CommonUser", classLoader,
                        "isMember", XC_MethodReplacement.returnConstant(true));
                break;
        }
    }
}
