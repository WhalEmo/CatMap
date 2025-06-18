package com.emrullah.catmap;

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

    public static void saveBegenilenSet(Context context, Set<String> begenilenSet) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_BEGENILEN_SET, begenilenSet).apply();
    }

    public static Set<String> loadBegenilenSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_BEGENILEN_SET, new HashSet<>());
    }
    // ✅ YENİ: Beğeni Sayısı Map’ini kaydet
    public static void saveBegeniSayilariMap(Context context, Map<String, Integer> begeniMap) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject jsonObject = new JSONObject(begeniMap); // Map → JSON
        prefs.edit().putString(KEY_BEGENI_SAYILARI, jsonObject.toString()).apply();
    }

    // ✅ YENİ: Beğeni Sayısı Map’ini yükle
    public static Map<String, Integer> loadBegeniSayilariMap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_BEGENI_SAYILARI, "{}");
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
}
