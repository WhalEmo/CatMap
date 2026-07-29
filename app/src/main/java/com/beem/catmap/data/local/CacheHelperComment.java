package com.beem.catmap.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
public class CacheHelperComment {
    private static final String PREFS_NAME = "begenilenYorumCache";
    private static final String KEY_BEGENILEN_SET = "begenilenSet";

    private static final String KEY_BEGENILEN_YANIT_SET = "begenilenYanitSet";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLikedSet(Context context, Set<String> begenilenSet) {
        getPrefs(context).edit().putStringSet(KEY_BEGENILEN_SET, begenilenSet).apply();
    }

    public static Set<String> loadLikedSet(Context context) {
        Set<String> savedSet = getPrefs(context).getStringSet(KEY_BEGENILEN_SET, null);
        if (savedSet == null) {
            return new HashSet<>();
        }
        return new HashSet<>(savedSet);
    }

    public static void saveLikedReplySet(Context context, Set<String> begenilenYanitSet) {
        getPrefs(context).edit().putStringSet(KEY_BEGENILEN_YANIT_SET, begenilenYanitSet).apply();
    }

    public static Set<String> loadLikedReplySet(Context context) {
        Set<String> savedSet = getPrefs(context).getStringSet(KEY_BEGENILEN_YANIT_SET, null);
        if (savedSet == null) {
            return new HashSet<>();
        }
        return new HashSet<>(savedSet);
    }

    public static void clearCache(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}