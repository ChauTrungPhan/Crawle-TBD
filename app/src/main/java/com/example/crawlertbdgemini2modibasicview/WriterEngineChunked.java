package com.example.crawlertbdgemini2modibasicview;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WriterEngineChunked extends Thread {
    private final BlockingQueue<Packet> queue;
    private final SQLiteDatabase db;
    private final int CHUNK_SIZE;
    public final AtomicBoolean writerBusy = new AtomicBoolean(false);
    //private volatile boolean running = true;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Câu lệnh chuẩn bị (prepared)
    private SQLiteStatement stmtUpsertUrlParent;      // upsert UrlInfo
    private SQLiteStatement stmtSelectUrlId;    // lấy id UrlInfo theo url
    private SQLiteStatement stmtUpsertThuocChild;    // upsert Thuoc
    private SQLiteStatement stmtUpdateUrlQueue;
    private SQLiteStatement stmtUpdateCheckpoint;

    public WriterEngineChunked(CrawlType crawlType, SQLiteDatabase db,
                               BlockingQueue<Packet> queue,
                               int chunkSize) {
        // XEM
        if (!db.isOpen()) {
            Log.d("WriterEngineChunked", "flushBatch: db.isOpen() = " + db.isOpen());
        }
        // END XEM
        this.db = db;
        this.queue = queue;
        this.CHUNK_SIZE = Math.max(1, chunkSize);

        // Chuẩn bị statement insert, update
        prepareStatements(crawlType);
    }

    private void prepareStatements(CrawlType crawlType ) {
        String tableParent;     // Table Cha
        String tableThuoc;      // Table Con
        String tableUrlsQueue;  // Table Urls Queue
        tableThuoc = crawlType.getTableThuocName();
        tableUrlsQueue = crawlType.getUrlQueueTableName();
        if (crawlType.getSettingIdName().toLowerCase().endsWith("2kt")) {
            tableParent = DBHelperThuoc.TABLE_PARENT_URLS_2KT;
            //tableThuoc = DBHelperThuoc.TABLE_THUOC_2KT;
            //tableUrlsQueue = DBHelperThuoc.TABLE_URLS_QUEUE_2KT;

        } else if (crawlType.getSettingIdName().toLowerCase().endsWith("3kt")) {
            tableParent = DBHelperThuoc.TABLE_PARENT_URLS_3KT;
//            tableThuoc = DBHelperThuoc.TABLE_THUOC_3KT;
//            tableUrlsQueue = DBHelperThuoc.TABLE_URLS_QUEUE_3KT;
        } else {
            tableParent = DBHelperThuoc.TABLE_PARENT_URLS_CUSTOM;
//            tableThuoc = DBHelperThuoc.TABLE_THUOC_CUSTOM;
//            tableUrlsQueue = DBHelperThuoc.TABLE_URLS_QUEUE_CUSTOM;
        }

        // UrlInfo UPSERT (Android SQLite hỗ trợ UPSERT)
        // Insert vào bản cha: 7 cột
        String sqlUpsertUrlParent =
                "INSERT INTO " + tableParent+"(" +
                        DBHelperThuoc.ID_URL + ", " +
                        DBHelperThuoc.LEVEL + ", " +
                        //DBHelperThuoc.KY_TU_SEARCH + "," +  // Cần Không, chỉ là giá tr trung gian của url

                        DBHelperThuoc.URL + ", " +  // link, Hyperlink: sẽ lấy được KY_TU_SEARCH

                        DBHelperThuoc.MA_THUOC + ", " +
                        DBHelperThuoc.STATUS + ", " +
                        DBHelperThuoc.GHI_CHU + ", " +
                        DBHelperThuoc.INDEX_COLOR + ", " +

                        DBHelperThuoc.CREATED_AT + ", " +   // Khi insert
                        DBHelperThuoc.LAST_UPDATED + ") " +
                        "VALUES (" +
                        "?, ?, " +
                        "?, " +
                        "?, ?, ?, ?, " +
                        "?, ?) " +   // THAY CURRENT_TIMESTAMP = giây
                        "ON CONFLICT(url) DO UPDATE SET " +
                        //DBHelperThuoc.PARENT_ID + " = excluded." + DBHelperThuoc.PARENT_ID + ", " +
                        //DBHelperThuoc.LEVEL + " = excluded." + DBHelperThuoc.LEVEL + ", " +

                        DBHelperThuoc.MA_THUOC + " = excluded." + DBHelperThuoc.MA_THUOC + ", " +
                        DBHelperThuoc.STATUS + " = excluded." + DBHelperThuoc.STATUS + ", " +
                        DBHelperThuoc.GHI_CHU + " = excluded." + DBHelperThuoc.GHI_CHU + ", " +
                        DBHelperThuoc.INDEX_COLOR + " = excluded." + DBHelperThuoc.INDEX_COLOR + ", " +
                        DBHelperThuoc.LAST_UPDATED + " = excluded." + DBHelperThuoc.LAST_UPDATED ;  // Dành cho update, giữ nguyên CREATED_AT

        stmtUpsertUrlParent = db.compileStatement(sqlUpsertUrlParent);

        stmtSelectUrlId = db.compileStatement(
                "SELECT " + DBHelperThuoc.ID + " FROM " + tableParent + " WHERE url = ?");

        // Bảng con: Thuoc UPSERT: cập nhật các trường khi trùng (url_Id, ma_thuoc): ThuocSQLITE: có 26 cột, bảng con có 27 cột
        String sqlUpsertThuocChild = "INSERT INTO " + tableThuoc + " (" +
                DBHelperThuoc.ID_URL + ", " +

//                DBHelperThuoc.PARENT_ID + ", " +
//                DBHelperThuoc.LEVEL + ", " +
//                DBHelperThuoc.KY_TU_SEARCH + ", " +

                DBHelperThuoc.MA_THUOC + ", " +

                DBHelperThuoc.MA_THUOC_LINK + ", " +
                DBHelperThuoc.TEN_THUOC + ", " +
                DBHelperThuoc.THANH_PHAN + ", " +
                DBHelperThuoc.THANH_PHAN_LINK + ", " +
                DBHelperThuoc.NHOM_THUOC + ", " +

                DBHelperThuoc.NHOM_THUOC_LINK + ", " +
                DBHelperThuoc.DANG_THUOC + ", " +
                DBHelperThuoc.DANG_THUOC_LINK + ", " +
                DBHelperThuoc.SAN_XUAT + ", " +
                DBHelperThuoc.SAN_XUAT_LINK + ", " +

                DBHelperThuoc.DANG_KY + ", " +
                DBHelperThuoc.DANG_KY_LINK + ", " +
                DBHelperThuoc.PHAN_PHOI + ", " +
                DBHelperThuoc.PHAN_PHOI_LINK + ", " +
                DBHelperThuoc.SDK + ", " +

                DBHelperThuoc.SDK_LINK + ", " +
                DBHelperThuoc.CAC_THUOC + ", " +
                DBHelperThuoc.CAC_THUOC_LINK + ", " +
//                DBHelperThuoc.GHI_CHU + ", " +          //Bảng con đã bỏ
//                DBHelperThuoc.INDEX_COLOR + ", " +      //Bảng con đã bỏ

                DBHelperThuoc.CREATED_AT + ", " +   // Dành cho insert
                DBHelperThuoc.LAST_UPDATED + ") " +
                "VALUES (" +
                "?, " +
                "?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, " +
                "?, ?) " +
                "ON CONFLICT(" + DBHelperThuoc.MA_THUOC + ") " +
                "DO UPDATE SET " +
                DBHelperThuoc.ID_URL + " = excluded." + DBHelperThuoc.ID_URL + ", " +
//                DBHelperThuoc.PARENT_ID + " = excluded." + DBHelperThuoc.PARENT_ID + ", " +   //Bảng con đã bỏ
//                DBHelperThuoc.LEVEL + " = excluded." + DBHelperThuoc.LEVEL + ", " +           //Bảng con đã bỏ
//                DBHelperThuoc.KY_TU_SEARCH + " = excluded." + DBHelperThuoc.KY_TU_SEARCH + ", " +   //Bảng con đã bỏ
                DBHelperThuoc.MA_THUOC + " = excluded." + DBHelperThuoc.MA_THUOC + ", " +

                DBHelperThuoc.MA_THUOC_LINK + " = excluded." + DBHelperThuoc.MA_THUOC_LINK + ", " +
                DBHelperThuoc.TEN_THUOC + " = excluded." + DBHelperThuoc.TEN_THUOC + ", " +
                DBHelperThuoc.THANH_PHAN + " = excluded." + DBHelperThuoc.THANH_PHAN + ", " +
                DBHelperThuoc.THANH_PHAN_LINK + " = excluded." + DBHelperThuoc.THANH_PHAN_LINK + ", " + // Đã sửa tên cột ở đây
                DBHelperThuoc.NHOM_THUOC + " = excluded." + DBHelperThuoc.NHOM_THUOC + ", " +

                DBHelperThuoc.NHOM_THUOC_LINK + " = excluded." + DBHelperThuoc.NHOM_THUOC_LINK + ", " +
                DBHelperThuoc.DANG_THUOC + " = excluded." + DBHelperThuoc.DANG_THUOC + ", " +
                DBHelperThuoc.DANG_THUOC_LINK + " = excluded." + DBHelperThuoc.DANG_THUOC_LINK + ", " +
                DBHelperThuoc.SAN_XUAT + " = excluded." + DBHelperThuoc.SAN_XUAT + ", " +
                DBHelperThuoc.SAN_XUAT_LINK + " = excluded." + DBHelperThuoc.SAN_XUAT_LINK + ", " +

                DBHelperThuoc.DANG_KY + " = excluded." + DBHelperThuoc.DANG_KY + ", " +
                DBHelperThuoc.DANG_KY_LINK + " = excluded." + DBHelperThuoc.DANG_KY_LINK + ", " +
                DBHelperThuoc.PHAN_PHOI + " = excluded." + DBHelperThuoc.PHAN_PHOI + ", " +
                DBHelperThuoc.PHAN_PHOI_LINK + " = excluded." + DBHelperThuoc.PHAN_PHOI_LINK + ", " +
                DBHelperThuoc.SDK + " = excluded." + DBHelperThuoc.SDK + ", " +

                DBHelperThuoc.SDK_LINK + " = excluded." + DBHelperThuoc.SDK_LINK + ", " +
                DBHelperThuoc.CAC_THUOC + " = excluded." + DBHelperThuoc.CAC_THUOC + ", " +
                DBHelperThuoc.CAC_THUOC_LINK + " = excluded." + DBHelperThuoc.CAC_THUOC_LINK + ", " +
//                DBHelperThuoc.GHI_CHU + " = excluded." + DBHelperThuoc.GHI_CHU + ", " +             //Bảng con đã bỏ
//                DBHelperThuoc.INDEX_COLOR + " = excluded." + DBHelperThuoc.INDEX_COLOR + ", " +     //Bảng con đã bỏ

                DBHelperThuoc.LAST_UPDATED + " = excluded." + DBHelperThuoc.LAST_UPDATED;   // Dành cho update, giữ nguyên CREATED_AT


        stmtUpsertThuocChild = db.compileStatement(sqlUpsertThuocChild);

        //sqlUpsertUrlQueue: chỉ update: STATUS, ERR_MESSAGE, LAST_RECORD_INDEX, LAST_ACCESSED
        stmtUpdateUrlQueue = db.compileStatement(
                "UPDATE " + tableUrlsQueue+ " SET " + DBHelperThuoc.STATUS+ "=?, " +
                        DBHelperThuoc.ERR_MESSAGE + "= ?, " + DBHelperThuoc.LAST_RECORD_INDEX + "= ?, " +
                        DBHelperThuoc.LAST_ACCESSED + "=?");

        stmtUpsertUrlParent = db.compileStatement(sqlUpsertUrlParent);

        stmtUpdateCheckpoint = db.compileStatement(
                "UPDATE CrawlCheckpoint SET last_flush_seq = last_flush_seq + ?, " +
                        "last_flush_time = ?, pending_in_queue = ?");
    }

    @Override public void run() {
        // XEM
        if (!db.isOpen()) {
            Log.d("WriterEngineChunked", "flushBatch: db.isOpen() = " + db.isOpen());
        }
        // END XEM

        //final ArrayList<Packet> buf = new ArrayList<>(CHUNK_SIZE);
        List<Packet> buf = new ArrayList<>(CHUNK_SIZE);


        android.util.Log.d("WriterEngine", "Chunked writer started (chunk=" + CHUNK_SIZE + ")");
        try {
            while (running.get() || !queue.isEmpty()) {
                //1. Mới dùng queue.poll(200, TimeUnit.MILLISECONDS);, KGONG DÙNG queue.take()
                // Chờ tối đa 200ms để gom packet
                Packet p = queue.poll(200, TimeUnit.MILLISECONDS);
                if (p != null) {
                    buf.add(p);
                }

                // Đủ batch hoặc lúc shutdown còn sót
                if (buf.size() >= CHUNK_SIZE || (!running.get() && !buf.isEmpty())) {
                    flushBatch(buf);
                    buf.clear();
                }


                //2. Đoan sau (cũ) được thay = flushBatch
                // chờ ít nhất 1 Packet (chờ 1 item (block))
//                Packet first = queue.take();    // ít nhất có 1
//                if (!running.get()) break;
//                buf.clear();
//                buf.add(first);
//                // gom thêm tối đa CHUNK_SIZE-1: drain thêm đến CHUNK_SIZE (Nếu không có đủ vẫn thực hiện)
//                queue.drainTo(buf, CHUNK_SIZE - 1); // Nếu không có đủ vẫn thực hiện
//
//                long start = System.currentTimeMillis();    // Để xem
//
//                writerBusy.set(true);
//                db.beginTransaction();
//                try {
//                    // Ghi lần lượt từng URL (cha) + con
//                    for (Packet p : buf) {
//                        // THÊM UPDATE TABLE QUEUE
//                        stmtUpdateUrlQueue.clearBindings();
//                        stmtUpdateUrlQueue.bindLong(1, p.urlInfo.getStatus());
//                        stmtUpdateUrlQueue.bindString(2, p.urlInfo.getGhiChu()==null?"":p.urlInfo.getGhiChu());
//                        stmtUpdateUrlQueue.bindLong(3, p.urlInfo.getLastRecordIndex());
//                        stmtUpdateUrlQueue.bindLong(4, p.urlInfo.getLast_attempt_at());
//
//                        stmtUpdateUrlQueue.executeUpdateDelete();
//                        // END THÊM
//                        long urlId = bindUpsertUrlAndGetId(p.urlInfo);  //urlInfo: bảng cha
//                        // Xem
//                        Log.d("WriterEngineChunked",
//                                " └─ Packet url=" + p.urlInfo.getUrl()
//                                        + " -> " + (p.records != null ? p.records.size() : 0) + " records");
//
//                        Log.d("WriterEngineChunked", "run: urlInfo.getUrl() = " + p.urlInfo.getUrl());
//                        if ((p.urlInfo.getUrl().endsWith("=1")) ||
//                                        p.urlInfo.getLevel() == 1) {
//                            int stop = 0;
//                        }
//
//                        if (!p.records.isEmpty()) {
//                            for (ThuocSQLite t : p.records) {
//                                // Xem
//                                Log.d("WriterEngineChunked", " run: t.ma_thuoc=" + t.ma_thuoc +
//                                        "\nt.ten_thuoc="+t.ten_thuoc);
//                                if(t.ma_thuoc.equals("3197") ||
//                                        t.ten_thuoc.contains("Broncho-Vaxom Children")) {
//                                    int stop = 0;
//                                }
//
//                                bindThuocAndExecute(urlId, t);
//                            }
//                        }
//                    }
//
//                    db.setTransactionSuccessful();
//                } catch (Exception ex) {
//                    android.util.Log.e("WriterEngineChunked", "TX error: " + ex.getMessage(), ex);
//                } finally {
//                    db.endTransaction();
//                    writerBusy.set(false);
//                    // Xem
//                    // 👉 Tính tổng record trong batch
//                    int totalRecords = 0;
//                    for (Packet p : buf) {
//                        totalRecords += (p.records != null ? p.records.size() : 0);
//                    }
//
//                    long dur = System.currentTimeMillis() - start;
//                    // 👉 Log kết quả batch
//                    Log.d("WriterEngineChunked",
//                            "✅ Commit batch: " + buf.size() + " packets, "
//                                    + totalRecords + " records, in " + dur + " ms");
//
//                    // end Xem
//
//                }
//
//
//                // Xem
//                long dur = System.currentTimeMillis() - start;
//                Log.d("WriterEngineChunked", "✅ Commit batch Packet: " + buf.size() + " urls in " + dur + " ms");
                //Log.d(TAG, "✅ Commit batch Packet: " + buf.size() + " urls in " + duration + " ms");

                // end 2. cũ

                // checkpoint app-level (không phải WAL checkpoint)
                try {
                    stmtUpdateCheckpoint.clearBindings();
                    stmtUpdateCheckpoint.bindLong(1, buf.size());
                    stmtUpdateCheckpoint.bindLong(2, System.currentTimeMillis());
                    stmtUpdateCheckpoint.bindLong(3, queue.size());
                    stmtUpdateCheckpoint.executeUpdateDelete();
                } catch (Exception e) {
                    android.util.Log.w("WriterEngine", "Update checkpoint failed", e);
                }
            }

            // ✅ Flush nốt khi shutdown
            if (!buf.isEmpty()) {
                flushBatch(buf);
                buf.clear();
            }

            //Với cách này, CrawlRuntime.stop() chỉ cần:
            // writer.shutdown(); writer.join(); là đảm bảo Writer đã flush hết dữ liệu còn lại.

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            Log.d("WriterEngineChunked", "Writer stopped (final flush).");

        }
    }

    private long bindUpsertUrlAndGetId(UrlInfo u) { // Ghi vào table url cha
        // UPSERT UrlInfo
        stmtUpsertUrlParent.clearBindings();
        // 9 cột
        stmtUpsertUrlParent.bindLong(1, u.getParentId());
        stmtUpsertUrlParent.bindLong(2, u.getLevel());
        //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
        //stmtUpsertUrlParent.bindString(3, "key" + u.getUrl().split("key")[1]);  // là KY_TU_SEARCH
        stmtUpsertUrlParent.bindString(3, u.getUrl());  // KHÓA CHÍNH: u.getUrl() KHÔNG THỂ NULL
        // Nếu u bỏ maThuocP, thì Lấy maThuocP = "https://www..." +"key" + u.getUrl().split("key")[1];
        stmtUpsertUrlParent.bindString(4, Utils.getMaThuocUrlCha(u.getUrl()));
        //stmtUpsertUrlParent.bindString(4, u.getMaThuoc_P());    // getMaThuoc_P: có thể lấy từ url
        stmtUpsertUrlParent.bindLong(5, u.getStatus());
        if (u.getGhiChu() != null && !u.getGhiChu().isEmpty()) stmtUpsertUrlParent.bindString(6, u.getGhiChu());
        else stmtUpsertUrlParent.bindNull(6); //getErrorMessage có thể đưa vào ghi chú: cột 5 là GHI_CHU
        stmtUpsertUrlParent.bindLong(7, u.getIndexColor());
        stmtUpsertUrlParent.bindLong(8, System.currentTimeMillis() / 1000);   //created_at : sert
        stmtUpsertUrlParent.bindLong(9, System.currentTimeMillis() / 1000);   //last_updated
        //
        stmtUpsertUrlParent.execute();

        // Lấy id (safe cho cả INSERT mới & UPDATE)
        stmtSelectUrlId.clearBindings();    // xóa cũ
        stmtSelectUrlId.bindString(1, u.getUrl());  //bind lại cột 1 mới lấy được simpleQueryForLong
        return stmtSelectUrlId.simpleQueryForLong();

    }

    private void bindThuocAndExecute(long urlId, ThuocSQLite t) {
        stmtUpsertThuocChild.clearBindings();
        stmtUpsertThuocChild.bindLong(1, urlId);
        //
//        stmtUpsertThuocChild.bindLong(2, t.parent_id);
//        stmtUpsertThuocChild.bindLong(3, t.level);
//        stmtUpsertThuocChild.bindString(4, safe(t.ky_tu_search));
        //
        stmtUpsertThuocChild.bindString(2, safe(t.ma_thuoc));

        stmtUpsertThuocChild.bindString(3, safe(t.ma_thuoc_link));
        stmtUpsertThuocChild.bindString(4, safe(t.ten_thuoc));
        stmtUpsertThuocChild.bindString(5, safe(t.thanh_phan));
        stmtUpsertThuocChild.bindString(6, safe(t.thanh_phan_link));
        stmtUpsertThuocChild.bindString(7, safe(t.nhom_thuoc));
        stmtUpsertThuocChild.bindString(8, safe(t.nhom_thuoc_link));
        stmtUpsertThuocChild.bindString(9, safe(t.dang_thuoc));
        stmtUpsertThuocChild.bindString(10, safe(t.dang_thuoc_link));
        stmtUpsertThuocChild.bindString(11, safe(t.san_xuat));
        stmtUpsertThuocChild.bindString(12, safe(t.san_xuat_link));
        stmtUpsertThuocChild.bindString(13, safe(t.dang_ky));
        stmtUpsertThuocChild.bindString(14, safe(t.dang_ky_link));
        stmtUpsertThuocChild.bindString(15, safe(t.phan_phoi));
        stmtUpsertThuocChild.bindString(16, safe(t.phan_phoi_link));
        stmtUpsertThuocChild.bindString(17, safe(t.sdk));
        stmtUpsertThuocChild.bindString(18, safe(t.sdk_link));
        stmtUpsertThuocChild.bindString(19, safe(t.cac_thuoc));
        stmtUpsertThuocChild.bindString(20, safe(t.cac_thuoc_link));
//        stmtUpsertThuocChild.bindString(21, safe(t.ghi_chu));   //Bảng con đã bỏ
//        stmtUpsertThuocChild.bindLong(22, t.index_color);       //Bảng con đã bỏ

        //stmtUpsertThuocChild.bindLong(26, t.updatedAt > 0 ? t.updatedAt : System.currentTimeMillis());   // insert
        stmtUpsertThuocChild.bindLong(21, System.currentTimeMillis()/1000);   // Lưu giây(seconds) insert: CREATED_AT: LẦN ĐẦU TẠO
        stmtUpsertThuocChild.bindLong(22, System.currentTimeMillis()/1000);   // Lưu giây(seconds) Last Update: CẬP NHẬT
        stmtUpsertThuocChild.execute();
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    public void shutdown() {
        /**Ý tưởng: khi gọi shutdown() thì:
         * Set cờ running = false.
         * Không interrupt ngay (để vòng lặp poll kết thúc một cách tự nhiên).
         * Sau vòng lặp, commit nốt batch còn lại.
         * Log "Writer stopped (final flush).".
         */

        //running = false;
        running.set(false);
        // không cần interrupt, cứ để poll(200ms) timeout rồi thoát

        // Sau đây là interrupt (gốc có)
        // giải phóng take(): đẩy 1 dummy Packet
        // Lưu giây(seconds)
//        try { queue.offer(new Packet(new UrlInfo("__shutdown__", 0, 0, null,0, null, 0,
//                -1, System.currentTimeMillis()/1000), Collections.emptyList())); } catch (Exception ignore) {}
//
        // end gốc
    }

    // MỚI: THAY CHO ĐỌAN CŨ
    private void flushBatch(List<Packet> buf) {
        if (buf == null || buf.isEmpty()) return;

        long start = System.currentTimeMillis();
        // XEM
        if (!db.isOpen()) {
            Log.d("WriterEngineChunked", "flushBatch: db.isOpen() = " + db.isOpen());
        }
        // END XEM

        db.beginTransaction();
        writerBusy.set(true);

        int totalRecords = 0;

        try {
            // Duyệt từng packet trong batch: // Ghi lần lượt từng URL (cha) + con
            for (Packet p : buf) {
                if (p == null || p.records == null || p.records.isEmpty()) continue;
                // THÊM UPDATE TABLE QUEUE
                stmtUpdateUrlQueue.clearBindings();
                stmtUpdateUrlQueue.bindLong(1, p.urlInfo.getStatus());
                stmtUpdateUrlQueue.bindString(2, p.urlInfo.getGhiChu()==null?"":p.urlInfo.getGhiChu());
                stmtUpdateUrlQueue.bindLong(3, p.urlInfo.getLastRecordIndex());
                stmtUpdateUrlQueue.bindLong(4, p.urlInfo.getLast_attempt_at());

                stmtUpdateUrlQueue.executeUpdateDelete();
                // END THÊM
                long urlId = bindUpsertUrlAndGetId(p.urlInfo);  //urlInfo: Ghi bảng cha và trả về url_id
                // Xem
                Log.d("WriterEngineChunked",
                        " └─ Packet url=" + p.urlInfo.getUrl()
                                + " -> " + (p.records != null ? p.records.size() : 0) + " records");

                Log.d("WriterEngineChunked", "run: urlInfo.getUrl() = " + p.urlInfo.getUrl());

                if ((p.urlInfo.getUrl().endsWith("=1")) ||
                        p.urlInfo.getLevel() == 1) {
                    int stop = 0;
                }
                // end XEM

                totalRecords += p.records.size();

                if (!p.records.isEmpty()) {
                    // Insert từng record trong packet
                    for (ThuocSQLite t : p.records) {
                        try {
                            // Xem
                            Log.d("WriterEngineChunked", " run: t.ma_thuoc=" + t.ma_thuoc +
                                    "\nt.ten_thuoc="+t.ten_thuoc);
                            if(t.ma_thuoc.equals("3197") ||
                                    t.ten_thuoc.contains("Broncho-Vaxom Children")) {
                                int stop = 0;
                            }

                            bindThuocAndExecute(urlId, t);
                        } catch (Exception e) {
                            Log.w("WriterEngineChunked", "Skip duplicate or invalid record: " + t.ma_thuoc, e);
                        }
                    }
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e("WriterEngineChunked", "Error while inserting batch", e);

        } finally {
            db.endTransaction();
            writerBusy.set(false);

            long dur = System.currentTimeMillis() - start;

            // ✅ Log kết quả batch
            Log.d("WriterEngineChunked",
                    "✅ Commit batch: " + buf.size() + " packets, "
                            + totalRecords + " records, in " + dur + " ms");
        }
    }

}

