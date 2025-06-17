package com.emrullah.catmap;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class CacheHelper {

    private static final String PREFS_NAME = "begenilenYorumCache";
    private static final String KEY_BEGENILEN_SET = "begenilenSet";

    public static void saveBegenilenSet(Context context, Set<String> begenilenSet) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_BEGENILEN_SET, begenilenSet).apply();
    }

    public static Set<String> loadBegenilenSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_BEGENILEN_SET, new HashSet<>());
    }
}
