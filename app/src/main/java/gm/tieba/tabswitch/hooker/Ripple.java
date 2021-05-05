package gm.tieba.tabswitch.hooker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Rule;

public class Ripple extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        Rule.findRule("Lcom/baidu/tieba/R$layout;->new_sub_pb_list_item:I", new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View newSubPbListItem = (View) param.getResult();
                        View view = newSubPbListItem.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id")
                                .getField("new_sub_pb_list_richText").getInt(null));
                        view.setBackground(sRes.getDrawable(R.drawable.item_background_button, null));
                    }
                });
            }
        });
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.sub.SubPbLayout", sClassLoader,
                Context.class, AttributeSet.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof RelativeLayout) {
                                View view = (View) field.get(param.thisObject);
                                view.setBackground(sRes.getDrawable(R.drawable.item_background_button, null));
                                return;
                            }
                        }
                    }
                });
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.main.PbCommenFloorItemViewHolder", sClassLoader,
                XposedHelpers.findClass("com.baidu.tbadk.TbPageContext", sClassLoader), View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof LinearLayout) {
                                View view = (View) field.get(param.thisObject);
                                if (view.getId() == sClassLoader.loadClass("com.baidu.tieba.R$id")
                                        .getField("all_content").getInt(null)) {
                                    view.setBackground(sRes.getDrawable(R.drawable.item_background_button, null));
                                    return;
                                }
                            }
                        }
                    }
                });
    }
}