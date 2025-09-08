package com.example.crawlertbdgemini2modibasicview.utils;

/**
 * Enum định nghĩa tất cả các khóa (keys) được sử dụng cho SharedPreferences trong ứng dụng.
 * Sử dụng enum giúp đảm bảo an toàn kiểu và tránh lỗi chính tả.
 */
public enum PrefKey {
    // 1. Cho 2KT
    K0_2KT, // K0 cho Crawler 2 ký tự
    K1_2KT, // K1 cho Crawler 2 ký tự
    TOTAL_URLS_2KT,
    CRAWLED_URLSTART1_2KT_COUNT,
    SUM_CRAWLED_URLS_2KT_COUNT,
    WORK_REQUEST_ID_2KT,        // THÊM
    CANCELLED_WORKER_2KT,
    // 2. 3KT
    K0_3KT, // K0 cho Crawler 3 ký tự
    K1_3KT, // K1 cho Crawler 3 ký tự
    TOTAL_URLS_3KT,
    CRAWLED_URLSTART1_3KT_COUNT,
    SUM_CRAWLED_URLS_3KT_COUNT,
    WORK_REQUEST_ID_3KT,
    CANCELLED_WORKER_3KT,

    // 3. CUSTOM
    K0_CUSTOM,  // K0 cho Crawler CUSTOM ký tự
    K1_CUSTOM,  // K1 cho Crawler CUSTOM ký tự
    TOTAL_URLS_CUSTOM_STRING,
    CRAWLED_URLSTART1_CUSTOM_STRING_COUNT,
    SUM_CRAWLED_URLS_CUSTOM_STRING_COUNT,
    WORK_REQUEST_ID_CUSTOM,
    CANCELLED_WORKER_CUSTOM,

    // ... (các khóa khác)

    // Cài đặt chung của ứng dụng
    FIRST_RUN, // Xác định lần chạy đầu tiên của ứng dụng
    LAST_EXPORT_PATH, // Đường dẫn đến thư mục xuất file gần nhất
    EXPORT_FORMAT, // Định dạng file xuất (XLSX hay XML)
    ENABLE_NOTIFICATION, // Bật/tắt thông báo
    AUTO_START_PERMISSION, // Trạng thái quyền tự khởi chạy

    // Cài đặt liên quan đến quá trình crawl
    SELECTED_CRAWL_TYPE_ID_NAME, // ID của loại crawl được chọn (radioUrl2kt, radioUrl3kt, radioUrlTuyChinh)
    K0_SETTING, // Giá trị K0 cho cài đặt crawl (nếu được sử dụng)
    K1_SETTING, // Giá trị K1 cho cài đặt crawl (nếu được sử dụng)
    CRAWL_BY_URL_SELECTED, // Xác định liệu crawl có theo URL hay không
    ELAPSED_TIME, // Thời gian đã trôi qua của quá trình crawl trước đó
    CUSTOM_CRAWL_STRING // Chuỗi ký tự tùy chỉnh cho loại crawl CUSTOM_STRING

    // Các khóa liên quan đến tiến độ của từng loại crawl
    // Các khóa này sẽ được lưu trữ trong CrawlType enum để đảm bảo tính đóng gói tốt hơn
    // nhưng vẫn được liệt kê ở đây để quản lý tập trung các PrefKey.
    // Việc truy cập chúng nên thông qua phương thức getter trong CrawlType

}