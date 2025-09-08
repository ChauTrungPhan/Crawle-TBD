package com.example.crawlertbdgemini2modibasicview.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.crawlertbdgemini2modibasicview.AppConstants; // Import AppConstants

import java.util.concurrent.locks.ReentrantLock;

/**
 * Lớp tiện ích để tương tác với SharedPreferences.
 * Cung cấp các phương thức tĩnh để lưu và đọc các loại dữ liệu cơ bản
 * sử dụng các khóa được định nghĩa trong {@link PrefKey}.
 */
public class SharedPreferencesUtils {

    // Sử dụng hằng số PREFS_SETTINGS từ AppConstants để đảm bảo tính nhất quán.
    private static final String PREF_NAME = AppConstants.PREFS_SETTINGS;
    static ReentrantLock writeLock = new ReentrantLock();

    // Private constructor to prevent instantiation
    private SharedPreferencesUtils() {
        // This class is not meant to be instantiated.
    }

    /**
     * Lấy đối tượng SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @return Đối tượng SharedPreferences.
     */
    public static synchronized SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lưu một giá trị boolean vào SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) để lưu trữ giá trị.
     * @param value Giá trị boolean cần lưu.
     */
    public static void saveBoolean(Context context, PrefKey key, boolean value) {   // ghi
        // ghi, PHẢI LOCK
        writeLock.lock();
        try {
            getPrefs(context).edit().putBoolean(key.name(), value).apply();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Lấy một giá trị boolean từ SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) của giá trị cần lấy.
     * @param defaultValue Giá trị mặc định nếu khóa không tồn tại.
     * @return Giá trị boolean được lưu, hoặc defaultValue nếu không tìm thấy.
     */
    public static boolean getBoolean(Context context, PrefKey key, boolean defaultValue) {  // Đọc, KHÔNG CẦN lock
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getBoolean(key.name(), defaultValue);
    }


    /**
     * Lưu một giá trị String vào SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key khóa (thay cho khóa PrefKey) để lưu trữ giá trị.
     * @param value Giá trị String cần lưu.
     */
    public static void saveString(Context context, PrefKey key, String value) { //Gốc là PrefKey key, thay bằng String typeCrawl
        // ghi, PHẢI LOCK
        writeLock.lock();
        try {
            getPrefs(context).edit().putString(key.name(), value).apply();
        } finally {
            writeLock.unlock();
        }

    }

    /**
     * Lấy một giá trị String từ SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) của giá trị cần lấy.
     * @param defaultValue Giá trị mặc định nếu khóa không tồn tại.
     * @return Giá trị String được lưu, hoặc defaultValue nếu không tìm thấy.
     */
    public static String getString(Context context, PrefKey key, String defaultValue) {
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getString(key.name(), defaultValue);
    }

    /**
     * Lưu một giá trị int vào SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) để lưu trữ giá trị.
     * @param value Giá trị int cần lưu.
     */
    public static void saveInt(Context context, PrefKey key, int value) {
        // ghi, PHẢI LOCK
        writeLock.lock();
        try {
            getPrefs(context).edit().putInt(key.name(), value).apply();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Lấy một giá trị int từ SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) của giá trị cần lấy.
     * @param defaultValue Giá trị mặc định nếu khóa không tồn tại.
     * @return Giá trị int được lưu, hoặc defaultValue nếu không tìm thấy.
     */
    public static synchronized int getInt(Context context, PrefKey key, int defaultValue) {
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getInt(key.name(), defaultValue);
    }

    /**
     * Lưu một giá trị long vào SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) để lưu trữ giá trị.
     * @param value Giá trị long cần lưu.
     */
    public static void saveLong(Context context, PrefKey key, long value) {
        // ghi, PHẢI LOCK
        writeLock.lock();
        try {
            getPrefs(context).edit().putLong(key.name(), value).apply();
        } finally {
            writeLock.unlock();
        }
    }


    // Thêm
    // THÊM CÁC PHƯƠNG THỨC CHO KIỂU LONG

    public static void saveLong(Context context, String keyName, long value) {
        // ghi, PHẢI LOCK
        writeLock.lock();
        try {
            getPrefs(context).edit().putLong(keyName, value).apply();
        } finally {
            writeLock.unlock();
        }
    }
    /**
     * Lấy một giá trị long từ SharedPreferences.
     *
     * @param context Context của ứng dụng.
     * @param key Khóa (PrefKey) của giá trị cần lấy.
     * @param defaultValue Giá trị mặc định nếu khóa không tồn tại.
     * @return Giá trị long được lưu, hoặc defaultValue nếu không tìm thấy.
     */

    // Thêm
    public static long getLong(Context context, PrefKey key, long defaultValue) {
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getLong(key.name(), defaultValue);
    }
    public static long getLong(Context context, String keyName, long defaultValue) {
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getLong(keyName, defaultValue);
    }
    // THÊM
    public static void remove(Context context, String key) {   //Thêm
        // ghi, PHẢI LOCK: remove cần lock không?
        writeLock.lock();
        try {
            // Nên remove khi phát hiện
            getPrefs(context).edit().remove(key).apply();
        } finally {
            writeLock.unlock();
        }
    }

    public static int getCountKyTu(Context context, PrefKey key) {
        // Đọc, KHÔNG CẦN LOCK
        return getPrefs(context).getInt(key.name(), 0);
    }
    public static void saveCountKyTu(Context context, PrefKey key, int value) {
        getPrefs(context).edit().putInt(key.name(), value).apply();
    }

    /**
     * Lưu trạng thái cờ HAS_CRAWL_STARTED vào SharedPreferences.
     * @param context Context của ứng dụng.
     * @param hasStarted Giá trị boolean true nếu crawl đã bắt đầu, false nếu reset.
     */
    public static void setHasCrawlStarted(Context context, boolean hasStarted) {
        // ghi, PHẢI LOCK: chỉ chạy 1 luồng trước khi chạy đâ luồng
        writeLock.lock();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(AppConstants.KEY_HAS_CRAWL_STARTED, hasStarted);
            editor.apply(); // Sử dụng apply() để lưu bất đồng bộ, commit() để lưu đồng bộ
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Lấy trạng thái cờ HAS_CRAWL_STARTED từ SharedPreferences.
     * @param context Context của ứng dụng.
     * @return true nếu crawl đã từng bắt đầu, false nếu chưa bao giờ hoặc đã bị reset.
     */
    public static boolean getHasCrawlStarted(Context context) {
        // Đọc, KHÔNG CẦN LOCK
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(AppConstants.KEY_HAS_CRAWL_STARTED, false); // Giá trị mặc định là false
    }
}