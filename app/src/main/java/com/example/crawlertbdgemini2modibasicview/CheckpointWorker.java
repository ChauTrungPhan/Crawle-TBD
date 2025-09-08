package com.example.crawlertbdgemini2modibasicview;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * ===============================================
 *  CHECKPOINT WORKER (WAL MODE)
 * ===============================================
 *
 *  Sequence Diagram:
 *
 *   CheckpointWorker Thread        SQLite DB (WAL)
 *            │ loop(): sleep(interval)
 *            │───────────────────────> kiểm tra file.db-wal size
 *            │<───────────────────────
 *            │ if (size > limit && isWriterIdle)
 *            │───────────────────────> PRAGMA wal_checkpoint(TRUNCATE)
 *            │<───────────────────────
 *            │ tiếp tục vòng lặp
 *
 * ===============================================
 * - Chỉ chạy checkpoint khi:
 *      1. file.db-wal > walLimitBytes
 *      2. WriterEngine idle + queue empty
 * - Giúp WAL không phình quá lớn → tiết kiệm disk.
 * - Không block Crawler Threads hoặc Writer Thread.
 * ===============================================
 */

public class CheckpointWorker extends Thread {
    private final String dbPath;
    private final Supplier<Boolean> canCheckpoint;
    private final long walSizeLimit;
    private final long checkIntervalMs;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public CheckpointWorker(Context context,
                            Supplier<Boolean> canCheckpoint,
                            long walSizeLimit,
                            long checkIntervalMs) {
        // ✅ Lấy path từ DBHelperThuoc Singleton
        DBHelperThuoc helper = DBHelperThuoc.getInstance(context);
        this.dbPath = helper.getDbPath();
        this.canCheckpoint = canCheckpoint;
        this.walSizeLimit = walSizeLimit;
        this.checkIntervalMs = checkIntervalMs;
        setName("CheckpointWorker");
    }

    @Override
    public void run() {
        File walFile = new File(dbPath + "-wal");
        while (running.get()) {
            try {
                Thread.sleep(checkIntervalMs);

                if (walFile.exists() && walFile.length() > walSizeLimit) {
                    if (canCheckpoint.get()) {
                        checkpointNow();
                    }
                }
            } catch (InterruptedException ignored) {}
        }
    }

    public void stopWorker() {
        running.set(false);
        this.interrupt();
    }

    private void checkpointNow() {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(
                    dbPath, null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)");
        } finally {
            if (db != null) db.close();
        }
    }
}
