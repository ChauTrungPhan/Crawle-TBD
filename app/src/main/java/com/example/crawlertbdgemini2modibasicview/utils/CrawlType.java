package com.example.crawlertbdgemini2modibasicview.utils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.crawlertbdgemini2modibasicview.AppConstants;
import com.example.crawlertbdgemini2modibasicview.DBHelperThuoc;
import com.example.crawlertbdgemini2modibasicview.GetKyTuAZ;
import com.example.crawlertbdgemini2modibasicview.UrlInfo;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public enum CrawlType {

    // Thêm dbValue tương ứng với giá trị sẽ lưu vào cột loai_thuoc trong DB
    TWO_CHAR(AppConstants.RADIOURL2KT,
            "Phương pháp Crawl 2 Ký tự",
            "2kt", // <-- loai_thuoc DB value
            // Table Cha
            DBHelperThuoc.TABLE_PARENT_URLS_2KT, // <-- Giữ lại tableName cũ nếu vẫn cần cho DBHelperThuoc hoặc logging
            DBHelperThuoc.TABLE_THUOC_2KT,   // Là bảng initUrlTable
            DBHelperThuoc.TABLE_URLS_QUEUE_2KT,
            PrefKey.TOTAL_URLS_2KT,
            PrefKey.CRAWLED_URLSTART1_2KT_COUNT,
            PrefKey.SUM_CRAWLED_URLS_2KT_COUNT,
            //// Vì Phụ thuộc k0, k1 nên: GetKyTuAZ.getArrayAZ_2KT() KHÔNG KHỞI TẠO kyTuArray ở đây nữa
            //GetKyTuAZ.getArrayAZ_2KT(), //SỬA: Gọi phương thức getter để tránh lỗi nếu mảng chưa khởi tạo
            AppConstants.FILE_EXCEL_2KT_NOEXT,          //"ThuocBietDuoc2kt.xlsx", // Thêm PrefKey cho K0 của 2 ký tự
            PrefKey.K0_2KT,   // Thêm PrefKey cho K1 của 2 ký tự
            PrefKey.K1_2KT,

            PrefKey.WORK_REQUEST_ID_2KT,
            PrefKey.CANCELLED_WORKER_2KT),
    THREE_CHAR(AppConstants.RADIOURL3KT,
            "Phương pháp Crawl 3 Ký tự",
            "3kt", // <-- loai_thuoc DB value
            // Table Cha
            DBHelperThuoc.TABLE_PARENT_URLS_3KT, // <-- Giữ lại tableName cũ
            DBHelperThuoc.TABLE_THUOC_3KT,   // Là bảng initUrlTable
            DBHelperThuoc.TABLE_URLS_QUEUE_3KT,
            PrefKey.TOTAL_URLS_3KT,
            PrefKey.CRAWLED_URLSTART1_3KT_COUNT,
            PrefKey.SUM_CRAWLED_URLS_3KT_COUNT,
            //GetKyTuAZ.getArrayAZ_3KT(), // Vì Phụ thuộc k0, k1 nên: KHÔNG KHỞI TẠO kyTuArray ở đây nữa
            AppConstants.FILE_EXCEL_3KT_NOEXT,  //"ThuocBietDuoc3kt.xlsx", // Thêm PrefKey cho K0 của 3 ký tự
            PrefKey.K0_3KT,   // Thêm PrefKey cho K1 của 3 ký tự
            PrefKey.K1_3KT,

            PrefKey.WORK_REQUEST_ID_3KT,
            PrefKey.CANCELLED_WORKER_3KT),
    CUSTOM_STRING(AppConstants.RADIOURLTUYCHINH,
            "Phương pháp Crawl Tùy chỉnh",
            "custom", // <-- loai_thuoc DB value
            // Table Cha
            DBHelperThuoc.TABLE_PARENT_URLS_CUSTOM, // <-- Giữ lại tableName cũ
            DBHelperThuoc.TABLE_THUOC_CUSTOM,   // Là bảng initUrlTable
            DBHelperThuoc.TABLE_URLS_QUEUE_CUSTOM,
            PrefKey.TOTAL_URLS_CUSTOM_STRING,
            PrefKey.CRAWLED_URLSTART1_CUSTOM_STRING_COUNT,
            PrefKey.SUM_CRAWLED_URLS_CUSTOM_STRING_COUNT,
            //null,       // Không có kyTuArray cố định. // Vì Phụ thuộc k0, k1 nên: KHÔNG KHỞI TẠO kyTuArray ở đây nữa
            AppConstants.FILE_EXCEL_CUSTOM_NOEXT,  //"ThuocBietDuocTuyChinh.xlsx", // Thêm PrefKey cho K0 của tùy chỉnh
            PrefKey.K0_CUSTOM,   // Thêm PrefKey cho K1 của tùy chỉnh
            PrefKey.K1_CUSTOM,

            PrefKey.WORK_REQUEST_ID_CUSTOM,
            PrefKey.CANCELLED_WORKER_CUSTOM);

//    private static final Logger log = LogManager.getLogger(CrawlType.class);
    private final String settingIdName;
    private final String description;
    private final String dbType; // <-- Thuộc tính mới để lưu giá trị vào DB
    private final String tableParentUrls;
    private final String tableThuoc; // <-- Đổi tên tableName thành tableThuoc nếu không còn dùng cho Room
    private final String urlQueueTableName;
    private final PrefKey totalUrlsPrefKey;
    private final PrefKey crawledUrlStart1sCountPrefKey;
    private final PrefKey crawledUrlsCountPrefKey;
    private final String excelFileName;
    //private String[] kyTuArray;
    private final PrefKey k0PrefKey; // New
    private final PrefKey k1PrefKey; // New
    private final PrefKey workIdPrefKey;
    private final PrefKey cancelledWorkerPrefKey;

    CrawlType(String settingIdName, String description, String dbType,
              String tableParentUrls, String tableThuoc, String urlQueueTableName, // <-- Cập nhật constructor
              PrefKey totalUrlsPrefKey, PrefKey crawledUrlStart1sCountPrefKey,
              PrefKey crawledUrlsCountPrefKey, String excelFileName,
              PrefKey k0PrefKey, PrefKey k1PrefKey, PrefKey workIdPrefKey, PrefKey cancelledWorkerPrefKey) {
        this.settingIdName = settingIdName;
        this.description = description;
        this.dbType = dbType; // <-- Gán giá trị mới
        this.tableParentUrls = tableParentUrls;

        this.tableThuoc = tableThuoc; // <-- Gán giá trị thuốc
        this.urlQueueTableName = urlQueueTableName; // Là initUrlTable
        this.totalUrlsPrefKey = totalUrlsPrefKey;
        this.crawledUrlStart1sCountPrefKey = crawledUrlStart1sCountPrefKey;
        this.crawledUrlsCountPrefKey = crawledUrlsCountPrefKey;
        this.excelFileName = excelFileName;
        //this.kyTuArray = kyTuArray;
        this.k0PrefKey = k0PrefKey;
        this.k1PrefKey = k1PrefKey;
        this.workIdPrefKey = workIdPrefKey;
        this.cancelledWorkerPrefKey = cancelledWorkerPrefKey;
    }

    public static CrawlType fromSettingIdName(String idName) {
        for (CrawlType type : values()) {
            if (type.settingIdName.equalsIgnoreCase(idName)) {
                return type;
            }
        }
        return TWO_CHAR;    // Mặc Định
    }

    // Setter
//    public void setKyTuArray(CrawlType crawlType, int k0, int k1) {
//        kyTuArray = Arrays.copyOfRange(getKyTuArray(), k0, k1);
//    }

    // Getters
    public String getSettingIdName() { return settingIdName; }
    public String getDescription() { return description; }
    public String getDbType() { return dbType; } // <-- Getter cho giá trị DB
    public String getParentTableUrls() { return tableParentUrls; } // <-- Getter
    public String getTableThuocName() { return tableThuoc; } // <-- Getter cho tên bảng cũ
    public String getUrlQueueTableName() { return urlQueueTableName; }
    public PrefKey getTotalUrlsPrefKey() { return totalUrlsPrefKey; }
    public PrefKey getCrawledUrlStart1sCountPrefKey() { return crawledUrlStart1sCountPrefKey; }
    public PrefKey getCrawledUrlsCountPrefKey() { return crawledUrlsCountPrefKey; }
    public String getExcelFileName() { return excelFileName; }
    //public String[] getKyTuArray() { return kyTuArray; }
    // New getters for k0PrefKey and k1PrefKey
    public PrefKey getK0PrefKey() { return k0PrefKey; }
    public PrefKey getK1PrefKey() { return k1PrefKey; }
    public PrefKey getWorkIdPrefKey() { return workIdPrefKey; }
    public PrefKey getCancelledWorkerPrefKey() { return cancelledWorkerPrefKey; }


    // Phương thức mới để lấy kyTuArray dựa trên k0 và k1 từ SettingsRepository
    public String[] getKyTuArrayK0K1(SettingsRepository settingsRepository) {
        if (this == TWO_CHAR || this == THREE_CHAR) {
            int k0 = settingsRepository.getK0(this.getK0PrefKey()); // Đảm bảo getK0 nhận PrefKey
            int k1 = settingsRepository.getK1(this.getK1PrefKey(), ""); // Đảm bảo getK1 nhận PrefKey
            return GetKyTuAZ.getArrayAZk0k1(settingsRepository);

        } else if (this == CUSTOM_STRING) {
            // Đối với CUSTOM_STRING, mảng ký tự được lấy từ chuỗi tùy chỉnh
            String customString = settingsRepository.getCustomCrawlString();
            return customString.split(",\\s*"); // Tách chuỗi bằng dấu phẩy và khoảng trắng
        }
        return new String[0]; // Trả về mảng rỗng nếu không khớp
    }


    // Thêm các PrefKey vào SettingsRepository để truy cập K0 và K1 cụ thể cho từng loại
    public int getTotalKyTuCount(SettingsRepository settingsRepository) {
        if (this == TWO_CHAR || this == THREE_CHAR) {
            int k0 = settingsRepository.getK0(this.getK0PrefKey());
            int k1 = settingsRepository.getK1(this.getK1PrefKey(), "");
            return (k1 - k0); // Tổng số ký tự
        } else if (this == CUSTOM_STRING) {
            String customString = settingsRepository.getCustomCrawlString();
            return customString.split(",\\s*").length;
        }
        return 0;
    }

}