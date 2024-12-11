package gm.tieba.tabswitch.hooker.eliminate

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.BuildConfig
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.util.getDimen
import gm.tieba.tabswitch.util.getObjectField
import org.json.JSONArray
import org.luckypray.dexkit.query.matchers.ClassMatcher

class PurgeMy : XposedContext(), IHooker, Obfuscated {

    private val mGridTopPadding = getDimen("tbds25").toInt()

    override fun key(): String {
        return "purge_my"
    }

    override fun matchers(): List<Matcher> {
        return if (DeobfuscationHelper.isTbBetweenVersionRequirement(
                BuildConfig.MIN_VERSION,
                "12.67"
            )
        )
            listOf(
                SmaliMatcher("Lcom/baidu/tieba/personCenter/view/PersonOftenFuncItemView;-><init>(Landroid/content/Context;)V"),
                SmaliMatcher(
                    "Lcom/baidu/nadcore/download/basic/AdAppStateManager;->instance()Lcom/baidu/nadcore/download/basic/AdAppStateManager;"
                ).apply {
                    classMatcher = ClassMatcher.create().usingStrings("隐私设置")
                }
            ) else emptyList()
    }

    override fun hook() {
        if (DeobfuscationHelper.isTbBetweenVersionRequirement(BuildConfig.MIN_VERSION, "12.67")) {
            hookBeforeMethod(
                "tbclient.Profile.DataRes\$Builder",
                "build", Boolean::class.javaPrimitiveType
            ) { param ->

                // 我的贴吧会员
                XposedHelpers.setObjectField(param.thisObject, "vip_banner", null)

                // 横幅广告
                XposedHelpers.setObjectField(param.thisObject, "banner", ArrayList<Any>())

                // 度小满 有钱花
                XposedHelpers.setObjectField(param.thisObject, "finance_tab", null)

                // 小程序
                XposedHelpers.setObjectField(param.thisObject, "recom_naws_list", ArrayList<Any>())
            }

            hookBeforeMethod(
                "tbclient.User\$Builder",
                "build", Boolean::class.javaPrimitiveType
            ) { param ->
                XposedHelpers.setObjectField(param.thisObject, "user_growth", null)
            }

            // Add padding to the top of 常用功能
            findRule(matchers()) { matcher, clazz, method ->
                when (matcher) {
                    "Lcom/baidu/tieba/personCenter/view/PersonOftenFuncItemView;-><init>(Landroid/content/Context;)V" ->
                        hookAfterConstructor(
                            clazz,
                            "com.baidu.tbadk.TbPageContext"
                        ) { param ->
                            val mView = getObjectField(param.thisObject, View::class.java)
                            mView?.setPadding(
                                mView.getPaddingLeft(),
                                mGridTopPadding,
                                mView.getPaddingRight(),
                                0
                            )
                        }

                    "Lcom/baidu/nadcore/download/basic/AdAppStateManager;->instance()Lcom/baidu/nadcore/download/basic/AdAppStateManager;" ->
                        hookReplaceMethod(clazz, method) { null }
                }
            }

            // 12.56+
            val personCenterMemberCardViewClass = XposedHelpers.findClassIfExists(
                "com.baidu.tieba.personCenter.view.PersonCenterMemberCardView",
                sClassLoader
            )
            personCenterMemberCardViewClass?.let {
                hookAfterConstructor(
                    it,
                    View::class.java
                ) { param ->
                    val mView = getObjectField(param.thisObject, View::class.java)
                    (mView?.parent as? ViewGroup)?.removeView(mView)
                }
            }

            // Skip because we already disabled all AB tests in purge
            if (!getBoolean("purge")) {
                // 我的页面 AB test
                hookBeforeMethod(
                    "com.baidu.tbadk.abtest.UbsABTestDataManager",
                    "parseJSONArray",
                    JSONArray::class.java
                ) { param ->
                    val currentABTestJson = param.args[0] as JSONArray
                    val newABTestJson = JSONArray()
                    for (i in 0 until currentABTestJson.length()) {
                        val currTest = currentABTestJson.getJSONObject(i)
                        if (!currTest.getString("sid").startsWith("12_64_my_tab_new")) {
                            newABTestJson.put(currTest)
                        }
                    }
                    param.args[0] = newABTestJson
                }
            }
        } else {
            // 12.68.1.0+
            hookBeforeMethod(
                "tbclient.Profile.DataRes\$Builder",
                "build", Boolean::class.javaPrimitiveType
            ) { param ->

                val zoneInfo =
                    XposedHelpers.getObjectField(param.thisObject, "zone_info")!! as List<*>
                XposedHelpers.setObjectField(param.thisObject, "zone_info", zoneInfo.filter {
                    // 保留 常用功能，辅助功能
                    XposedHelpers.getObjectField(it, "type") in listOf(
                        "common_func",
                        "auxiliary_func"
                    )
                })

//            param.thisObject.javaClass.declaredFields.forEach {
//                XposedHelpers.getObjectField(param.thisObject, it.name)?.let { value ->
//                    log("Field: ${it.name}, Value: $value")
//                }
//            }

            }

            hookBeforeMethod(
                "tbclient.Profile.CommonFunc\$Builder",
                "build", Boolean::class.javaPrimitiveType
            ) { param ->

                // 去除常用功能红点
                XposedHelpers.setObjectField(param.thisObject, "red_point_version", 0L)
            }
        }

    }
}
