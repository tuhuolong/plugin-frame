
package app.lib.plugin.frame.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by chenhao on 17/1/10.
 */

public class PreferenceUtil {
    public static String getSettingString(final Context c, final String key,
            final String defaultValue) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getString(key, defaultValue);
    }

    public static void setSettingString(final SharedPreferences sp, final String key,
            final String value) {
        sp.edit().putString(key, value).apply();
    }

    public static void setSettingString(final Context c, final String key, final String value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        settings.edit().putString(key, value).apply();
    }

    public static void appendSettingString(final SharedPreferences sp, final String key,
            final String append) {
        String oldValue = sp.getString(key, "");
        sp.edit().putString(key, oldValue + append).apply();
    }

    public static boolean getSettingBoolean(final Context c, final String key,
            final boolean defaultValue) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getBoolean(key, defaultValue);
    }

    public static boolean getSettingBoolean(SharedPreferences sp, String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    public static boolean hasKey(final Context c, final String key) {
        return PreferenceManager.getDefaultSharedPreferences(c).contains(key);
    }

    public static void setSettingBoolean(final Context c, final String key, final boolean value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        settings.edit().putBoolean(key, value).apply();
    }

    public static void setSettingBoolean(final SharedPreferences sp, final String key,
            final boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    public static void setSettingInt(final Context c, final String key, final int value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        settings.edit().putInt(key, value).apply();
    }

    public static void setSettingInt(final SharedPreferences sp, final String key,
            final int value) {
        sp.edit().putInt(key, value).apply();
    }

    public static void increaseSettingInt(final Context c, final String key) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        increaseSettingInt(settings, key);
    }

    public static void increaseSettingInt(final SharedPreferences sp, final String key) {
        final int v = sp.getInt(key, 0) + 1;
        sp.edit().putInt(key, v).apply();
    }

    public static void increaseSettingInt(final SharedPreferences sp, final String key,
            final int increment) {
        final int v = sp.getInt(key, 0) + increment;
        sp.edit().putInt(key, v).apply();
    }

    public static int getSettingInt(final Context c, final String key, final int defaultValue) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getInt(key, defaultValue);
    }

    public static int getSettingInt(SharedPreferences sp, String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    public static void setSettingFloat(final Context c, final String key, final float value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        settings.edit().putFloat(key, value).apply();
    }

    public static float getSettingFloat(final Context c, final String key,
            final float defaultValue) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getFloat(key, defaultValue);
    }

    public static void setSettingLong(final Context c, final String key, final long value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        settings.edit().putLong(key, value).apply();
    }

    public static void setSettingLong(final SharedPreferences sp, final String key,
            final long value) {
        sp.edit().putLong(key, value).apply();
    }

    public static long getSettingLong(SharedPreferences sp, String key, long defValue) {
        return sp.getLong(key, defValue);
    }

    public static long getSettingLong(final Context c, final String key, final long defaultValue) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getLong(key, defaultValue);
    }

    public static void increaseSettingLong(final SharedPreferences sp, final String key,
            final long increment) {
        final long v = sp.getLong(key, 0) + increment;
        sp.edit().putLong(key, v).apply();
    }

    public static void removePreference(final Context context, final String key) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(key).apply();
    }

    public static void removePreference(SharedPreferences sp, String key) {
        sp.edit().remove(key).apply();
    }

    public static void clearPreference(final SharedPreferences p) {
        final SharedPreferences.Editor editor = p.edit();
        editor.clear();
        editor.apply();
    }
}
