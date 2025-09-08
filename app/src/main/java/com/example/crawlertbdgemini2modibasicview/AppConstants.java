package com.example.crawlertbdgemini2modibasicview;

import java.net.URL;

/**
 * Lớp chứa các hằng số chung của ứng dụng.
 * Bao gồm các hằng số liên quan đến WorkManager, Request Codes, Notification, và Database.
 * Các khóa SharedPreferences cụ thể được quản lý trong enum {@link com.example.crawlertbdgemini2modibasicview.utils.PrefKey}.
 */
public class AppConstants {
    // Của File excel
    public static final String FILE_EXCEL_2KT_NOEXT ="ThuocBietDuoc2kt.xlsx";
    public static final String FILE_EXCEL_3KT_NOEXT = "ThuocBietDuoc3kt.xlsx";
    public static final String FILE_EXCEL_CUSTOM_NOEXT = "ThuocBietDuocCustom.xlsx";
    // INDEX_COLOR(IndexColor) MÀU SẴC: STYLE CỦA CELL EXCEL
    // Định nghĩa các STYLE_INDEX từ styles.xml của bạn
    private static final int STYLE_INDEX_NORMAL = 0;    // 0 - default: Nền trắng + chữ đen
    private static final int STYLE_INDEX_YELLOW = 1;    // 1 - Nền yellow + Chữ đen mặc định
    private static final int STYLE_INDEX_RED = 2;       // 2 - Nền red + Chữ trắng
    private static final int STYLE_INDEX_GREEN = 3;     // 3 - Nền green + Chữ đen
    // TITLE + HEADER
    private static final int STYLE_INDEX_TITLE = 4;     // 4 - Nền trong suốt(TITLE) + Chữ Xanh Blue, ĐẬM
    private static final int STYLE_INDEX_HEADER = 5;    // 5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM

    //private static final int STYLE_INDEX_DATE = 6;      // 6 - Nền CAM ORANGE(LỖI) + Chữ TRẮNG, ĐẬM
    private static final int STYLE_INDEX_ERR = 6;      // 6 - Nền CAM ORANGE(LỖI) + Chữ TRẮNG, ĐẬM






    /// /

    // Của CrawlType
    public static final String RADIOURL2KT = "radioUrl2kt";
    public static final String RADIOURL3KT = "radioUrl3kt";
    public static final String RADIOURLTUYCHINH = "radioUrlTuyChinh";

    //URL
    public static final String URL0 = "https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=";
    public static final String URL1 = "&opt=TT&start=1";

    // Default Crawl Type ID
    public static final String CRAWL_TYPE_DEFAULT_ID = "radioUrl2kt"; // Chú ý: Đảm bảo khớp với giá trị trong CrawlType enum

    // WorkManager Keys
    public static final String WORK_REQUEST_ID_KEY = "work_request_id";
    public static final String WORK_PROGRESS_PERCENT = "work_progress_percent";
    public static final String WORK_TOTAL_RECORDS = "total_records";
    public static final String WORK_CURRENT_RECORDS_PROCESSED = "current_records_processed";
    public static final String URL_CURRENT = "url_current";
    public static final String WORK_STATUS_MESSAGE = "status_message";
    //private static final String PREF_NAME = "MyCrawlerPrefs"; // Tên của file SharedPreferences
    public static final String KEY_HAS_CRAWL_STARTED = "has_crawl_started"; // Key cho cờ

    // Request Codes
    public static final String UNIQUE_WORK_NAME = "unique_CrawlWork"; // Tên duy nhất cho WorkRequest
    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_CODE_AUTO_START_PERMISSION = 2;
    // Hàng số table
    public static final String TABLE_THUOC = "TABLE_THUOC";
    public static final String NAME_FILE_EXCEL = "NAME_FILE_EXCEL";
    public static final String CRAWL_TYPE = "CRAWL_TYPE";
    public static final String CUSTOM_CRAWL_STRING = "CUSTOM_CRAWL_STRING";

    public static final String EXPORT_TYPE = "EXPORT_TYPE";

    // Notification Constants
    public static final String NOTIFICATION_CHANNEL_ID = "simplifiedcoding";
    public static final int NOTIFICATION_ID = 10; // ID cố định cho thông báo

    // Database Constants
    public static final String DATABASE_NAME = "ThuocBietDuoc"; // Đảm bảo trùng với AppDatabase
    public static final int DATABASE_VERSION = 1; // Đảm bảo trùng với AppDatabase
    // Other Constants
    public static final int CHUNK_SIZE_EXCEL_EXPORT = 5000; // Kích thước chunk khi xuất Excel

    // Default values for settings (nếu K0, K1 là cài đặt chung không thay đổi)
    // Nếu K0, K1 là cài đặt cho từng loại crawl, nên di chuyển vào CrawlType hoặc SettingsRepository
    // Có thể bỏ KEY_K0_SETTING và KEY_K1_SETTING nếu dùng PrefKey
    // public static final String KEY_K0_SETTING = "k0_setting";
    // public static final String KEY_K1_SETTING = "k1_setting";
    // ...

    //public static final int DEFAULT_K0_SETTING = 0;
    //public static final int DEFAULT_K1_SETTING = -1; // -1 cho "đến cuối mảng" (hoặc một giá trị nào đó biểu thị "đến cuối mảng") vào AppConstants.java.
    // SharedPreferences, DAO
    public static final String PREFS_SETTINGS = "MyPrefsSettings"; // Đảm bảo trùng với AppConstants.PREFS_SETTINGS nếu có ý định

    public static final String WORK_ELAPSED_TIME = "elapsed_time";
    public static final String WORK_REMAINING_TIME = "remaining_time";

    // WorkManager Progress Keys for Thread Monitoring
    public static final String THREADS_WAITING = "threads_waiting";
    public static final String THREADS_RUNNING = "threads_running";
    public static final String THREADS_COMPLETED = "threads_completed"; // KHÔNG TỂ CHÍNH XÁC
    //số luồng đang chờ đợi
    public static final String QUEUED_TASKS = "queued_tasks";
    //số luồng đang hoạt động
    public static final String ACTIVE_THREADS = "active_threads";
    //số luồng tối đa
    public static final String PARALLELISM = "parallelism";
    // Room
    public static final String DATABASE_ROOM_NAME = "database_room_name";

    public static final String CUMULATIVE_ELAPSED_TIME_PREF_KEY_2KT = "cumulative_elapsed_time_2kt";
    public static final String CUMULATIVE_ELAPSED_TIME_PREF_KEY_3KT = "cumulative_elapsed_time_3kt";
    public static final String CUMULATIVE_ELAPSED_TIME_PREF_KEY_CUSTOM = "cumulative_elapsed_time_custom";

    public static final String WORKREQUEST_ID_2KT = "workRequest_id_2kt";
    public static final String WORKREQUEST_ID_3KT = "workRequest_id_3kt";
    public static final String WORKREQUEST_ID_CUSTOM = "workRequest_id_custom";
    public static final String KEY_CUSTOM_CRAWL_STRING = "key_custom_crawl_string";
    // Private constructor to prevent instantiation
    private AppConstants() {
        // This class is not meant to be instantiated.
    }
}