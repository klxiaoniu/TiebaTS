package gm.tieba.tabswitch.hooker.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Rule {
    private static List<Map<String, String>> sRulesFromDb;

    public static void init(Context context) {
        sRulesFromDb = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
        Cursor c = db.query("rules", null, null, null, null, null, null);
        for (int j = 0; j < c.getCount(); j++) {
            c.moveToNext();
            Map<String, String> map = new HashMap<>();
            map.put("rule", c.getString(1));
            map.put("class", c.getString(2));
            map.put("method", c.getString(3));
            sRulesFromDb.add(map);
        }
        c.close();
        db.close();
    }

    public interface Callback {
        void onRuleFound(String rule, String clazz, String method) throws Throwable;
    }

    public static void findRule(Object... rulesAndCallback) throws Throwable {
        if (rulesAndCallback.length != 0
                && rulesAndCallback[rulesAndCallback.length - 1] instanceof Callback) {
            Callback callback = (Callback) rulesAndCallback[rulesAndCallback.length - 1];
            for (String rule : getParameterRules(rulesAndCallback)) {
                for (Map<String, String> map : sRulesFromDb) {
                    if (Objects.equals(map.get("rule"), rule)) {
                        callback.onRuleFound(rule, map.get("class"), map.get("method"));
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }

    private static String[] getParameterRules(Object[] rulesAndCallback) {
        if (rulesAndCallback[0] instanceof String[]) return (String[]) rulesAndCallback[0];

        String[] rules = new String[rulesAndCallback.length - 1];
        for (int i = 0; i < rulesAndCallback.length - 1; i++) {
            rules[i] = (String) rulesAndCallback[i];
        }
        return rules;
    }

    public static boolean isRuleFound(String rule) {
        for (Map<String, String> map : sRulesFromDb) {
            if (Objects.equals(map.get("rule"), rule)) {
                return true;
            }
        }
        return false;
    }
}