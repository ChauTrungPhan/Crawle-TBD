package com.example.crawlertbdgemini2modibasicview.utils;

public enum AppKeys {
    // SharedPreferences Keys
    PREFS_SETTINGS,
    KEY_IS_FIRST_RUN,
    KEY_SELECTED_CRAWL_TYPE_ID_NAME,
    KEY_K0_SETTING,
    KEY_K1_SETTING,
    KEY_CHOON_CRAWLER_THEO_URL,
    KEY_ELAPSED_TIME,

    // WorkManager Keys
    WORK_REQUEST_ID_KEY,
    WORK_PROGRESS_PERCENT,

    // Request Codes (Nếu muốn gộp)
    REQUEST_CODE_WRITE_EXTERNAL_STORAGE,

    // Default Crawl Type ID (Nếu muốn gộp)
    CRAWL_TYPE_DEFAULT_ID,

    // Existing PrefKey
    FIRST_RUN,
    LAST_EXPORT_PATH,
    EXPORT_FORMAT,
    ENABLE_NOTIFICATION,
    AUTO_START_PERMISSION,

    // Keys cho tổng số URL và số URL đã xử lý có "start=1"
    TOTAL_URLS_START1_KEY,
    PROCESSED_URLS_START1_COUNT_KEY,
    SUM_PROCESSED_URLS_COUNT_KEY
}