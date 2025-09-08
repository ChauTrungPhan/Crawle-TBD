package com.example.crawlertbdgemini2modibasicview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement; // <-- Đảm bảo import này tồn tại
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;
import com.example.crawlertbdgemini2modibasicview.utils.Utils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBHelperThuoc extends SQLiteOpenHelper {
    private static final String TAG = "DBHelperThuoc";

    //private static final Lock dbWriteLock = new ReentrantLock();
    private static final ReentrantLock dbWriteLock = new ReentrantLock();
    //Thay ReentrantLock = ReentrantReadWriteLock
    public static final ReadWriteLock dbReadWriteLock = new ReentrantReadWriteLock();
    //Cách sử dụng nhiều khóa THEO tên bảng (table): CHÚ Ý THỨ TỤ TRÁNH BỊ DEADLOCK
//    private static final Lock lock1TableThuoc = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi
//    private static final Lock lock2CompletedUrlsTable = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi
//    private static final Lock lock3InitUrlsTable  = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi


    public static final String DATABASE_NAME = "ThuocBietDuoc";
    private static final int DATABASE_VERSION = AppConstants.DATABASE_VERSION;        //1;
    private static volatile DBHelperThuoc instance;
    //private static DBHelperThuoc instance;
    private SQLiteDatabase db; // single connection tái sử dụng


    // Bảng cha: chứa thông tin url
    //public static final String TABLE_PARENT_URLS = "table_parent_urls"; // Chung: KHI DUNG + THÊM 2KT, 3KT HOẶC CUSTOM
    public static final String TABLE_PARENT_URLS_2KT = "table_parent_urls_2kt";
    public static final String TABLE_PARENT_URLS_3KT = "table_parent_urls_3kt";
    public static final String TABLE_PARENT_URLS_CUSTOM = "table_parent_urls_custom";
    // Các bảng con: chứa thông tin thuốc, LÀ CÁC RECORD
    public static final String TABLE_THUOC_2KT = "table_thuoc_2kt";
    public static final String TABLE_THUOC_3KT = "table_thuoc_3kt";
    public static final String TABLE_THUOC_CUSTOM = "table_thuoc_custom";

    //public static final String CRAWLED_FROM_URL = "CRAWLED_FROM_URL";
    // Đảm bảo các hằng số cột này đã được định nghĩa:
//    public static final String LEVEL = "level";
//    public static final String PARENT_ID = "parent_id";

    // Của Table Parent (cha)
    public static final String PARENT_ID = "parent_id";   //PARENT_ID_OF_CRAWLED_URL
    public static final String LEVEL = "level";   //Hoặc LEVEL_OF_CRAWLED_URL
    public static final String MA_THUOC_P = "ma_thuoc_p";   //Cho bảng cha: TẠO MÃ GIẢ
    public static final String MA_THUOC_LINK_P = "link_ma_thuoc_p"; // Của Cha
    public static final String GHI_CHU = "ghi_chu";
    public static final String LAST_RECORD_INDEX = "last_record_index";
    public static final String INDEX_COLOR = "index_color";
    public static final String LAST_UPDATED = "last_updated";   // INTEGER (UNIX time giây)
    // Của Table Child (Con)
    public static final String MA_THUOC = "ma_thuoc";
    public static final String MA_THUOC_LINK = "ma_thuoc_link";
    public static final String TEN_THUOC = "ten_thuoc";
    public static final String THANH_PHAN = "thanh_phan";
    public static final String THANH_PHAN_LINK = "thanh_phan_link";
    public static final String NHOM_THUOC = "nhom_thuoc";
    public static final String NHOM_THUOC_LINK = "nhom_thuoc_link"; // Đã sửa chính tả
    public static final String DANG_THUOC = "dang_thuoc";
    public static final String DANG_THUOC_LINK = "dang_thuoc_link";
    public static final String SAN_XUAT = "san_xuat";
    public static final String SAN_XUAT_LINK = "san_xuat_link";
    public static final String DANG_KY = "dang_ky";
    public static final String DANG_KY_LINK = "dang_ky_link";
    public static final String PHAN_PHOI = "phan_phoi";
    public static final String PHAN_PHOI_LINK = "phan_phoi_link";
    public static final String SDK = "sdk";
    public static final String SDK_LINK = "sdk_link";
    public static final String CAC_THUOC = "cac_thuoc";
    public static final String CAC_THUOC_LINK = "cac_thuoc_link";

    public static final String CREATED_AT = "created_at";   // INTEGER (UNIX time giây)
    public static final String ERR_MESSAGE = "err_message";

    public static final String TABLE_URLS_QUEUE_2KT = "table_urls_queue_2kt";
    public static final String TABLE_URLS_QUEUE_3KT = "table_urls_queue_3kt";
    public static final String TABLE_URLS_QUEUE_CUSTOM = "table_urls_queue_custom";

    public static final String ID = "id";
    public static final String URL_ID = "url_id";   //FK tham chiếu đến ID (là số nguyên sẽ nhanh nhất, không tốn dữ liệu)
    public static final String URL = "url"; //Con Đã bỏ
    public static final String URL_P = "url_p"; //Cha
    public static final String STATUS = "status";
    public static final String LAST_ACCESSED = "last_accessed";
    public static final String KY_TU_SEARCH = "ky_tu_search";
    private final Context context;

    //1. BẢNG CON: String tạo bản thuốc: Xem như bảng con (child)

    public static final String createThuoc2kt_child  =
            "CREATE TABLE " +  TABLE_THUOC_2KT + "(" +      // %s SẼ ĐƯỢC THAY = BIẾN CHUỖI
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                    URL_ID + " INTEGER NOT NULL, " +   // <-- khóa ngoại: FK tham chiếu tableParent.id (số nguyên sẽ nhanh)
                    //
//                    PARENT_ID + " INTEGER, " +                             //Hoặc: PARENT_ID_OF_CRAWLED_URL
//                    LEVEL + " INTEGER NOT NULL, " +                     //Hoăc: LEVEL_OF_CRAWLED_URL
//                    KY_TU_SEARCH + " TEXT, " +
                    //URL + " TEXT NOT NULL, " +                     // <-- URL làm khóa ngoại: sẽ chậm, tốn ram, do chuỗi dài truy xuất sẽ cậm
                    MA_THUOC + " TEXT UNIQUE NOT NULL, " +

                    MA_THUOC_LINK + " TEXT, " +
                    TEN_THUOC + " TEXT, " +
                    THANH_PHAN + " TEXT, " +
                    THANH_PHAN_LINK + " TEXT, " +
                    NHOM_THUOC + " TEXT, " +

                    NHOM_THUOC_LINK + " TEXT, " +
                    DANG_THUOC + " TEXT, " +
                    DANG_THUOC_LINK + " TEXT, " +
                    SAN_XUAT + " TEXT, " +
                    SAN_XUAT_LINK + " TEXT, " +

                    DANG_KY + " TEXT, " +
                    DANG_KY_LINK + " TEXT, " +
                    PHAN_PHOI + " TEXT, " +
                    PHAN_PHOI_LINK + " TEXT, " +
                    SDK + " TEXT, " +

                    SDK_LINK + " TEXT, " +
                    CAC_THUOC + " TEXT, " +
                    CAC_THUOC_LINK + " TEXT, " +
                    GHI_CHU + " TEXT, " +
                    INDEX_COLOR + " INTEGER DEFAULT 0, " +

                    CREATED_AT + " INTEGER NOT NULL, " +    // java hỗ trợ CURRENT_TIMESTAMP để tự động gán thời gian hiện tại khi một bản ghi được tạo.
                    LAST_UPDATED + " INTEGER NOT NULL, " +
                    //-- khai báo FOREIGN KEY phải đặt cuối cùng (ngoài định nghĩa cột)
                    //"FOREIGN KEY(" + URL +") REFERENCES %s(" + URL + ") ON DELETE CASCADE, " +

                    "FOREIGN KEY("+ URL_ID + ") REFERENCES " + TABLE_PARENT_URLS_2KT + "(" + ID + ") ON DELETE CASCADE, " +
                    //"UNIQUE(" + URL_ID + ", " + MA_THUOC + ")" +  // 1 URL_ID chỉ chứa 1 mã thuốc duy nhất
                    "UNIQUE(" + MA_THUOC + ")" +  // Toàn cầu (Toàn bảng): HỢP LÝ HƠN

                    ");";

    ///
    public static final String CREATE_TABLE_THUOC_CHILD =
            "CREATE TABLE %s (" +      // %s SẼ ĐƯỢC THAY = BIẾN CHUỖI
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                    URL_ID + " INTEGER NOT NULL, " +        // <-- khóa ngoại: FK tham chiếu tableParent.id (số nguyên sẽ nhanh)

//                    PARENT_ID + " INTEGER, " +              // Đưa vào table cha (Hoặc: PARENT_ID_OF_CRAWLED_URL)
//                    LEVEL + " INTEGER NOT NULL, " +         //  Đưa vào table cha (Hoăc: LEVEL_OF_CRAWLED_URL)
//                    KY_TU_SEARCH + " TEXT, " +                //  Đưa vào table cha
                    //URL + " TEXT NOT NULL, " +            // <-- URL làm khóa ngoại: sẽ chậm, tốn ram, do chuỗi dài truy xuất sẽ cậm

                    MA_THUOC + " TEXT UNIQUE NOT NULL, " +

                    MA_THUOC_LINK + " TEXT, " +
                    TEN_THUOC + " TEXT, " +
                    THANH_PHAN + " TEXT, " +
                    THANH_PHAN_LINK + " TEXT, " +
                    NHOM_THUOC + " TEXT, " +

                    NHOM_THUOC_LINK + " TEXT, " +
                    DANG_THUOC + " TEXT, " +
                    DANG_THUOC_LINK + " TEXT, " +
                    SAN_XUAT + " TEXT, " +
                    SAN_XUAT_LINK + " TEXT, " +

                    DANG_KY + " TEXT, " +
                    DANG_KY_LINK + " TEXT, " +
                    PHAN_PHOI + " TEXT, " +
                    PHAN_PHOI_LINK + " TEXT, " +
                    SDK + " TEXT, " +

                    SDK_LINK + " TEXT, " +
                    CAC_THUOC + " TEXT, " +
                    CAC_THUOC_LINK + " TEXT, " +
                    //GHI_CHU + " TEXT, " +   // Đưa Vào Table Cha
                    //INDEX_COLOR + " INTEGER DEFAULT 0, " +  // Đưa Vào Table Cha

                    CREATED_AT + " INTEGER, " +    // java hỗ trợ CURRENT_TIMESTAMP để tự động gán thời gian hiện tại khi một bản ghi được tạo.
                    LAST_UPDATED + " INTEGER, " +
                    //-- khai báo FOREIGN KEY phải đặt cuối cùng (ngoài định nghĩa cột)
                    //"FOREIGN KEY(" + URL +") REFERENCES %s(" + URL + ") ON DELETE CASCADE, " +

                    "FOREIGN KEY("+ URL_ID + ") REFERENCES %s(" + ID + ") ON DELETE CASCADE, " +
                    //"UNIQUE(" + URL_ID + ", " + MA_THUOC + ")" +   // 1 URL_ID chỉ chứa 1 mã thuốc duy nhất
                    "UNIQUE(" + MA_THUOC + ")" +  // Toàn cầu (Toàn bảng): HỢP LÝ HƠN
                    ");";




    //2. BẢNG CHA (PARENT): String tạo bản CHA chứa URL: : MỚI THÊM
    public static final String
            CREATE_PARENT_URLS =
            "CREATE TABLE %s (" +      // %s SẼ ĐƯỢCTHAY = BIẾN CHUỖI
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    //ID + " INTEGER, " + // KHÔNG CẦN ID, VÌ ĐÃ CÓ URL LÀ KHÓA CHÍNH
                    PARENT_ID + " LONG, " +
                    LEVEL + " INTEGER NOT NULL, " +          //Hoặc: PARENT_ID+LEVEL LÀM KHÓA CHÍNH

                    //KY_TU_SEARCH + " TEXT, " +              //KY_TU_SEARCH + " TEXT, " +: KHÔNG CẦN, LÂY TỪ URL
                    URL + " TEXT NOT NULL UNIQUE, " +        //UNIQUE: CŨNG LÀ LINK, tuowng đương tên: MA_THUOC_LINK
                    //URL + " TEXT NOT NULL, " +                     // <-- URL làm khóa ngoại: sẽ chậm, tốn ram, do chuỗi dài truy xuất sẽ cậm
                    MA_THUOC + " TEXT UNIQUE NOT NULL, " +          // Mã thuốc giả, có dạng: https://w.w.w

                    STATUS + " INTEGER DEFAULT 0, " +        //-- 0 pending, 1 success, -1 failed, -2 canceled, -3 timeout

                    GHI_CHU + " TEXT, " +   //GHI_CHU = ERR_MESSAGE + " TEXT, " +
                    INDEX_COLOR + " INTEGER DEFAULT 0, " +
                    // Lấy giây (milis: Ý có y nghĩa)
                    CREATED_AT + " INTEGER, " +
                    LAST_UPDATED + " INTEGER " +
                    ");";
    //

    //3. BẢNG QUEUE(Chứa các url ban đầu+các url phân trang được thêm vào sau khi phát hiện có): String tạo
    public static final String CREATE_URL_QUEUE_COMMON =
            "CREATE TABLE %s ( " +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    URL + " TEXT UNIQUE NOT NULL, " +
                    PARENT_ID + " LONG NOT NULL, " +
                    LEVEL + " INTEGER NOT NULL DEFAULT 1, " +   //đặt định ban đầu tùy chọn: 1 hoặc 0
                    STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                    KY_TU_SEARCH + " TEXT, " +
                    ERR_MESSAGE + " TEXT, " +   //GHI_CHU
                    LAST_RECORD_INDEX + " INTEGER DEFAULT -1, " +
                    LAST_ACCESSED + " INTEGER DEFAULT " + System.currentTimeMillis()/1000 +       //LAST_ACCESSED hay CREATED_AT
                    ");";

    private DBHelperThuoc(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }

        this.context = context.getApplicationContext();
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.enableWriteAheadLogging();

            // Sau đây là các lệnh: Kiểm tra trạng thái WAL, kiểm tra: CÓ THỂ BỎ
            // PRAGMA journal_mode trả về Cursor => dùng rawQuery thay vì execSQL
            // Chỉ giữ PRAGMA cần thiết
            Cursor c = db.rawQuery("PRAGMA journal_mode=WAL", null);    // Cài đặt chế độ WAL
            if (c.moveToFirst()) {
                Log.d(TAG, "journal_mode set to: " + c.getString(0));
            }
            c.close();

            // Các PRAGMA không trả về Cursor thì dùng execSQL bình thường
            // PRAGMA synchronous -> không trả về kết quả => execSQL ok
            db.execSQL("PRAGMA synchronous=NORMAL");   // tốc độ tốt, an toàn đủ
            // PRAGMA wal_autocheckpoint trả về kết quả => dùng rawQuery
            Cursor c2 = db.rawQuery("PRAGMA wal_autocheckpoint=0", null);
            if (c2.moveToFirst()) {
                Log.d(TAG, "wal_autocheckpoint set to: " + c2.getInt(0));
            }
            c2.close();
            Log.d(TAG, "Database initialized with WAL mode + custom checkpoint");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing database with PRAGMA settings", e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close(); // Chỉ đóng nếu db đang mở
            }
        }
    }

    public static DBHelperThuoc getInstance(Context ctx) {  // KHÔNG synchronized TÒAN HÀM SẼ NHANH HƠN
        if (instance == null) {
            synchronized (DBHelperThuoc.class) {
                if (instance == null) {
                    instance = new DBHelperThuoc(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** Lấy 1 kết nối ghi dùng chung (singleton connection) */
    public synchronized SQLiteDatabase getDb() {
        try {
            if (db == null || !db.isOpen()) {
                db = getWritableDatabase();
                Log.d("DB_LIFECYCLE", "DB OPEN (writable) by thread: " + Thread.currentThread().getName(), new Throwable("DB Open Trace"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening writable database, attempting to recreate", e);

            // Nếu DB file hỏng thì xóa file và tạo lại
            try {
                context.deleteDatabase(DATABASE_NAME);
                db = getWritableDatabase();
                Log.w(TAG, "Database file was recreated due to error");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to recreate database", ex);
                throw ex; // ném lại nếu vẫn lỗi
            }
        }
        return db;
    }

    @Override
    public synchronized void close() {
        Log.d("DB_LIFECYCLE", "DB CLOSE by thread: " + Thread.currentThread().getName(), new Throwable("DB Close Trace"));
        super.close();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        //1. PP DÙNG CHA-CON Tạo Bản CON Thuoc
        //1.1 Tạo Bản Cha Parent
        String create_parent_urls = String.format(CREATE_PARENT_URLS, TABLE_PARENT_URLS_2KT);
        db.execSQL(create_parent_urls);
        db.execSQL(create_parent_urls.replace(TABLE_PARENT_URLS_2KT, TABLE_PARENT_URLS_3KT));
        db.execSQL(create_parent_urls.replace(TABLE_PARENT_URLS_2KT, TABLE_PARENT_URLS_CUSTOM));
        // INDEX CHO STATUS, LEVEL: nếu sau này có query lọc nhiều theo status/level.
        //db.execSQL("CREATE INDEX idx_parent_status ON " + TABLE_PARENT_URLS_2KT + "(" + STATUS + ");");
        //db.execSQL("CREATE INDEX idx_parent_level ON " + TABLE_PARENT_URLS_2KT + "(" + LEVEL + ");");

        //1.2 Tạo bảng con
        db.execSQL(createThuoc2kt_child);   // Bảng 2kt
        //db.execSQL(String.format(CREATE_TABLE_THUOC_CHILD, TABLE_THUOC_2KT, TABLE_PARENT_URLS_2KT));
        //-- Index để tối ưu truy vấn:
        // Index để join nhanh
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_urlId ON " +
                TABLE_THUOC_2KT +"(" + DBHelperThuoc.URL_ID +");"   //để tăng tốc join.
        );
        // Thêm index riêng cho maThuoc
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_maThuoc ON " +
                TABLE_THUOC_2KT +"(" + DBHelperThuoc.MA_THUOC +");"
                );

//        CREATE INDEX IF NOT EXISTS idx_thuoc_maThuoc ON Thuoc(maThuoc);
//        CREATE INDEX IF NOT EXISTS idx_thuoc_url ON Thuoc(url);

        db.execSQL(createThuoc2kt_child.replace(TABLE_THUOC_2KT, TABLE_THUOC_3KT)
                .replace(TABLE_PARENT_URLS_2KT, TABLE_PARENT_URLS_3KT));   // Bảng 3kt
        //db.execSQL(String.format(CREATE_TABLE_THUOC_CHILD, TABLE_THUOC_3KT, TABLE_PARENT_URLS_3KT));
        // Index để join nhanh
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_urlId ON " +
                TABLE_THUOC_3KT +"(" + DBHelperThuoc.URL_ID +");"
        );
        // Thêm index riêng cho maThuoc
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_maThuoc ON " +
                TABLE_THUOC_3KT +"(" + DBHelperThuoc.MA_THUOC +");"
        );

        db.execSQL(createThuoc2kt_child.replace(TABLE_THUOC_2KT, TABLE_THUOC_CUSTOM)
                .replace(TABLE_PARENT_URLS_2KT, TABLE_PARENT_URLS_CUSTOM));   // Bảng custom

        //db.execSQL(String.format(CREATE_TABLE_THUOC_CHILD, TABLE_THUOC_CUSTOM, TABLE_PARENT_URLS_CUSTOM));
        // Index để join nhanh
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_urlId ON " +
                TABLE_THUOC_CUSTOM +"(" + DBHelperThuoc.URL_ID +");"
        );
        // Thêm index riêng cho maThuoc
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_thuoc_maThuoc ON " +
                TABLE_THUOC_CUSTOM +"(" + DBHelperThuoc.MA_THUOC +");"
        );

        //3. Checkpoint bền vững để phục hồi
        //Thêm bảng checkpoint (tuỳ chọn nhưng rất hữu ích):
        db.execSQL("CREATE TABLE IF NOT EXISTS CrawlCheckpoint (" +
                "  id INTEGER PRIMARY KEY CHECK (id=1), " +
                "  last_flush_seq INTEGER DEFAULT 0, " +      //-- số gói đã flush gần nhất
                "  last_flush_time INTEGER, " +               //-- epoch millis
                "  pending_in_queue INTEGER DEFAULT 0, " +    //-- ước lượng hàng đợi
                "  version INTEGER DEFAULT 1);"
        );

        //4. Tạo Bản URL QUEUE để cào: BẢNG QUEUE GIỐNG BẢNG CHA(PARENT): XEM LẠI ĐỂ TINH GỌN
        String createQueue2KT = String.format(CREATE_URL_QUEUE_COMMON, TABLE_URLS_QUEUE_2KT);
        Log.d(TAG, "Creating queue table: " + createQueue2KT);  // Log để tìm lỗi
        db.execSQL(createQueue2KT);

        db.execSQL(createQueue2KT.replace(TABLE_URLS_QUEUE_2KT, TABLE_URLS_QUEUE_3KT));

        db.execSQL(createQueue2KT.replace(TABLE_URLS_QUEUE_2KT, TABLE_URLS_QUEUE_CUSTOM));

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // onUpgrade Bản Cha Parent
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARENT_URLS_2KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARENT_URLS_3KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARENT_URLS_CUSTOM);
        // onUpgrade Bản Con Thuoc
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_THUOC_2KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_THUOC_3KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_THUOC_CUSTOM);
        // onUpgrade Bản URL QUEUE để cào
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URLS_QUEUE_2KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URLS_QUEUE_3KT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URLS_QUEUE_CUSTOM);

        onCreate(db);
    }

    public long addUrlToQueue(String queueTableName, String url, long parentId, int level, String kyTuSearch) {

        ContentValues values = new ContentValues();
        values.put(URL, url);
        values.put(PARENT_ID, parentId);
        values.put(LEVEL, level);
        values.put(STATUS, 0);
        values.put(KY_TU_SEARCH, kyTuSearch);

        long newRowId = -1;
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            newRowId = db.insertWithOnConflict(queueTableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (newRowId == -1) {
                // URL already exists in queue: thì update lại
                int rowsAffected = db.update(queueTableName, values, URL + " = ?", new String[]{url});
                Log.w(TAG, "URL already exists in queue, skipping insert: " + url + " in " + queueTableName);
            } else {
                Log.d(TAG, "Added URL to queue: " + url + " with ID: " + newRowId + " in " + queueTableName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding URL to queue: " + url + " in " + queueTableName, e);
        } finally {
            //dbWriteLock.unlock();
            // Đảm bảo khóa được giải phóng trong mọi trường hợp
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return newRowId;
    }

    public void updateStatusUrlQueue(String queueTableName, String url, String errorMessage) {

        ContentValues values = new ContentValues();
        //values.put(URL, url);
        values.put(ERR_MESSAGE, errorMessage);

        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
                // URL already exists in queue: thì update lại
            int rowsAffected = db.update(queueTableName, values, URL + " = ?", new String[]{url});
                Log.w(TAG, "URL already exists in queue, skipping insert: " + url + " in " + queueTableName);
        } catch (Exception e) {
            Log.e(TAG, "Error adding URL to queue: " + url + " in " + queueTableName, e);
        } finally {
            //dbWriteLock.unlock();
            // Đảm bảo khóa được giải phóng trong mọi trường hợp
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        //return newRowId;
    }

    public void initializeUrlQueue(String queueTableName, List<String> initialUrls, String[] kyTuArray) {

        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.beginTransaction();
            db.delete(queueTableName, LEVEL + " > 0", null);

            for (int i = 0; i < initialUrls.size(); i++) {
                String url = initialUrls.get(i);
                String kyTuSearch = (i < kyTuArray.length) ? kyTuArray[i] : "";

                ContentValues values = new ContentValues();
                values.put(URL, url);
                values.put(LEVEL, 0);
                values.put(STATUS, 0);
                values.put(LAST_ACCESSED, System.currentTimeMillis()/1000);    //(String) null

                Cursor cursor = null;
                try {
                    cursor = db.query(queueTableName, new String[]{ID}, URL + " = ?", new String[]{url}, null, null, null);
                    if (cursor.moveToFirst()) {
                        ContentValues updateValues = new ContentValues();
                        updateValues.put(STATUS, 0);
                        updateValues.put(LAST_ACCESSED, System.currentTimeMillis()/1000);  //(String) null
                        db.update(queueTableName, updateValues, URL + " = ?", new String[]{url});
                        Log.d(TAG, "Updated existing root URL in queue: " + url + " in " + queueTableName);
                    } else {
                        values.put(PARENT_ID, i + 1);
                        db.insertOrThrow(queueTableName, null, values);
                        Log.d(TAG, "Inserted new root URL to queue: " + url + " in " + queueTableName);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            db.setTransactionSuccessful();  // Đánh dấu giao dịch thành công nếu mọi thứ ổn
        } catch (Exception e) {
            Log.e(TAG, "Error initializing URL queue: " + queueTableName, e);
        } finally {
            // Luôn đảm bảo endTransaction() được gọi, bất kể thành công hay thất bại
            // KHÔNG GỌI db.close() Ở ĐÂY!
            //Không đóng database ở đây vì nó được quản lý bởi SQLiteOpenHelper
            // và có thể được sử dụng bởi các phương thức khác.
            db.endTransaction();
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public Cursor getUnprocessedUrlsCursorFromQueue(String queueTableName, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                queueTableName,
                new String[]{ID, URL, PARENT_ID, LEVEL, KY_TU_SEARCH},
                STATUS + " = 0",
                null,
                null,
                null,
                LEVEL + " ASC, " + ID + " ASC",
                String.valueOf(limit)
        );
    }

    public void markUrlAsProcessing(String initUrlsTable, String url, int level) {   //Đang xử lý status = 1. Thêm tham số level
        // CŨ: KHÔNG DÙNG
        ContentValues values = new ContentValues();
        values.put(DBHelperThuoc.LEVEL, level); // Của url cha thì level = 0 (Mặc định)
        values.put(DBHelperThuoc.URL, url);
        values.put(DBHelperThuoc.STATUS, 1); // Đang xử lý status = 1
        //Xem
        //int k = dbThuoc.rawQuery("SELECT * FROM " + initUrlsTable, null).getInt(0);
//        if (dbThuoc.rawQuery("SELECT * FROM " + initUrlsTable, null).moveToFirst()) {
//            int d = dbThuoc.rawQuery("SELECT * FROM " + initUrlsTable, null).getInt(0);
//        }
//        Cursor cursor = dbThuoc.rawQuery("SELECT * FROM " + initUrlsTable, null);
//        //int t = cursor.getInt(0);
//        while (cursor.moveToNext()) {
//            for (int n = 0; n < cursor.getColumnCount(); n++)
//                Log.d(TAG, "markUrlAsProcessing: " + cursor.getString(n));
//        }
//
//        cursor.close();
        // end xem

        //dbWriteLock.lock();
        //lock3InitUrlsTable.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock

        try {
            SQLiteDatabase dbThuoc = this.getWritableDatabase();
            int rowsAffected = dbThuoc.update(initUrlsTable, values, "url = ?", new String[]{url});
            // Dùng rowsAffected dể XEM có cập nhật thành công hay không
            if (rowsAffected == 0) {
                Log.d("DBHelperThuoc", "markUrlAsProcessing: Không thể cập nhật trạng thái của URL: " + url);
            } else {
                Log.d("DBHelperThuoc", "markUrlAsProcessing: Đã cập nhật trạng thái của URL: " + url + " thành Đang xử lý (status = 1)");
            }
        } finally {
            //dbWriteLock.unlock();
            //lock3InitUrlsTable.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public void markUrlAsProcessedInQueue(String queueTableName, String url) {
            // Đã Cào dữ liệu thuốc HOÀN THÀNH THÌ: status = 1

        ContentValues values = new ContentValues();
        values.put(STATUS, 1);
        values.put(LAST_ACCESSED, System.currentTimeMillis()/1000);    //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;  // Khai báo null

        try {
            db = this.getWritableDatabase();
            //int rowsAffected = dbThuoc.update(queueTableName, values, ID + " = ?", new String[]{urlId});
            int rowsAffected = db.update(queueTableName, values, URL + " = ?", new String[]{url});
            if (rowsAffected > 0) {
                Log.d(TAG, "Marked URL ID " + url + " as processed in queue " + queueTableName + ".");

                //Log.d(TAG, "Marked URL ID " + urlId + " as processed in queue " + queueTableName + ".");
            } else {
                Log.w(TAG, "Failed to mark URL ID " + url + " as processed (not found or already processed) in " + queueTableName + ".");

                //Log.w(TAG, "Failed to mark URL ID " + urlId + " as processed (not found or already processed) in " + queueTableName + ".");
            }
        } finally {
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public void resetUrlQueueTable(String queueTableName) {

        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().unlock(); // Dùng readLock
        SQLiteDatabase db = null;  // Khai báo null
        try {
            db = this.getWritableDatabase();
            db.beginTransaction();
            db.delete(queueTableName, LEVEL + " > 0", null);

            ContentValues values = new ContentValues();
            values.put(STATUS, 0);
            values.put(LAST_ACCESSED, System.currentTimeMillis()/1000);
            db.update(queueTableName, values, LEVEL + " = 0", null);

            db.setTransactionSuccessful();
            Log.d(TAG, "URL queue table " + queueTableName + " reset.");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting URL queue table " + queueTableName, e);
        } finally {
            if (db != null) {
                db.endTransaction();
            }
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public long insertOrUpdateThuoc(String tableName, ThuocSQLite thuoc) {

        ContentValues values = new ContentValues();
        //values.put(URL, thuoc.url);
        values.put(URL_ID, thuoc.url_id);
        //values.put(PARENT_ID, thuoc.parent_id);
        //values.put(LEVEL, thuoc.level);
        //values.put(KY_TU_SEARCH, thuoc.ky_tu_search);
        values.put(MA_THUOC, thuoc.ma_thuoc);
        values.put(TEN_THUOC, thuoc.ten_thuoc);
        values.put(THANH_PHAN, thuoc.thanh_phan);
        values.put(NHOM_THUOC, thuoc.nhom_thuoc);
        values.put(DANG_THUOC, thuoc.dang_thuoc);
        values.put(SAN_XUAT, thuoc.san_xuat);
        values.put(DANG_KY, thuoc.dang_ky);
        values.put(PHAN_PHOI, thuoc.phan_phoi);
        values.put(SDK, thuoc.sdk);
        values.put(CAC_THUOC, thuoc.cac_thuoc);
        values.put(MA_THUOC_LINK, thuoc.ma_thuoc_link);
        values.put(THANH_PHAN_LINK, thuoc.thanh_phan_link);
        values.put(NHOM_THUOC_LINK, thuoc.nhom_thuoc_link);
        values.put(DANG_THUOC_LINK, thuoc.dang_thuoc_link);
        values.put(SAN_XUAT_LINK, thuoc.san_xuat_link);
        values.put(DANG_KY_LINK, thuoc.dang_ky_link);
        values.put(PHAN_PHOI_LINK, thuoc.phan_phoi_link);
        values.put(SDK_LINK, thuoc.sdk_link);
        values.put(CAC_THUOC_LINK, thuoc.cac_thuoc_link);
        //values.put(GHI_CHU, thuoc.ghi_chu);
        //values.put(INDEX_COLOR, thuoc.index_color);
        values.put(CREATED_AT, System.currentTimeMillis()/1000);

        long resultId = -1;
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rowsAffected = db.update(tableName, values, MA_THUOC + " = ?", new String[]{thuoc.ma_thuoc});
            if (rowsAffected == 0) {
                resultId = db.insertOrThrow(tableName, null, values);
                Log.d(TAG, "Inserted Thuoc: " + thuoc.ma_thuoc + " into " + tableName + " from url_id: " + thuoc.url_id);
            } else {
                Log.d(TAG, "Updated Thuoc: " + thuoc.ma_thuoc + " in " + tableName + " from url_id: " + thuoc.url_id);
                Cursor cursor = db.query(tableName, new String[]{ID}, MA_THUOC + " = ?", new String[]{thuoc.ma_thuoc}, null, null, null);
                if (cursor.moveToFirst()) {
                    resultId = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
                }
                cursor.close();
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "SQLiteConstraintException when inserting/updating Thuoc: " + thuoc.ma_thuoc + " in " + tableName, e);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting/updating Thuoc: " + thuoc.ma_thuoc + " in " + tableName, e);
        } finally {
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return resultId;
    }

    /**
     * @param initUrlsTable
     * @param url
     * @return
     */
    private boolean isUrlExists(String initUrlsTable, String url) { // Cần không vì initUrlsTable có url là khóa chính
        //boolean exists;
        Cursor cursor = null;
        //dbWriteLock.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock
        try {
            SQLiteDatabase db = this.getReadableDatabase(); // Lấy thể hiện DB trong khối khóa
            cursor = db.query(initUrlsTable, new String[]{DBHelperThuoc.URL}, DBHelperThuoc.URL + " = ?", new String[]{url}, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null){
                cursor.close();
            }
            //dbWriteLock.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }
        //return exists;
    }

    /**
     * Chèn url con (phân trang) tìm được vào Bảng Queue dùng để Crawl (initUrlsTable)
     * @param initUrlsTable
     * @param url
     * @param parentId
     * @param level
     */
    public void saveChildUrlToUrlQueueTable(String initUrlsTable,
                                            String url, long parentId, int level) {

        // level đã + 1 khi gọi hàm này. PP mới level là số trang page của trang con
        if (!isUrlExists(initUrlsTable, url)) { // Cần không vì initUrlsTable có url là khóa chính
            //Lưu url con vào bảng initUrlsTable
            ContentValues values = new ContentValues();
            values.put(DBHelperThuoc.PARENT_ID, parentId);
            values.put(DBHelperThuoc.LEVEL, level); //leve > 0 là url con
            values.put(DBHelperThuoc.URL, url); // là khóa chính
            values.put(DBHelperThuoc.STATUS, 0);    //Mặc định là 0 (chưa xử lý)
            //values.put(DBHelperThuoc.IS_CHILD, level > 1?1:0); // Đánh dấu là url con (ban đầu là 1 (true))
            //Không ném lỗi nếu trùng url
            //dbThuoc.insertWithOnConflict( initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrls

            // Hoặc: bắt lỗi nếu trùng url
            //dbWriteLock.lock();
            dbReadWriteLock.writeLock().lock(); // Dùng readLock
            SQLiteDatabase db = null    ;
            try {
                db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
                db.beginTransaction();
                db.insertOrThrow(initUrlsTable, null, values);

                db.setTransactionSuccessful();
            } catch (SQLiteConstraintException e) {
                Log.w("CrawlWorker", "URL already exists: " + url);
            } finally {
                if (db != null) { // Chỉ kiểm tra db có null không. KHÔNG KIỂM TRA db.inTransaction(), NẾU KHÔNG SẼ GÂY LỖI
                    // Gọi endTransaction() mà không cần kiểm tra inTransaction()
                    // Vì thiếu lệnh này mà insertMultipleThuoc bị lỗi đã setTransactionSuccessful mà không endTransaction
                    db.endTransaction();
                }
//                if (db != null && db.inTransaction()) { // Kiểm tra xem giao dịch có đang hoạt động không trước khi kết thúc
//                    db.endTransaction();    // Vì thiếu lệnh này mà insertMultipleThuoc bị lỗi đã setTransactionSuccessful mà không endTransaction
//                }
                //dbWriteLock.unlock();
                dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
            }

        }

    }

    public void insertParent(SQLiteDatabase db, String tableParent, String parent_id,String level,
                             String url, String createdAt, String ghiChu, int indexColor, String errorMessage) {
        ContentValues values = new ContentValues();
        values.put(PARENT_ID, parent_id);
        values.put(LEVEL, level);
        values.put(URL, url);
        values.put(CREATED_AT, createdAt);
        values.put(GHI_CHU, ghiChu);
        values.put(INDEX_COLOR, indexColor);
        //INSERT INTO " + tableThuoc2KThay3KT
        values.put(ERR_MESSAGE, ghiChu);  //Tạm thời ERR_MESSAGE nằm trong ghi_chu
        db.insertWithOnConflict(tableParent, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Dẹp transaction trong từng thread ForkJoin (an toàn, đơn giản nhất)
     *
     * Chèn nhiều bản ghi ThuocPrefs vào bảng, xử lý xung đột (ON CONFLICT DO UPDATE).
     * @param tableThuoc2KThay3KT Tên bảng đích (ví dụ: TABLE_THUOC_TBDT_SEARCH)
     * @param listThuoc           Danh sách các đối tượng ThuocPrefs cần chèn/cập nhật.
     *
     * Ưu: Dễ áp dụng, an toàn thread, không conflict transaction
     * Nhược: Không atomic nếu cần rollback toàn bộ 1 batch nếu lỗi.
     */
    public void insertMultipleThuocNotransaction (String tableThuoc2KThay3KT, List<ThuocSQLite> listThuoc) {
        /**
         * Ưu: Dễ áp dụng, an toàn thread, không conflict transaction
         * Nhược: Không atomic nếu cần rollback toàn bộ 1 batch nếu lỗi.
         */
        dbReadWriteLock.writeLock().lock();
        SQLiteDatabase db = null;
        SQLiteStatement stmt = null;
        try {
            // Insert vào Bảng con
            db = this.getWritableDatabase();
            String query = "INSERT INTO " + tableThuoc2KThay3KT + " (" +
                    URL_ID + ", " +
                    PARENT_ID + ", " +
                    LEVEL + ", " +
                    KY_TU_SEARCH + ", " +
                    MA_THUOC + ", " +

                    MA_THUOC_LINK + ", " +
                    TEN_THUOC + ", " +
                    THANH_PHAN + ", " +
                    THANH_PHAN_LINK + ", " +
                    NHOM_THUOC + ", " +

                    NHOM_THUOC_LINK + ", " +
                    DANG_THUOC + ", " +
                    DANG_THUOC_LINK + ", " +
                    SAN_XUAT + ", " +
                    SAN_XUAT_LINK + ", " +

                    DANG_KY + ", " +
                    DANG_KY_LINK + ", " +
                    PHAN_PHOI + ", " +
                    PHAN_PHOI_LINK + ", " +
                    SDK + ", "+

                    SDK_LINK + ", " +
                    CAC_THUOC + ", " +
                    CAC_THUOC_LINK + ", " +     //URL + ", " +: bảng con bỏ
                    GHI_CHU + ", " +
                    INDEX_COLOR + ", " +

                    CREATED_AT + ", " +     // DÀNH CHO INSERT
                    LAST_UPDATED + ") " +

                    "VALUES (" +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?) " +
                    "ON CONFLICT(" + MA_THUOC + ") " +
                    "DO UPDATE SET " +
                    URL_ID + " = excluded." + URL_ID + ", " +
                    PARENT_ID + " = excluded." + PARENT_ID + ", " +
                    LEVEL + " = excluded." + LEVEL + ", " +
                    KY_TU_SEARCH + " = excluded." + KY_TU_SEARCH + ", " +
                    MA_THUOC + " = excluded." + MA_THUOC + ", " +

                    MA_THUOC_LINK + " = excluded." + MA_THUOC_LINK + ", " +
                    TEN_THUOC + " = excluded." + TEN_THUOC + ", " +
                    THANH_PHAN + " = excluded." + THANH_PHAN + ", " +
                    THANH_PHAN_LINK + " = excluded." + THANH_PHAN_LINK + ", " + // Đã sửa tên cột ở đây
                    NHOM_THUOC + " = excluded." + NHOM_THUOC + ", " +

                    NHOM_THUOC_LINK + " = excluded." + NHOM_THUOC_LINK + ", " +
                    DANG_THUOC + " = excluded." + DANG_THUOC + ", " +
                    DANG_THUOC_LINK + " = excluded." + DANG_THUOC_LINK + ", " +
                    SAN_XUAT + " = excluded." + SAN_XUAT + ", " +
                    SAN_XUAT_LINK + " = excluded." + SAN_XUAT_LINK + ", " +

                    DANG_KY + " = excluded." + DANG_KY + ", " +
                    DANG_KY_LINK + " = excluded." + DANG_KY_LINK + ", " +
                    PHAN_PHOI + " = excluded." + PHAN_PHOI + ", " +
                    PHAN_PHOI_LINK + " = excluded." + PHAN_PHOI_LINK + ", " +
                    SDK + " = excluded." + SDK + ", " +

                    SDK_LINK + " = excluded." + SDK_LINK + ", " +
                    CAC_THUOC + " = excluded." + CAC_THUOC + ", " +
                    CAC_THUOC_LINK + " = excluded." + CAC_THUOC_LINK + ", " +
                    GHI_CHU + " = excluded." + GHI_CHU + ", " +
                    INDEX_COLOR + " = excluded." + INDEX_COLOR + ", " +

                    LAST_UPDATED + " = excluded." + LAST_UPDATED;   // DÀNH CHO UPDATE

            stmt = db.compileStatement(query); // Gán giá trị cho stmt ở đây
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTimestamp = sdf.format(new Date());

            for (ThuocSQLite t : listThuoc) {
                stmt.clearBindings();
                // binding giá trị như cũ
                int colIndex = 1;
                stmt.bindLong(colIndex++, t.url_id);      //parent=parent_id_of_crawled_url
                //stmt.bindLong(colIndex++, t.parent_id);      //parent=parent_id_of_crawled_url
                //stmt.bindLong(colIndex++, t.level);             //LEVEL=level_of_crawled_url
                //stmt.bindString(colIndex++, t.ky_tu_search != null ? t.ky_tu_search : "");
                stmt.bindString(colIndex++, t.ma_thuoc != null ? t.ma_thuoc : "");

                stmt.bindString(colIndex++, t.ma_thuoc_link != null ? t.ma_thuoc_link : "");
                stmt.bindString(colIndex++, t.ten_thuoc != null ? t.ten_thuoc : "");
                stmt.bindString(colIndex++, t.thanh_phan != null ? t.thanh_phan : "");
                stmt.bindString(colIndex++, t.thanh_phan_link != null ? t.thanh_phan_link : "");
                stmt.bindString(colIndex++, t.nhom_thuoc != null ? t.nhom_thuoc : "");

                stmt.bindString(colIndex++, t.nhom_thuoc_link != null ? t.nhom_thuoc_link : "");
                stmt.bindString(colIndex++, t.dang_thuoc != null ? t.dang_thuoc : "");
                stmt.bindString(colIndex++, t.dang_thuoc_link != null ? t.dang_thuoc_link : "");
                stmt.bindString(colIndex++, t.san_xuat != null ? t.san_xuat : "");
                stmt.bindString(colIndex++, t.san_xuat_link != null ? t.san_xuat_link : "");

                stmt.bindString(colIndex++, t.dang_ky != null ? t.dang_ky : "");
                stmt.bindString(colIndex++, t.dang_ky_link != null ? t.dang_ky_link : "");
                stmt.bindString(colIndex++, t.phan_phoi != null ? t.phan_phoi : "");
                stmt.bindString(colIndex++, t.phan_phoi_link != null ? t.phan_phoi_link : "");
                stmt.bindString(colIndex++, t.sdk != null ? t.sdk : "");

                stmt.bindString(colIndex++, t.sdk_link != null ? t.sdk : "");
                stmt.bindString(colIndex++, t.cac_thuoc != null ? t.cac_thuoc : "");
                stmt.bindString(colIndex++, t.cac_thuoc_link != null ? t.cac_thuoc_link : "");
                //stmt.bindString(colIndex++, t.ghi_chu != null ? t.ghi_chu : "");
                //stmt.bindLong(colIndex++, t.index_color);

                stmt.bindString(colIndex++, String.valueOf(System.currentTimeMillis()/1000));      //currentTimestamp

                stmt.executeInsert();

            }
            Log.d(TAG, "insertMultipleThuoc: Hoàn tất insert/update " + listThuoc.size());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi insertMultipleThuoc: " + e.getMessage(), e);
        } finally {
            if (stmt != null) stmt.close();
            dbReadWriteLock.writeLock().unlock();
        }

    }

    /**
     * Chèn nhiều bản ghi ThuocPrefs vào bảng, xử lý xung đột (ON CONFLICT DO UPDATE).
     *
     * @param tableThuoc2KThay3KT Tên bảng đích (ví dụ: TABLE_THUOC_TBDT_SEARCH)
     * @param listThuoc           Danh sách các đối tượng ThuocPrefs cần chèn/cập nhật.
     */
    public void insertMultipleThuoc(String tableThuoc2KThay3KT, List<ThuocSQLite> listThuoc) {
        //BỊ LỖI: Lỗi khi insertMultipleThuoc vào TABLE_THUOC_2KT: Cannot perform this operation because the transaction has already been marked successful.  The only thing you can do now is call endTransaction().
        Log.d(TAG, "insertMultipleThuoc: BẮT ĐẦU insertMultipleThuoc cho " + tableThuoc2KThay3KT);
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng writeLock
        SQLiteDatabase db = null; // Khai báo db ở đây
        SQLiteStatement stmt = null; // Khai báo stmt ở đây
        try {
            db = this.getWritableDatabase(); // Lấy thể hiện SQLiteDatabase trong khối khóa tại đây
            db.beginTransaction(); // Bắt đầu giao dịch

            String query = "INSERT INTO " + tableThuoc2KThay3KT + " (" +
                    URL_ID + ", " +
                    PARENT_ID + ", " +
                    LEVEL + ", " +
                    KY_TU_SEARCH + ", " +
                    MA_THUOC + ", " +

                    MA_THUOC_LINK + ", " +
                    TEN_THUOC + ", " +
                    THANH_PHAN + ", " +
                    THANH_PHAN_LINK + ", " +
                    NHOM_THUOC + ", " +

                    NHOM_THUOC_LINK + ", " +
                    DANG_THUOC + ", " +
                    DANG_THUOC_LINK + ", " +
                    SAN_XUAT + ", " +
                    SAN_XUAT_LINK + ", " +

                    DANG_KY + ", " +
                    DANG_KY_LINK + ", " +
                    PHAN_PHOI + ", " +
                    PHAN_PHOI_LINK + ", " +
                    SDK + ", "+

                    SDK_LINK + ", " +
                    CAC_THUOC + ", " +
                    CAC_THUOC_LINK + ", " +     //URL + ", " +: bảng con bỏ
                    GHI_CHU + ", " +
                    INDEX_COLOR + ", " +

                    CREATED_AT + ") " +

                    "VALUES (" +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, " +
                    "?) " +
                    "ON CONFLICT(" + URL_ID + ", " + MA_THUOC + ") " +
                    "DO UPDATE SET " +

                    URL_ID + " = excluded." + URL_ID + ", " +
                    PARENT_ID + " = excluded." + PARENT_ID + ", " +
                    LEVEL + " = excluded." + LEVEL + ", " +
                    KY_TU_SEARCH + " = excluded." + KY_TU_SEARCH + ", " +
                    MA_THUOC + " = excluded." + MA_THUOC + ", " +

                    MA_THUOC_LINK + " = excluded." + MA_THUOC_LINK + ", " +
                    TEN_THUOC + " = excluded." + TEN_THUOC + ", " +
                    THANH_PHAN + " = excluded." + THANH_PHAN + ", " +
                    THANH_PHAN_LINK + " = excluded." + THANH_PHAN_LINK + ", " + // Đã sửa tên cột ở đây
                    NHOM_THUOC + " = excluded." + NHOM_THUOC + ", " +

                    NHOM_THUOC_LINK + " = excluded." + NHOM_THUOC_LINK + ", " +
                    DANG_THUOC + " = excluded." + DANG_THUOC + ", " +
                    DANG_THUOC_LINK + " = excluded." + DANG_THUOC_LINK + ", " +
                    SAN_XUAT + " = excluded." + SAN_XUAT + ", " +
                    SAN_XUAT_LINK + " = excluded." + SAN_XUAT_LINK + ", " +

                    DANG_KY + " = excluded." + DANG_KY + ", " +
                    DANG_KY_LINK + " = excluded." + DANG_KY_LINK + ", " +
                    PHAN_PHOI + " = excluded." + PHAN_PHOI + ", " +
                    PHAN_PHOI_LINK + " = excluded." + PHAN_PHOI_LINK + ", " +
                    SDK + " = excluded." + SDK + ", " +

                    SDK_LINK + " = excluded." + SDK_LINK + ", " +
                    CAC_THUOC + " = excluded." + CAC_THUOC + ", " +
                    CAC_THUOC_LINK + " = excluded." + CAC_THUOC_LINK + ", " +
                    GHI_CHU + " = excluded." + GHI_CHU + ", " +
                    INDEX_COLOR + " = excluded." + INDEX_COLOR + ", " +

                    CREATED_AT + " = excluded." + CREATED_AT;

            stmt = db.compileStatement(query); // Gán giá trị cho stmt ở đây

//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//            String currentTimestamp = sdf.format(new Date());

            for (ThuocSQLite t : listThuoc) {
                stmt.clearBindings();   // Xóa bind cũ
                int colIndex = 1;
                stmt.bindLong(colIndex++, t.url_id);
                //stmt.bindLong(colIndex++, t.parent_id);      //parent=parent_id_of_crawled_url
                //stmt.bindLong(colIndex++, t.level);             //LEVEL=level_of_crawled_url
                //stmt.bindString(colIndex++, t.ky_tu_search != null ? t.ky_tu_search : "");
                stmt.bindString(colIndex++, t.ma_thuoc != null ? t.ma_thuoc : "");

                stmt.bindString(colIndex++, t.ma_thuoc_link != null ? t.ma_thuoc_link : "");
                stmt.bindString(colIndex++, t.ten_thuoc != null ? t.ten_thuoc : "");
                stmt.bindString(colIndex++, t.thanh_phan != null ? t.thanh_phan : "");
                stmt.bindString(colIndex++, t.thanh_phan_link != null ? t.thanh_phan_link : "");
                stmt.bindString(colIndex++, t.nhom_thuoc != null ? t.nhom_thuoc : "");

                stmt.bindString(colIndex++, t.nhom_thuoc_link != null ? t.nhom_thuoc_link : "");
                stmt.bindString(colIndex++, t.dang_thuoc != null ? t.dang_thuoc : "");
                stmt.bindString(colIndex++, t.dang_thuoc_link != null ? t.dang_thuoc_link : "");
                stmt.bindString(colIndex++, t.san_xuat != null ? t.san_xuat : "");
                stmt.bindString(colIndex++, t.san_xuat_link != null ? t.san_xuat_link : "");

                stmt.bindString(colIndex++, t.dang_ky != null ? t.dang_ky : "");
                stmt.bindString(colIndex++, t.dang_ky_link != null ? t.dang_ky_link : "");
                stmt.bindString(colIndex++, t.phan_phoi != null ? t.phan_phoi : "");
                stmt.bindString(colIndex++, t.phan_phoi_link != null ? t.phan_phoi_link : "");
                stmt.bindString(colIndex++, t.sdk != null ? t.sdk : "");

                stmt.bindString(colIndex++, t.sdk_link != null ? t.sdk_link : "");
                stmt.bindString(colIndex++, t.cac_thuoc != null ? t.cac_thuoc : "");
                stmt.bindString(colIndex++, t.cac_thuoc_link != null ? t.cac_thuoc_link : "");
                //stmt.bindString(colIndex++, t.ghi_chu != null ? t.ghi_chu : "");
                //stmt.bindLong(colIndex++, t.index_color);

                stmt.bindLong(colIndex++, System.currentTimeMillis()/1000 );        //currentTimestamp: là String time

                stmt.executeInsert();
            }

            db.setTransactionSuccessful(); // Đánh dấu transaction thành công
            Log.d(TAG, "insertMultipleThuoc: Hoàn thành insert/update " + listThuoc.size() + " bản ghi vào " + tableThuoc2KThay3KT);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi insertMultipleThuoc vào " + tableThuoc2KThay3KT + ": " + e.getMessage(), e);
            // Không cần gọi setTransactionSuccessful() ở đây

        } finally {
            // Đảm bảo kết thúc giao dịch
            // Luôn đảm bảo endTransaction() được gọi, bất kể thành công hay thất bại
            // KHÔNG GỌI db.close() Ở ĐÂY!

            if (db != null) { // Chỉ kiểm tra db có null không. KHÔNG KIỂM TRA db.inTransaction(), NẾU KHÔNG SẼ GÂY LỖI
                // Gọi endTransaction() mà không cần kiểm tra inTransaction()
                db.endTransaction();
            }
            if (stmt != null) {
                stmt.close(); // Đóng SQLiteStatement
            }
            //dbWriteLock.unlock();   // Giải phóng khóa ghi
            dbReadWriteLock.writeLock().unlock(); // Giải phóng writeLock
            // QUAN TRỌNG: KHÔNG ĐÓNG DB Ở ĐÂY!
            // SQLiteOpenHelper sẽ tự động quản lý việc đóng/mở và giữ kết nối hiệu quả.
        }
    }

    /**
     * Lấy số lượng bản ghi trong một bảng cụ thể.
     * @param tableName Tên của bảng cần đếm số bản ghi.
     * @return Số lượng bản ghi trong bảng, hoặc 0 nếu bảng không tồn tại hoặc có lỗi.
     */
    public long getCountRecord(String tableName) {   //getThuocCount: đăt tên chung, rồi lấy tông record của tên bảng
        Cursor cursor = null;
        long count = 0;

        // Lấy db ở đây trong mỗi phương thức
        dbReadWriteLock.readLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            // Kiểm tra xem bảng có tồn tại không
            cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getInt(0) == 0) {
                    Log.w(TAG, "Table '" + tableName + "' does not exist.");
                    return 0; // Bảng không tồn tại
                }
            }
            if (cursor != null) {
                cursor.close(); // Đóng cursor sau khi kiểm tra bảng
                cursor = null;
            }

            // Đếm số lượng bản ghi trong bảng
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting count from table " + tableName + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
            // Không đóng database ở đây vì nó được quản lý bởi SQLiteOpenHelper
            // và có thể được sử dụng bởi các phương thức khác.
            // db.close(); // KHÔNG ĐÓNG DB TẠI ĐÂY
        }
        return count;
    }

    /**
     * Phương thức để xóa tất cả bản ghi trong một bảng cụ thể.
     * @param tableName Tên bảng cần xóa dữ liệu.
     * @return Số hàng đã xóa.
     */
    public int deleteAllRecords(String tableName) {
        //dbWriteLock.lock(); // Sử dụng lock nếu đây là thao tác ghi
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        int rowsAffected = 0;
        SQLiteDatabase db = null; // Lấy db tại đây
        try {
            db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            rowsAffected = db.delete(tableName, null, null);
            Log.d(TAG, "Deleted " + rowsAffected + " records from table " + tableName);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting records from table " + tableName + ": " + e.getMessage());
        } finally {
            //dbWriteLock.unlock(); // Giải phóng lock
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
            // Không đóng database ở đây
        }
        return rowsAffected;
    }


    public String getDateFromTable(String tableName, String type) {

        String date = "Không có dữ liệu";
        Cursor cursor = null;
        //dbWriteLock.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock
        //SQLiteDatabase db = null; // Khai báo null sau khóa
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            cursor = db.rawQuery("SELECT " + type + "(" + CREATED_AT + ") FROM " + tableName, null);
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);
                if (result != null) date = result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting " + type + " date from " + tableName + ": " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            //dbWriteLock.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }
        return date;
    }

    //region Các phương thức mới cho InitUrlsTable
    public boolean addInitUrl(String initUrlsTable, String url) {
        //lock3InitUrlsTable.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        long id = -1;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            ContentValues values = new ContentValues();
            values.put(URL, url);
            id = db.insertWithOnConflict(initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            // Log.d("DBHelperThuoc", "Đã thêm Init URL: " + url + " vào " + initUrlsTable);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi thêm Init URL vào " + initUrlsTable + ": " + e.getMessage());
        } finally {
            //lock3InitUrlsTable.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return id != -1;
    }

    public List<String[]> loadUrlsFromGetKyTuAZ(String initUrlsTable, int k0, int k1) { //KHÔNG gồm k1
        //tableThuoc: Phải gán giá trị trong constructor, là biến thành viên: có tác dụng toàn class
        //Tổng url sẽ là hàng số: luôn tính trên 1 lần chạy, kể cả khi thoát và app tự chạy lại
        //List<String> urls = new ArrayList<>();
        //String tableThuoc = currentCrawlType.getUrlQueueTableName();

        List<String> listKyTu = GetKyTuAZ.getList_AZ_2KThay3KT(initUrlsTable, k0, k1); //KHÔNG gồm k1
        List<String[]> listRowUrls = new ArrayList<>();

        int parentId = 0;
        // Ghi URL vào bảng initUrlsTable
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        try {

            SQLiteDatabase db = this.getWritableDatabase();
            assert listKyTu != null;
            for (String kytu : listKyTu) {
                if (kytu.trim().isEmpty())
                    continue; //Chỉ kiểm tra url rỗng (hoặc chỉ chứa các khoảng trắng)
                // Vẫn lấy kytu có khoảng trắng ở đầu và cuối
                String[] row = new String[3];
                row[0] = String.valueOf(parentId);  //parentId
                row[1] = String.valueOf(1);     //Cũ: level = 0; Mới: level (thành page) = 1
                row[2] = (AppConstants.URL0 + kytu + AppConstants.URL1.trim());   //url

                // Xem: tìm lỗi thiếu đếm processedUrlStart1Counter thiếu 1
                Log.d(TAG, "loadUrlsFromAsset: processedUrlStart1Counter, url (start=1) = " + row[2] + ";parentId=" + row[0]);
                //
                listRowUrls.add(row);   // Hoặc chỉ 1 lệnh: listRowUrls.add(new String[]{String.valueOf(parentId), String.valueOf(parentId), (url0 + kytu + url1).trim()});


//            kytu = url0 + kytu + url1;
//            String trimmedUrl = kytu.trim(); //Cắt khoảng trắng ở đầu và cuối
//            urls.add(trimmedUrl);

                // Ghi URL vào bảng initUrlsTable
                ContentValues values = new ContentValues();
                values.put(DBHelperThuoc.PARENT_ID, parentId);
                values.put(DBHelperThuoc.LEVEL, 1); //Cũ: Của url cha level = 0 (Mặc định); Mới: level (thành page) = 1 (Mặc định)
                values.put("url", (AppConstants.URL0 + kytu + AppConstants.URL1).trim());
                values.put("status", 0); // 0: chưa xử lý, có thể thêm cột 'completed' nếu cần. Mặc định completed = 0 (uncompleted)
                //values.put(DBHelperThuoc.BOOLEAN_URL_CHILD, 0); // Mặc định ban đầu là 0 (false) : không phai là url Child (con)
                //Cũ: có ném lô
                //dbThuoc.insert( initUrlsTable, null, values); //initUrlsTable. tableNameThuoc_Nao phải có
                //Mới: không ném lỗi
                db.insertWithOnConflict(initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
                parentId++;
            }
        } finally {
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        // Lưu tổng số URL vào SharedPreferences
        //saveTotalUrls2SharePrefs(tableThuoc, listRowUrls.size());     //XEM LẠI: TẠI SAO LUU VÀO PREFS
        return listRowUrls;
    }

    public List<String[]> loadUrlsFromUrlQueueTableName(String initUrlsTable) { //KHÔNG gồm k1
        //tableThuoc: Phải gán giá trị trong constructor, là biến thành viên: có tác dụng toàn class
        //Tổng url sẽ là hàng số: luôn tính trên 1 lần chạy, kể cả khi thoát và app tự chạy lại

        //String url0 = "https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=";
        //String url1 = "&opt=TT&start=1";

        List<String[]> listRowUrls = new ArrayList<>();

        // Chưa xử lý có STATUS = 0
        //dbWriteLock.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock
        //SQLiteDatabase db = null; // Khai báo null sau khóa
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            Cursor cursor = db.query(initUrlsTable, new String[]{URL, PARENT_ID, LEVEL}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String[] row = new String[3];
                    row[0] = cursor.getString(0);   //url
                    row[1] = cursor.getString(1);   // PARENT_ID
                    row[2] = cursor.getString(2);   //LEVEL: Cũ: level = 0; Mới: level (thành page) = 1
                    listRowUrls.add(row);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } finally {
            //dbWriteLock.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }

        return listRowUrls;
   }
    public List<String> getAllUrlQueue(String initUrlsTable) {  // Chỉ lấy các url chưa xử lý (STATUS = 0)
        // Chỉ lấy các url chưa xử lý (STATUS = 0)
        List<String> urls = new ArrayList<>();
        //lock3InitUrlsTable.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock

        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            cursor = db.query(initUrlsTable, new String[]{URL}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    urls.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi lấy tất cả Init URL từ " + initUrlsTable + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            //lock3InitUrlsTable.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }
        return urls;
    }

    public boolean deleteInitUrl(String initUrlsTable, String url) {
        //lock3InitUrlsTable.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = null;

        int rowsAffected = 0;
        try {
            db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            rowsAffected = db.delete(initUrlsTable, URL + " = ?", new String[]{url});
            // Log.d("DBHelperThuoc", "Đã xóa Init URL: " + url + " khỏi " + initUrlsTable + ". Số hàng ảnh hưởng: " + rowsAffected);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi xóa Init URL khỏi " + initUrlsTable + ": " + e.getMessage());
        } finally {
            //lock3InitUrlsTable.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock

        }
        return rowsAffected > 0;
    }

    public boolean isInitUrlsTableEmpty(String initUrlsTable) {
        //lock3InitUrlsTable.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock

        Cursor cursor = null;
        boolean isEmpty = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + initUrlsTable, null);
            if (cursor.moveToFirst()) {
                if (cursor.getInt(0) > 0) {
                    isEmpty = false;
                }
            }
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi kiểm tra bảng Init URL " + initUrlsTable + " trống: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            //lock3InitUrlsTable.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }
        return isEmpty;
    }

    public boolean addCompletedUrlChild(String completedUrlsChildTable, String url) {
        //dbWriteLock.lock();
        //lock2CompletedUrlsTable.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock

        long id = -1;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            ContentValues values = new ContentValues();
            values.put(URL, url);
            id = db.insertWithOnConflict(completedUrlsChildTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi thêm URL đã hoàn thành vào " + completedUrlsChildTable + ": " + e.getMessage());
        } finally {
            //dbWriteLock.unlock();
            //lock2CompletedUrlsTable.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return id != -1;
    }
    public long addThuoc(String tableName, ThuocSQLite thuoc) {
        //dbWriteLock.lock(); // Khóa ghi
        dbReadWriteLock.writeLock().lock(); // Dùng readLock
        long result = -1;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            ContentValues values = new ContentValues();
            //values.put(PARENT_ID, thuoc.parent_id);   //PARENT=PARENT_ID_OF_CRAWLED_URL
            //values.put(LEVEL, thuoc.level);
            //values.put(KY_TU_SEARCH, thuoc.ky_tu_search);
            values.put(MA_THUOC, thuoc.ma_thuoc);
            values.put(TEN_THUOC, thuoc.ten_thuoc);
            values.put(THANH_PHAN, thuoc.thanh_phan);
            values.put(NHOM_THUOC, thuoc.nhom_thuoc);
            values.put(DANG_THUOC, thuoc.dang_thuoc);
            values.put(SAN_XUAT, thuoc.san_xuat);
            values.put(DANG_KY, thuoc.dang_ky);
            values.put(PHAN_PHOI, thuoc.phan_phoi);
            values.put(SDK, thuoc.sdk);
            values.put(CAC_THUOC, thuoc.cac_thuoc);
            //values.put(LINK_KY_TU_SEARCH, thuoc.link_ky_tu_search);
            values.put(MA_THUOC_LINK, thuoc.ma_thuoc_link);
            values.put(THANH_PHAN_LINK, thuoc.thanh_phan_link);
            values.put(NHOM_THUOC_LINK, thuoc.nhom_thuoc_link);
            values.put(DANG_THUOC_LINK, thuoc.dang_thuoc_link);
            values.put(SAN_XUAT_LINK, thuoc.san_xuat_link);
            values.put(DANG_KY_LINK, thuoc.dang_ky_link);
            values.put(PHAN_PHOI_LINK, thuoc.phan_phoi_link);
            values.put(SDK_LINK, thuoc.sdk_link);
            values.put(CAC_THUOC_LINK, thuoc.cac_thuoc_link);
            //values.put(GHI_CHU, thuoc.ghi_chu);
            //values.put(INDEX_COLOR, thuoc.index_color);
            values.put(CREATED_AT, System.currentTimeMillis()/1000); // Tự động thêm timestamp


                result = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE); // SỬA: Conflict policy

        } finally {
            //dbWriteLock.unlock(); // Mở khóa ghi
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return result;
    }

    // Đặt biệt
    /**
     * Xóa các bản ghi trùng lặp trong một bảng cụ thể dựa trên cột ma_thuoc.
     * Chỉ giữ lại một bản ghi cho mỗi ma_thuoc duy nhất, giữ bản ghi có ID nhỏ nhất (thường là bản ghi được thêm vào trước).
     * @param tableThuoc Tên của bảng cần xử lý trùng lặp.
     * @return Số lượng bản ghi trùng lặp đã bị xóa.
     */
    public int deleteDuplicateRecords(String tableThuoc) {
        //dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng readLock

        int deletedRows = 0;
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            // Kiểm tra xem bảng có tồn tại không
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableThuoc});
                if (cursor != null && cursor.moveToFirst()) {
                    if (cursor.getInt(0) == 0) {
                        Log.w(TAG, "Table '" + tableThuoc + "' does not exist. Cannot delete duplicates.");
                        return 0; // Bảng không tồn tại
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Xóa các bản ghi trùng lặp dựa trên ma_thuoc, giữ lại bản ghi có ID nhỏ nhất
            // Đây là một ví dụ cơ bản. Tùy thuộc vào yêu cầu, bạn có thể muốn giữ bản ghi mới nhất (MAX(id))
            String query = "DELETE FROM " + tableThuoc +
                    " WHERE " + ID + " NOT IN (" +      // Không nằm trong danh sách sau
                    "    SELECT MIN(" + ID + ")" +  // Chỉ giữ lại bản ghi có ID nhỏ nhất
                    "    FROM " + tableThuoc +
                    "    GROUP BY " + MA_THUOC +    //"    GROUP BY " + MA_THUOC + " AND " + PHAN_PHOI  +
                    ");";
            db.execSQL(query);

            // Để lấy số lượng hàng bị xóa, bạn có thể đếm lại trước và sau
            // Hoặc sử dụng trigger nếu cần số lượng chính xác sau mỗi lần xóa.
            // Đối với mục đích đơn giản, ta có thể log ra hoặc trả về 0 và thực hiện đếm lại toàn bộ.
            // SQLite không trả về số hàng bị ảnh hưởng trực tiếp từ execSQL cho DELETE.
            // Bạn có thể làm như sau để có số lượng ước tính (trước khi xóa)
            long initialCount = getCountRecord(tableThuoc); // Giả sử getThuocCount đã có
            db.execSQL(query); // Thực hiện xóa
            long finalCount = getCountRecord(tableThuoc);
            deletedRows = (int) (initialCount - finalCount);

            Log.d(TAG, "Deleted " + deletedRows + " duplicate records from table " + tableThuoc);

        } catch (Exception e) {
            Log.e(TAG, "Error deleting duplicate records from table " + tableThuoc + ": " + e.getMessage());
            deletedRows = 0;
        } finally {
            // Không đóng database ở đây
            //dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng readLock
        }
        return deletedRows;
    }

    // Đảm bảo CREATE_TABLE_URL_QUEUE có các cột này:
// public static final String CREATE_TABLE_URL_QUEUE = "CREATE TABLE " + TABLE_URL_QUEUE + " (" +
//         ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
//         URL + " TEXT UNIQUE," + // URL phải là UNIQUE
//         STATUS + " INTEGER," +
//         LEVEL + " INTEGER," + // Thêm cột LEVEL
//         PARENT_ID + " INTEGER," + // Thêm cột PARENT_ID
//         CREATED_AT + " TEXT" +
//         ");";


    /**
     * Lấy danh sách các UrlInfo (URL, Level, ParentId) dựa trên trạng thái (STATUS).
     *
     * @param status Trạng thái của URL (0: chờ xử lý, 1: đã xử lý).
     * @return Danh sách các đối tượng UrlInfo.
     */
    //public List<UrlInfo> getDetailedUrlInfoByStatus(String table_Url_Queue, @Nullable String urlEndWith, int status) {
    public List<UrlInfo> getDetailedUrlInfoByStatus(String table_Url_Queue, int status) {
        List<UrlInfo> urlInfos = new ArrayList<>();
        Cursor cursor = null;
        //dbWriteLock.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock
        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            String[] columns = {URL, LEVEL, PARENT_ID, STATUS}; // Các cột cần lấy

            //
//            String query;
//            if (urlEndWith != null && !urlEndWith.isEmpty()) {
//                query = "SELECT URL,LEVEL,PARENT_ID FORM " + table_Url_Queue + " WHERE URL LIKE '%" + urlEndWith + "'" +
//                        " AND STATUS = " + status;
//            } else {
//                query = "SELECT URL,LEVEL,PARENT_ID FORM " + table_Url_Queue + " WHERE STATUS = " + status;
//            }
//
//            cursor = db.rawQuery(query, null);
            //

            cursor = db.query(table_Url_Queue, columns,
                    STATUS + " = ?", new String[]{String.valueOf(status)},
                    null, null, null);

            Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.isBeforeFirst()=" + cursor.isBeforeFirst()); // <-- false
            Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.isAfterLast()=" + cursor.isAfterLast());     // <-- true
            Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.getCount()=" + cursor.getCount());           // <-- 1
            Log.d(TAG, "SQL Query: SELECT " + TextUtils.join(",", columns) + " FROM " + table_Url_Queue + " WHERE " + STATUS + " = " + status);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String url = cursor.getString(cursor.getColumnIndexOrThrow(URL));
                    int level = cursor.getInt(cursor.getColumnIndexOrThrow(LEVEL));
                    long parentId = cursor.getLong(cursor.getColumnIndexOrThrow(PARENT_ID));
                    if (url.contains("+")) {
                        int iStop = 0;
                        Log.d("CrawlType", "getLisUrLFromCrawlType: DAU CONG=" + url);
                    }

                    //thuocSQLite.ky_tu_search = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
                    //thuocSQLite.ma_thuoc = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn

                    //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
                    String maThuocP = "https://www..." +"key" + url.split("key")[1];    // Tạo Mã Thuốc giả chu Url cha
                    urlInfos.add(new UrlInfo(url, parentId, level, maThuocP, status, null, 0, -1,
                            System.currentTimeMillis()/1000));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting detailed URL info by status " + status + ": " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Không đóng DB ở đây
            //dbWriteLock.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }
        return urlInfos;
    }

    public void removeUrlFromInitUrlsTable(CrawlType crawlType, String url) {

//        dbWriteLock.lock();
        dbReadWriteLock.writeLock().lock(); // Dùng writeLock

        try {
            SQLiteDatabase db = this.getWritableDatabase(); // Lấy thể hiện DB trong khối khóa
            db.delete(crawlType.getUrlQueueTableName(), "url = ?", new String[]{url}); //initUrlsTable = crawlType.getUrlQueueTableName()
        } finally {
//            dbWriteLock.unlock();
            dbReadWriteLock.writeLock().unlock(); // Giải phóng writeLock

        }
    }

    public boolean isDataExists(CrawlType crawlType, String url) {
        //dbWriteLock.lock();
        dbReadWriteLock.readLock().lock(); // Dùng readLock (ĐỌC)
        SQLiteDatabase db = null;

        boolean exists;
        Cursor cursor = null;


        try {
            db = this.getReadableDatabase(); // Lấy thể hiện DB trong khối khóa
            cursor = db.query(crawlType.getTableThuocName(), new String[]{"url"}, "url = ?", new String[]{url}, null, null, null);
            exists = cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            //dbWriteLock.unlock();
            dbReadWriteLock.readLock().unlock(); // Giải phóng readLock
        }

        return exists;
    }

    private void deleteOldDataThuoc(CrawlType crawlType, String url) {
        dbReadWriteLock.writeLock().lock(); // Dùng writeLock
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase(); // PHẢI LÀ getWritableDatabase()
            db.delete(crawlType.getTableThuocName(), "url = ?", new String[]{url});
        } finally {
            dbWriteLock.unlock();
        }

    }

    public long getUrl_id (SQLiteDatabase db, String tableUrlParent, String url) {
        long url_id = -1;
        try (Cursor cursor = db.query(tableUrlParent, new  String[] {ID}, URL + "=?", new String[]{url}, null, null, null);) {
            if (cursor.moveToFirst()) {
                url_id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
            }
        }
        return url_id;
    }

    // Xem lại trùng chức năng
    public boolean isMaThuocExists(String tableThuocCon, String maThuoc, String url) { // Cần không vì initUrlsTable có url là khóa chính
        // CHÚ Ý HM NÀY TÌM MÃ THUỐC MÀ CÓ URL "KHÁC" url ĐANG XÉT THÌ MỚI LÀ TRUE
        // Lấy writable database ngay trước khi sử dụng
        String tableUrlCha;
        if (tableThuocCon.endsWith("2kt")) {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_2KT;
        } else if (tableThuocCon.endsWith("3kt")) {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_3KT;
        } else {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_CUSTOM;
        }

        dbReadWriteLock.readLock().lock();
        try {
            SQLiteDatabase db = this.getReadableDatabase(); // LẤY DB KHI CẦN
            // Lây url_id từ Table Cha (là ID)
            long url_id = this.getUrl_id(db, tableUrlCha, url);
            if (url_id == -1) {
                return false;
            }
            //String ky_tu_search = Utils.getKyTuSeach(url);   // tableThuoc = table_thuoc_2kt: ĐÃ BỎ CỘT url
            Cursor cursor = db.query(tableThuocCon, new String[]{URL_ID}, MA_THUOC + "=?" + " AND " + URL_ID + "!=?" , new String[]{maThuoc, String.valueOf(url_id)}, null, null, null);
            //Cursor cursor = db.query(tableThuocCon, new String[]{MA_THUOC}, MA_THUOC + "=?" + " AND " + URL_ID + "!=?" , new String[]{maThuoc, String.valueOf(url_id)}, null, null, null);
            boolean exists = cursor.getCount() > 0;
            // XEM
//            if (exists) {
//                int stop=0;
//            }

            cursor.close();
            return exists;
        } finally {
            dbReadWriteLock.readLock().unlock();
        }
    }

    // Thêm
    // CẬP NHẬT PHƯƠNG THỨC loadQueueUrls (nếu có) tương tự như loadCompletedUrls
    private void loadQueueUrls(String tableName, ConcurrentLinkedQueue<String> urlQueue) {

        // Tương tự: loadCompletedUrls
        // ...
        // Thay vì: SQLiteDatabase db = dbHelperThuoc.getWritableDatabase();
        // Cứ gọi dbHelperThuoc.getReadableDatabase() khi cần.
        // Cursor cursor = db.rawQuery(...);
        // Thay thành:
        // Cursor cursor = dbHelperThuoc.getReadableDatabase().rawQuery(...);
        // ...

        Cursor cursor = null;
        try {
            // Lấy writable database ngay trước khi sử dụng
            SQLiteDatabase db = this.getWritableDatabase(); // LẤY DB KHI CẦN
            // ... (code sử dụng cursor)
        } catch (Exception e) {
            Log.e(TAG, "Error loading queue URLs: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // KHÔNG đóng DB ở đây. SQLiteOpenHelper quản lý.
        }
    }

    public List<UrlInfo> getListQueueUrls(SettingsRepository settingsRepository) {
        String[] arrayKyTu = new String[0];
        //int k0 = settingsRepository.getK0(crawlType.getK0PrefKey()); // Đảm bảo getK0 nhận PrefKey
        //int k1 = settingsRepository.getK1(crawlType.getK1PrefKey()); // Đảm bảo getK1 nhận PrefKey
        arrayKyTu = GetKyTuAZ.getArrayAZk0k1(settingsRepository);
        CrawlType crawlType = settingsRepository.getSelectedCrawlType();

        // Lưu tổng Ký tự LẤY ĐƯỢC, CŨNG = list URL.size
        settingsRepository.saveTotalUrls(crawlType.getTotalUrlsPrefKey(), arrayKyTu.length);
        /** PHẢI DELETE HẾT RECORD TRONG BẢNG: crawlType.getUrlQueueTableName()
         * RỒI LOAD URL MỚI TÙY THEO HỆ SỐ K0, K1 LẠI
         */
        this.deleteAllRecords(crawlType.getUrlQueueTableName());
        SQLiteDatabase dbThuoc = this.getWritableDatabase();    // Không cần close

        // Lấu list urlInfos: Mỗi Phần tử urlInfos là 1 record
        List<UrlInfo> urlInfos = new ArrayList<>();
        String kytu;
        //long parentId = 0;  // parentId: PHẢI LÀ k0 ĐỂ ĐẢM BẢO THEO THỨ TỰ: VÌ CÓ THỂ CHỌN BAN ĐẦU K0=3
        long parentId = settingsRepository.getK0(crawlType.getK0PrefKey()); // Đảm bảo getK0 nhận PrefKey
        // Ghi URL vào bảng initUrlsTable
        //SQLiteDatabase dbThuoc = DBHelperThuoc.getInstance(context).getWritableDatabase();
        for (int n=0; n < arrayKyTu.length; n++){
            kytu = arrayKyTu[n];
            if (kytu.trim().isEmpty()) continue; //Chỉ kiểm tra url rỗng (hoặc chỉ chứa các khoảng trắng)
            // Vẫn lấy kytu có khoảng trắng ở đầu và cuối
//            String[] row = new String[3];
//            row[0] = String.valueOf(parentId);  //parentId
//            row[1] = String.valueOf(1);     //Cũ: level = 0; Mới: level (thành page) = 1
//            row[2] = (AppConstants.URL0 + kytu + AppConstants.URL1).trim();   //url
//
//            // Xem: tìm lỗi thiếu đếm processedUrlStart1Counter thiếu 1
//            //Log.d(TAG, "loadUrlsFromAsset: processedUrlStart1Counter, url (start=1) = " + row[2]+ ";parentId=" + row[0]);
//            //
//            listRowUrls.add(row);   // Hoặc chỉ 1 lệnh: listRowUrls.add(new String[]{String.valueOf(parentId), String.valueOf(parentId), (url0 + kytu + url1).trim()});

//            kytu = url0 + kytu + url1;
//            String trimmedUrl = kytu.trim(); //Cắt khoảng trắng ở đầu và cuối
//            urls.add(trimmedUrl);

            // Sử dụng List<UrlInfo> urlInfos

            String url = (AppConstants.URL0 + kytu + AppConstants.URL1).trim();   //url
            // Xem
            if ((url.contains("+"))||url.contains("A+")||(url.contains("+A"))) {
                int iStop = 0;
                Log.d("CrawlType", "getLisUrLFromCrawlType: DAU CONG=" + url);
            }

            //int level = 1;     //Cũ: level = 0; Mới: level (thành page) = 1
            //long parentId = String.valueOf(parentId);  //parentId
            //urlInfos.add(new UrlInfo(url, level, parentId++));
            // Giá trị trả về
            //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            String maThuocP = "https://www..." +"key" + url.split("key")[1];
            urlInfos.add(new UrlInfo(AppConstants.URL0 + kytu + AppConstants.URL1.trim(), parentId, 1, maThuocP,0,
                    null, 0, -1, System.currentTimeMillis()/1000));

            // Ghi URL vào bảng urlQueueTableName
            ContentValues values = new ContentValues();
            values.put(DBHelperThuoc.PARENT_ID, parentId);
            // LEVEL: mặt đinh = 1
            //values.put(DBHelperThuoc.LEVEL, 1); //Cũ: Của url cha level = 0 (Mặc định); Mới: level (thành page) = 1 (Mặc định)
            // STATUS: BAN ĐẦU mặt đinh = 0 chưa xử lý
            //values.put(STATUS, 0); // 0: chưa xử lý, có thể thêm cột 'completed' nếu cần. Mặc định completed = 0 (uncompleted)
            values.put("url", (AppConstants.URL0 + kytu + AppConstants.URL1).trim());

            //values.put(ERR_MESSAGE, "");    //Ban đầu chưa có ERR_MESSAGE=""
            //values.put(LAST_RECORD_INDEX, -1);  //-1: Mặc đinh= -1, nghĩa là BAN ĐÀU chưa có record cuối thu được
            //values.put(DBHelperThuoc.BOOLEAN_URL_CHILD, 0); // Mặc định ban đầu là 0 (false) : không phai là url Child (con)
            //Cũ: có ném lô
            //dbThuoc.insert( initUrlsTable, null, values); //initUrlsTable. tableNameThuoc_Nao phải có
            //Mới: không ném lỗi
            //dbThuoc.insertWithOnConflict( initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
            dbThuoc.insertWithOnConflict( crawlType.getUrlQueueTableName(), null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
            parentId++;
        }

        return urlInfos;

    }

    public static class ColumnInfoUrlCha {
        public final String dbColumnName;      // Tên cột trong SQLite
        public final String headerName; // Tiêu đề cột trong Excel
        //public final String title;             // Tiêu đề cột trong Excel
        public final String dbLinkColumnName;  // Nếu cần hyperlink
        public final int width;
        public final boolean wrapText;
        public int columnIndex = -1;           // Gán sau khi query
        public int linkColumnIndex = -1;

        public ColumnInfoUrlCha(String dbColumnName, String headerName, String dbLinkColumnName, int width, boolean wrapText) {
            this.dbColumnName = dbColumnName;
            //this.title = title;
            this.headerName = headerName;
            this.dbLinkColumnName = dbLinkColumnName;
            this.width = width;
            this.wrapText = wrapText;
        }
    }

    public static final List<ColumnInfoUrlCha> EXPORT_COLUMNS_CHA = Arrays.asList(
            // Ý nghĩa: ColumnInfo(String dbCol, String header, String linkCol, int width, boolean wrapText)new ColumnInfo(DBHelperThuoc.PARENT_ID, "PARENT_ID", null, 12 * 256, false));    // hoặc PARENT_ID=Mã cha
            new ColumnInfoUrlCha(PARENT_ID, "PARENT_ID", null, 12 * 256, false),    // hoặc PARENT_ID=Mã cha
            new ColumnInfoUrlCha(LEVEL, "Cấp độ", null, 8 * 256, false),            // Hoặc : LEVEL

            new ColumnInfoUrlCha(KY_TU_SEARCH, "KÝ TỰ SEARCH", null, 15 * 256, false),  //"Chữ cái tìm kiếm"

            // URL = MA_THUOC_LINK_P: ĐẶT TRƯỚC KY_TU_SEARCH ĐỂ DUYỆT TRƯỚC ĐỂ LẤY ĐƯỢC KY_TU_SEARCH (SAU)
            new ColumnInfoUrlCha(MA_THUOC_P, "Mã thuốc", URL_P, 20 * 256, false),
            //TEN_THUOC: dùng để ghi lại ghi chú
            new ColumnInfoUrlCha(TEN_THUOC, "Tên thuốc", null, 25 * 256, true),
            new ColumnInfoUrlCha(GHI_CHU, "Ghi chú", null, 25 * 256, true)
            //new ColumnInfo(INDEX_COLOR, "Màu dòng", null, 8 * 256, false);
    );

    /// Chuyển từ ExcelExporterOptimized  sang DBHelperThuoc
    public static class ColumnInfo {
        public final String dbColumnName;      // Tên cột trong SQLite
        public final String headerName; // Tiêu đề cột trong Excel
        //public final String title;             // Tiêu đề cột trong Excel
        public final String dbLinkColumnName;  // Nếu cần hyperlink
        public final int width;
        public final boolean wrapText;
        public int columnIndex = -1;           // Gán sau khi query
        public int linkColumnIndex = -1;

        public ColumnInfo(String dbColumnName, String headerName, String dbLinkColumnName, int width, boolean wrapText) {
            this.dbColumnName = dbColumnName;
            //this.title = title;
            this.headerName = headerName;
            this.dbLinkColumnName = dbLinkColumnName;
            this.width = width;
            this.wrapText = wrapText;
        }
    }

    public static final List<ColumnInfo> EXPORT_COLUMNS = Arrays.asList(
            // Ý nghĩa: ColumnInfo(String dbCol, String header, String linkCol, int width, boolean wrapText)new ColumnInfo(DBHelperThuoc.PARENT_ID, "PARENT_ID", null, 12 * 256, false));    // hoặc PARENT_ID=Mã cha
            new ColumnInfo(PARENT_ID, "PARENT_ID", null, 12 * 256, false),    // hoặc PARENT_ID=Mã cha
            new ColumnInfo(LEVEL, "Cấp độ", null, 8 * 256, false),            // Hoặc : LEVEL
            new ColumnInfo(KY_TU_SEARCH, "KÝ TỰ SEARCH", null, 15 * 256, false),  //"Chữ cái tìm kiếm"
            new ColumnInfo(MA_THUOC, "Mã thuốc", MA_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(TEN_THUOC, "Tên thuốc", null, 25 * 256, true),
            new ColumnInfo(THANH_PHAN, "Thành phần", THANH_PHAN_LINK, 25 * 256, true),
            new ColumnInfo(NHOM_THUOC, "Nhóm thuốc", NHOM_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(DANG_THUOC, "Dạng thuốc", DANG_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(SAN_XUAT, "Nhà sản xuất", SAN_XUAT_LINK, 20 * 256, true),
            new ColumnInfo(DANG_KY, "Đăng ký", DANG_KY_LINK, 15 * 256, false),
            new ColumnInfo(PHAN_PHOI, "Phân phối", PHAN_PHOI_LINK, 20 * 256, false),
            new ColumnInfo(SDK, "Số đăng ký", SDK_LINK, 20 * 256, false),
            new ColumnInfo(CAC_THUOC, "Các thuốc cùng nhóm", CAC_THUOC_LINK, 25 * 256, true),    //nếu còn phía sau thì thêm ","
            new ColumnInfo(GHI_CHU, "Ghi chú", null, 25 * 256, true)    // Tạo cột Ghi chú cho sheet Excel
            //new ColumnInfo(INDEX_COLOR, "Màu dòng", null, 8 * 256, false);
    );

    /*
     *
     * @return
     */
    public boolean hasErrorUrls(CrawlType crawlType) {
        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + crawlType.getUrlQueueTableName() +  " WHERE status=0", null);
        Cursor cursor = db.rawQuery("SELECT COUNT(*) AS cnt FROM " + crawlType.getUrlQueueTableName() + " WHERE status=0",
                null);
        Log.d("DB", "rowCount=" + cursor.getCount());
        Log.d("DB", "colCount=" + cursor.getColumnCount());
        Log.d("DB", "colName=" + cursor.getColumnName(0));
        // Mói
        long count = DatabaseUtils.longForQuery(
                db,
                "SELECT COUNT(*) FROM " + crawlType.getUrlQueueTableName() + " WHERE status=0",
                null
        );
        return count > 0;

        // Cũ: lỗi
//        boolean hasErrors = false;
//        if (cursor != null) { // Kiểm tra có bị close? Tại sao
//            if (cursor.moveToFirst()) {
//                Log.d("DB", "isAfterLast=" + cursor.isAfterLast());
//                Log.d("DB", "isBeforeFirst=" + cursor.isBeforeFirst());
//                Log.d("DB", "position=" + cursor.getPosition());
//                int idx = cursor.getColumnIndex("cnt"); // luôn = 0
//                Log.d("DB", "idx=" + idx);
//                if (idx >= 0) {
//                    Log.d("DB", "value=" + cursor.getString(idx)); // thử getString thay vì getInt
//                }
//
//                //hasErrors = cursor.getInt(0) > 0; // luôn hợp lệ khi cursor chưa bị đóng
//                hasErrors = cursor.getInt(cursor.getColumnIndexOrThrow("cnt")) > 0;
//            }
//            cursor.close();
//        }
//
//        return hasErrors;
    }

    public long getCountErrorUrls(String tableQueue) {
        // Tại Sao còn Status =0 sau khi worke return SUCCEEDED
        SQLiteDatabase db = this.getReadableDatabase();

        long numErrors = DatabaseUtils.longForQuery(
                db,
                "SELECT COUNT(*) FROM " + tableQueue + " WHERE status=0",
                null
        );
        return numErrors;
        //PP Cũ: dùng SELECT COUNT(*),  Bị Lỗi
//        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableQueue +" WHERE status=0", null);
//        int numErrors = 0;
//        if (cursor.moveToFirst()) {
//            numErrors = cursor.getInt(0);   // Bị lỗi
//        }
//        cursor.close();
//        return numErrors;
    }

    /**
     *
     * @param url
     * @param status
     * @param errorMessage
     */
    public void updateErrorMessageException(String tableQueue, String url, int status, String errorMessage) {
        dbWriteLock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(STATUS, status);
            values.put(ERR_MESSAGE, errorMessage);
            db.update(tableQueue, values, "url=?", new String[]{url});
        } finally {
            dbWriteLock.unlock();
        }
    }

    /**
     *
     * @return
     */
    public List<ErrorUrl> getListErrorUrls(CrawlType crawlType) {
        List<ErrorUrl> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PARENT_ID + ", " + LEVEL + ", " + URL+ ", " + STATUS + ", " + ERR_MESSAGE + " FROM " + crawlType.getUrlQueueTableName() + " WHERE status=0", null);
        //cursor = db.query(crawlType.getUrlQueueTableName(), new String[]{PARENT_ID,LEVEL, URL, STATUS, ERR_MESSAGE}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                //int id = cursor.getInt(0);
                String url = cursor.getString(cursor.getColumnIndexOrThrow(URL));
//                long parentId = cursor.getLong(cursor.getColumnIndexOrThrow(PARENT_ID));
//                int level = cursor.getInt(cursor.getColumnIndexOrThrow(LEVEL));

                int status = cursor.getInt(cursor.getColumnIndexOrThrow(STATUS));
                String errMsg = cursor.getString(cursor.getColumnIndexOrThrow(ERR_MESSAGE));
                //list.add(new ErrorUrl(parentId, level, url, status, errMsg));
                list.add(new ErrorUrl(url, status, errMsg));

            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }



    /**
     * Lấy đường dẫn tuyệt đối của file .db
     */
    public String getDbPath() {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    /**
     * Lấy đường dẫn file WAL
     */
    public String getWalPath() {
        return getDbPath() + "-wal";
    }

    /**
     * Lấy đường dẫn file SHM
     */
    public String getShmPath() {
        return getDbPath() + "-shm";
    }



}

