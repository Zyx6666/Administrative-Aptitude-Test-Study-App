package com.idiom.memo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SettingsDao {
    private final DbHelper helper;
    private final String prefix;

    public SettingsDao(Context context, String moduleId) {
        helper = new DbHelper(context);
        prefix = moduleId + ":";
    }

    public int getInt(String key, int def) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("settings", new String[]{"value"}, "key=?",
                new String[]{prefix + key}, null, null, null)) {
            if (c.moveToFirst()) {
                try {
                    return Integer.parseInt(c.getString(0));
                } catch (NumberFormatException e) {
                    return def;
                }
            }
        }
        return def;
    }

    public void setInt(String key, int value) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("key", prefix + key);
        v.put("value", String.valueOf(value));
        db.insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int[] getIntervals() {
        return new int[]{
                getInt("unknown_interval_days", 1),
                getInt("fuzzy_interval_days", 3),
                getInt("known_interval_days", 7)
        };
    }

    public int[] getRatios() {
        return new int[]{
                getInt("draw_ratio_unknown", 50),
                getInt("draw_ratio_fuzzy", 30),
                getInt("draw_ratio_known", 20)
        };
    }

    public int getSessionSize() {
        return getInt("session_size", 20);
    }

    public int getFreqThreshold() {
        return getInt("freq_threshold_percent", 30);
    }

    public void setFreqThreshold(int percent) {
        setInt("freq_threshold_percent", percent);
    }

    public void save(int[] intervals, int[] ratios, int sessionSize) {
        setInt("unknown_interval_days", intervals[0]);
        setInt("fuzzy_interval_days", intervals[1]);
        setInt("known_interval_days", intervals[2]);
        setInt("draw_ratio_unknown", ratios[0]);
        setInt("draw_ratio_fuzzy", ratios[1]);
        setInt("draw_ratio_known", ratios[2]);
        setInt("session_size", sessionSize);
    }
}
