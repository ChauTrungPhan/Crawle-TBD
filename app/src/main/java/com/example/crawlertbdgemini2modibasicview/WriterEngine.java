package com.example.crawlertbdgemini2modibasicview;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ===============================================
 *  WRITER ENGINE (1 THREAD GHI WAL)
 * ===============================================
 *
 *  Sequence Diagram:
 *
 *   CrawlRuntime             WriterEngine Thread            SQLite DB (WAL)
 *        │ queue.put(batch)         │
 *        │─────────────────────────>│ queue.take()
 *        │                           │────────────────────> beginTransaction()
 *        │                           │
 *        │                           │ for (Thuoc in batch):
 *        │                           │     SELECT maThuoc
 *        │                           │     if !exists → INSERT
 *        │                           │     else → UPDATE
 *        │                           │────────────────────> ghi vào db-wal
 *        │                           │<────────────────────
 *        │                           │ commit
 *        │                           │────────────────────> db-shm sync
 *        │                           │<────────────────────
 *
 * ===============================================
 * - Chỉ 1 luồng duy nhất ghi DB → tránh tranh chấp write lock.
 * - Sử dụng WAL mode → read/write song song được.
 * - Batch transaction → giảm overhead begin/commit nhiều lần.
 * ===============================================
 */

public class WriterEngine {
    private final SQLiteDatabase db;
    private final BlockingQueue<List<ThuocSQLite>> queue;
    final AtomicBoolean writerBusy = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private String tableThuoc;

    public WriterEngine(SQLiteDatabase db, BlockingQueue<List<ThuocSQLite>> q, String tableThuoc) {
        this.db = db;
        this.queue = q;
        this.tableThuoc = tableThuoc;
    }

    public void start() {
        new Thread(this::loop, "WriterThread").start();
    }

    private void loop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                List<ThuocSQLite> batch = queue.take();
                if (batch == null || batch.isEmpty()) continue;

                writerBusy.set(true);
                if (db != null && db.isOpen()) {

                    db.beginTransaction();
                    try {
                        for (ThuocSQLite thuoc : batch) {
                            ContentValues cv = new ContentValues();
                            //cv.put(DBHelperThuoc.PARENT_ID, thuoc.parent_id);
                            //cv.put(DBHelperThuoc.LEVEL, thuoc.level);
                            //cv.put(DBHelperThuoc.KY_TU_SEARCH, thuoc.ky_tu_search);
                            cv.put(DBHelperThuoc.MA_THUOC, thuoc.ma_thuoc);
                            cv.put(DBHelperThuoc.MA_THUOC_LINK, thuoc.ma_thuoc_link);
                            cv.put(DBHelperThuoc.TEN_THUOC, thuoc.ten_thuoc);
                            cv.put(DBHelperThuoc.THANH_PHAN, thuoc.thanh_phan);
                            cv.put(DBHelperThuoc.THANH_PHAN_LINK, thuoc.thanh_phan_link);
                            cv.put(DBHelperThuoc.NHOM_THUOC, thuoc.nhom_thuoc);
                            cv.put(DBHelperThuoc.NHOM_THUOC_LINK, thuoc.nhom_thuoc_link);
                            cv.put(DBHelperThuoc.DANG_THUOC, thuoc.dang_thuoc);
                            cv.put(DBHelperThuoc.DANG_THUOC_LINK, thuoc.dang_thuoc_link);
                            cv.put(DBHelperThuoc.SAN_XUAT, thuoc.san_xuat);
                            cv.put(DBHelperThuoc.SAN_XUAT_LINK, thuoc.san_xuat_link);
                            cv.put(DBHelperThuoc.DANG_KY, thuoc.dang_ky);
                            cv.put(DBHelperThuoc.DANG_KY_LINK, thuoc.dang_ky_link);
                            cv.put(DBHelperThuoc.PHAN_PHOI, thuoc.phan_phoi);
                            cv.put(DBHelperThuoc.PHAN_PHOI_LINK, thuoc.phan_phoi_link);
                            cv.put(DBHelperThuoc.SDK, thuoc.sdk);
                            cv.put(DBHelperThuoc.SDK_LINK, thuoc.sdk_link);
                            cv.put(DBHelperThuoc.CAC_THUOC, thuoc.cac_thuoc);
                            cv.put(DBHelperThuoc.CAC_THUOC_LINK, thuoc.cac_thuoc_link);
                            //cv.put(DBHelperThuoc.URL, thuoc.url);
                            //cv.put(DBHelperThuoc.GHI_CHU, thuoc.ghi_chu);
                            //cv.put(DBHelperThuoc.INDEX_COLOR, thuoc.index_color);
                            cv.put(DBHelperThuoc.CREATED_AT, thuoc.updatedAt);   //Cột thứ 26 (index=25)

//                        if (!isMaThuocExists(thuoc.ma_thuoc)) {
//                            db.insert(tableThuoc, null, cv);
//                        } else {
//                            db.update(tableThuoc, cv,  DBHelperThuoc.MA_THUOC + "=?", new String[]{thuoc.ma_thuoc});
//                        }

                            // KIỂM TRA TẠI SAO
                            if (!db.isOpen()) {
                                Log.e("WriterEngine", "Database is closed! Skipping write...");
                                return;
                            }

                            // code KHÔNG CẦN KIỂM TRA MÃ THUỐC ĐÃ CÓ
                            long affectedRows = db.update(tableThuoc, cv, DBHelperThuoc.MA_THUOC + "=?", new String[]{thuoc.ma_thuoc});
                            if (affectedRows == 0) {
                                db.insert(tableThuoc, null, cv);
                            }

                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                        writerBusy.set(false);
                    }
                } else {
                    Log.w("WriterEngine", "Database is closed, skip writing...");
                    break; // thoát loop nếu DB đã đóng
                }

            } catch (InterruptedException e) {
                Log.e("WriterEngine", "Error writing to DB", e);
                Thread.currentThread().interrupt();
                break;
            }
            try {
                Thread.sleep(100); // tránh loop 100% CPU
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    private boolean isMaThuocExists(String maThuoc) {
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + tableThuoc + " WHERE " + DBHelperThuoc.MA_THUOC + "=? LIMIT 1",
                new String[]{maThuoc})) {
            return c.moveToFirst();
        }
    }

    public void stop() {
        running.set(false);
    }
}

