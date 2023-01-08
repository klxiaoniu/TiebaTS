package gm.tieba.tabswitch.hooker.eliminate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.Parser;
import gm.tieba.tabswitch.util.ReflectUtils;

public class FragmentTab extends XposedContext implements IHooker, Obfuscated {

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringMatcher("has_show_message_tab_tips"));
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            var md = ReflectUtils.findFirstMethodByExactType(clazz, ArrayList.class);
            XposedBridge.hookMethod(md, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    for (var fieldName : Parser.parseMainTabActivityConfig()) {
                        if (Preferences.getStringSet("fragment_tab").contains(fieldName)) {
                            var clazz = XposedHelpers.findClass(
                                    "com.baidu.tbadk.core.atomData.MainTabActivityConfig", sClassLoader);
                            XposedHelpers.setStaticBooleanField(clazz, fieldName,
                                    !XposedHelpers.getStaticBooleanField(clazz, fieldName));
                        }
                    }

                    var tabsToRemove = new HashSet<String>();
                    if (Preferences.getBoolean("home_recommend")) {
                        tabsToRemove.add("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic");
                    }
                    if (Preferences.getBoolean("write_thread")) {
                        tabsToRemove.add("com.baidu.tieba.write.bottomButton.WriteThreadDelegateStatic");
                    }
                    var list = (ArrayList<?>) param.args[0];
                    list.removeIf(tab -> tabsToRemove.contains(tab.getClass().getName()));
                }
            });
        });
    }
}
