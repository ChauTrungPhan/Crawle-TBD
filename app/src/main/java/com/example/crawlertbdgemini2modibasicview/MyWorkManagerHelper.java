package com.example.crawlertbdgemini2modibasicview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.work.WorkInfo;

public class MyWorkManagerHelper {
    // Dùng cho SharedPreferences
    private static final String WORK_ID_KEY = "work_id";
    private static final String WORK_STATE_KEY = "work_state";

    // Dùng cho SharedPreferences
    public static void saveWorkState(Context context, String workId, String workState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(WORK_ID_KEY, workId)
                .putString(WORK_STATE_KEY, workState)
                .apply();
    }

    // Dùng cho SharedPreferences
    public static String getSavedWorkId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(WORK_ID_KEY, null);
    }

    // Dùng cho SharedPreferences
    public static String getSavedWorkState(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(WORK_STATE_KEY, null);
    }

    // Dùng cho SharedPreferences
    public static void clearSavedWorkState(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(WORK_ID_KEY)
                .remove(WORK_STATE_KEY)
                .apply();
    }

    // Dùng cho SharedPreferences
    public static void updateWorkState(Context context, WorkInfo workInfo) {
        if (workInfo != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit()
                    .putString(WORK_STATE_KEY, workInfo.getState().name())
                    .apply();
        }
    }
}