package com.beem.catmap.YorumYanit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
public class CacheHelperYorum {

    private static final String PREFS_NAME = "begenilenYorumCache";
    private static final String KEY_BEGENILEN_SET = "begenilenSet";
    private static final String KEY_BEGENI_SAYILARI = "begeniSayilariMap";

    // --- YENİ EKLENENLER (Yanıtlar İçin) ---
    private static final String KEY_BEGENILEN_YANIT_SET = "begenilenYanitSet";
    private static final String KEY_BEGENI_SAYILARI_YANIT = "begeniSayilariYanitMap";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveBegenilenSet(Context context, Set<String> begenilenSet) {
        getPrefs(context).edit().putStringSet(KEY_BEGENILEN_SET, begenilenSet).apply();
    }

    public static Set<String> loadBegenilenSet(Context context) {
        Set<String> savedSet = getPrefs(context).getStringSet(KEY_BEGENILEN_SET, null);
        if (savedSet == null) {
            return new HashSet<>();
        }
        return new HashSet<>(savedSet);
    }

    // --- YENİ: Yanıt Beğeni Set Kaydet/Yükle ---
    public static void saveBegenilenYanitSet(Context context, Set<String> begenilenYanitSet) {
        getPrefs(context).edit().putStringSet(KEY_BEGENILEN_YANIT_SET, begenilenYanitSet).apply();
    }

    public static Set<String> loadBegenilenYanitSet(Context context) {
        Set<String> savedSet = getPrefs(context).getStringSet(KEY_BEGENILEN_YANIT_SET, null);
        if (savedSet == null) {
            return new HashSet<>();
        }
        return new HashSet<>(savedSet);
    }

    public static void saveBegeniSayilariMap(Context context, Map<String, Integer> begeniMap) {
        if (begeniMap == null) return;
        JSONObject jsonObject = new JSONObject(begeniMap);
        getPrefs(context).edit().putString(KEY_BEGENI_SAYILARI, jsonObject.toString()).apply();
    }

    public static Map<String, Integer> loadBegeniSayilariMap(Context context) {
        String json = getPrefs(context).getString(KEY_BEGENI_SAYILARI, "{}");
        Map<String, Integer> result = new HashMap<>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int value = jsonObject.getInt(key);
                result.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void clearCache(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}