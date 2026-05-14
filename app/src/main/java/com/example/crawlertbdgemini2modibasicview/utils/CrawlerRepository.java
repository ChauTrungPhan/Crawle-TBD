package com.example.crawlertbdgemini2modibasicview.utils;

import android.content.Context;
import android.database.DatabaseUtils;

import com.example.crawlertbdgemini2modibasicview.DBHelperThuoc;

public class CrawlerRepository {
    private final DBHelperThuoc dbHelper;

    public CrawlerRepository(Context context) {
        // Luôn sử dụng ApplicationContext để tránh rò rỉ bộ nhớ
        this.dbHelper = DBHelperThuoc.getInstance(context.getApplicationContext());
    }

    /**
     * Kiểm tra xem còn URL nào chưa được xử lý (status = 0)
     */
    public boolean hasPendingUrls(CrawlType crawlType) {
        long count = DatabaseUtils.longForQuery(
                dbHelper.getReadableDatabase(),
                "SELECT COUNT(*) FROM " + crawlType.getUrlQueueTableName() + " WHERE status = 0",
                null
        );
        return count > 0;
    }

    /**
     * Kiểm tra danh sách lỗi
     * Kiểm tra xem có URL bị lỗi không
     */
    public boolean hasErrorUrls(CrawlType crawlType) {
        // Sử dụng hàm đã sửa ở bước trước để tránh lỗi rò rỉ Cursor
        return dbHelper.hasErrorUrls(crawlType);
    }

    // Bạn có thể thêm các hàm delete, reset data vào đây
}
