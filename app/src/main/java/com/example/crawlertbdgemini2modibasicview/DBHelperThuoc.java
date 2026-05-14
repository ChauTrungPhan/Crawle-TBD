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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBHelperThuoc extends SQLiteOpenHelper {
    private static final String TAG = "DBHelperThuoc";
    // Tên và Version Database
    public static final String DATABASE_NAME = "ThuocBietDuoc";
    private static final int DATABASE_VERSION = 2; // Tăng lên 2 để cập nhật cấu trúc bảng (thêm id_url)

    // Singleton instance và đối tượng DB dùng chung
    private static volatile DBHelperThuoc instance; //single connection tái sử dụng
    private SQLiteDatabase db;  //db=mDatabase
    private final Context mContext;
    //private final Context context;

    // Cơ chế Lock để bảo vệ đa luồng
    // WriteLock: Dùng cho Insert/Update/Delete
    // ReadLock: Dùng cho Query/Select
    private final ReadWriteLock dbLock = new ReentrantReadWriteLock();

    //// Cũ
    //private static final Lock dbWriteLock = new ReentrantLock();
    //private static final ReentrantLock dbWriteLock = new ReentrantLock();
    //Thay ReentrantLock = ReentrantReadWriteLock
    //public static final ReadWriteLock dbReadWriteLock = new ReentrantReadWriteLock();
    // end Cũ
    //Cách sử dụng nhiều khóa THEO tên bảng (table): CHÚ Ý THỨ TỤ TRÁNH BỊ DEADLOCK: KHÔNG NÊN
//    private static final Lock lock1TableThuoc = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi
//    private static final Lock lock2CompletedUrlsTable = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi
//    private static final Lock lock3InitUrlsTable  = new ReentrantLock(); //Chỉ sử dụng 1 khóa lock cho tất cả phương thức ghi
        //////////////////////
    // Bảng cha: chứa thông tin url
    public static final String TABLE_PARENT_URLS_2KT = "table_parent_urls_2kt";
    public static final String TABLE_PARENT_URLS_3KT = "table_parent_urls_3kt";
    public static final String TABLE_PARENT_URLS_CUSTOM = "table_parent_urls_custom";
    // Các bảng con: chứa thông tin thuốc, LÀ CÁC RECORD
    public static final String TABLE_THUOC_2KT = "table_thuoc_2kt";
    public static final String TABLE_THUOC_3KT = "table_thuoc_3kt";
    public static final String TABLE_THUOC_CUSTOM = "table_thuoc_custom";
    // Của Table Parent (cha)
    public static final String ID_URL = "id_url";   //PARENT_ID_OF_CRAWLED_URL: FK tham chiếu đến ID (là số nguyên sẽ nhanh nhất, không tốn dữ liệu)
    //public static final String ID_URL = "id_url";   //FK tham chiếu đến ID (là số nguyên sẽ nhanh nhất, không tốn dữ liệu)
    public static final String URL = "url"; //Của Cha(lưu link là url)
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

    public static final String P_URL = "p_url"; //Cha
    public static final String STATUS = "status";
    public static final String LAST_ACCESSED = "last_accessed";
    public static final String KY_TU_SEARCH = "ky_tu_search";


    //1. BẢNG CON: String tạo bản con chứa thông tin thuốc: Xem như bảng con (child)
    public static final String createThuoc2kt_child  =
            "CREATE TABLE " +  TABLE_THUOC_2KT + "(" +      // %s SẼ ĐƯỢC THAY = BIẾN CHUỖI
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                    ID_URL + " INTEGER NOT NULL, " +   // <-- khóa ngoại: FK tham chiếu tableParent.id (số nguyên sẽ nhanh)

                    //URL + " TEXT NOT NULL, " +                     // <-- URL làm khóa ngoại: sẽ chậm, tốn ram, do chuỗi dài truy xuất sẽ cậm

                    // MA_THUOC: Chỉ cần NOT NULL ở đây, UNIQUE khai báo cuối bảng cho rõ ràng
                    MA_THUOC + " TEXT NOT NULL, " +
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
                    // Khai báo khóa ngoại FOREIGN KEY phải đặt cuối cùng (ngoài định nghĩa cột)
                    //"FOREIGN KEY(" + URL +") REFERENCES %s(" + URL + ") ON DELETE CASCADE, " +
                    // Hoặc viết
                    "FOREIGN KEY("+ ID_URL + ") REFERENCES " + TABLE_PARENT_URLS_2KT + "(" + ID + ") ON DELETE CASCADE, " +
                    //"UNIQUE(" + ID_URL + ", " + MA_THUOC + ")" +  // 1 ID_URL chỉ chứa 1 mã thuốc duy nhất
                    // Đảm bảo mã thuốc là duy nhất trên toàn bảng (Global Unique)
                    //"UNIQUE(" + MA_THUOC + ")" +  // Toàn cầu (Toàn bảng): HỢP LÝ HƠN
                    "CONSTRAINT unique_ma_thuoc UNIQUE (" + MA_THUOC + ")" +

                    ");";

    ///
    //2. BẢNG CHA (PARENT): String tạo bản CHA chứa URL: : MỚI THÊM
    public static final String
            CREATE_PARENT_URLS =
            "CREATE TABLE %s (" +      // %s SẼ ĐƯỢCTHAY = BIẾN CHUỖI
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    //ID + " INTEGER NOT NULL, " + // KHÔNG CẦN ID, VÌ ĐÃ CÓ URL LÀ KHÓA CHÍNH
                    ID_URL + " INTEGER NOT NULL UNIQUE, " +        // Chứa thứ tự cào url
                    LEVEL + " INTEGER NOT NULL, " +          //Hoặc: ID_URL+LEVEL LÀM KHÓA CHÍNH

                    //KY_TU_SEARCH + " TEXT, " +              //KY_TU_SEARCH + " TEXT, " +: KHÔNG CẦN, LÂY TỪ URL
                    URL + " TEXT NOT NULL UNIQUE, " +        //UNIQUE: CŨNG LÀ LINK, tương đương tên: MA_THUOC_LINK
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
                    ID_URL + " INTEGER NOT NULL, " +
                    LEVEL + " INTEGER NOT NULL DEFAULT 1, " +   //đặt định ban đầu tùy chọn: 1 hoặc 0
                    STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                    KY_TU_SEARCH + " TEXT, " +
                    ERR_MESSAGE + " TEXT, " +   //GHI_CHU
                    LAST_RECORD_INDEX + " INTEGER DEFAULT -1, " +
                    LAST_ACCESSED + " INTEGER DEFAULT " + System.currentTimeMillis()/1000 +       //LAST_ACCESSED hay CREATED_AT
                    ");";

    /**
     * 1. Constructor Private: Chỉ khởi tạo các tham số cơ bản.
     * Không thực hiện mở hay đóng database ở đây.
     */
    private DBHelperThuoc(@Nullable Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();
    }

    /**
     * 2. Singleton: Đảm bảo toàn app chỉ có một Helper duy nhất.
     */
    public static DBHelperThuoc getInstance(Context ctx) {  // KHÔNG synchronized TÒAN HÀM SẼ NHANH HƠN
        if (instance == null) {
            synchronized (DBHelperThuoc.class) {
                if (instance == null) {
                    instance = new DBHelperThuoc(ctx);
                }
            }
        }
        return instance;
    }

    /**
     * 3. onConfigure: Nơi quan trọng nhất để thiết lập chế độ vận hành.
     * Được gọi trước onCreate/onUpgrade.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Kích hoạt Write-Ahead Logging (WAL)
        // Giúp Crawler có thể Ghi dữ liệu mà không làm treo luồng Đọc của UI
        db.enableWriteAheadLogging();

        // Bật Foreign Key để đảm bảo tính toàn vẹn dữ liệu cha-con
        db.setForeignKeyConstraintsEnabled(true);

        // Tối ưu tốc độ ghi: NORMAL là sự cân bằng tốt nhất giữa tốc độ và an toàn
        db.execSQL("PRAGMA synchronous = NORMAL");

        Log.d(TAG, "Database configured: WAL and Foreign Keys enabled.");
    }

    /** Bạn nên xóa hoàn toàn phương thức close() ghi đè đó đi,
     *  hoặc chỉ dùng nó để Log mà không gọi super.close() (tuy nhiên cách này không khuyến khích vì dễ gây nhầm lẫn).
     Cách làm chuẩn:

     * Bỏ hàm close(): Để SQLiteOpenHelper tự lo.
     * Sử dụng kết nối xuyên suốt: Bạn đã có hàm getDb() trả về một instance duy nhất, hãy cứ để nó mở.

     * 4. Vậy khi nào thì mới cần đóng?
     * Thông thường, bạn chỉ cần đóng Database trong các trường hợp cực kỳ đặc biệt:
     * Bạn muốn copy file database ra ngoài hoặc ghi đè file database mới vào.
     * Ứng dụng có chức năng "Đăng xuất" và bạn muốn giải phóng toàn bộ dữ liệu liên quan đến người dùng đó.

     * Trong các bài Unit Test dữ liệu.

     */
//    @Override
//    public synchronized void close() {
//        Log.d("DB_LIFECYCLE", "DB CLOSE by thread: " + Thread.currentThread().getName(), new Throwable("DB Close Trace"));
//        super.close();
//    }

    /** Thay thế cho public synchronized void close()
     * Hàm chủ động đóng Database có ghi log trace.
     * Chỉ dùng khi cần bảo trì (Copy DB) hoặc thoát hẳn ứng dụng.
     *
     * Cách sử dụng trong các tình huống thực tế:
     * 1/ Tình huống A: Khi bạn nhấn Button "Chép Database"
     * Đây là lúc quan trọng nhất cần dùng đến hàm này.
     * VD:
     * btnExportDb.setOnClickListener(v -> {
     *     // 1. Chủ động đóng để flush hết file -wal vào file chính
     *     dbHelper.logAndClose();
     *
     *     // 2. Thực hiện hàm copy file .db (lúc này file đã an toàn để copy)
     *     copyDatabaseFile();
     *
     *     // 3. Gửi mail hoặc thông báo thành công
     *     Toast.makeText(context, "Đã sẵn sàng gửi DB!", Toast.LENGTH_SHORT).show();
     *
     *     // Lưu ý: Nếu sau đó bạn tiếp tục Crawl, chỉ cần gọi dbHelper.getDb(),
     *     // kết nối sẽ tự động mở lại.
     * });
     *
     * 2/ Tình huống B: Khi người dùng Đăng xuất (Logout)
     * Bạn muốn dọn dẹp sạch sẽ tài nguyên trước khi thoát. (Gọi hàm performLogout())
     * VD:
     * public void performLogout() {
     *         // Dừng các luồng Crawler trước (nếu có)
     *         crawlerManager.stopAllThreads();
     *
     *         // Sau đó mới đóng DB
     *         dbHelper.logAndClose();
     *
     *         // Chuyển về màn hình đăng nhập
     *     }
     */
    public void logAndClose() {
        dbLock.writeLock().lock(); // Chặn mọi thao tác khác để đóng an toàn
        try {
            if (db != null && db.isOpen()) {
                // Ghi log kèm theo StackTrace để biết chính xác code ở đâu gọi hàm này
                Log.w(TAG, "!!! CHỦ ĐỘNG ĐÓNG DATABASE !!! Caller: " + Thread.currentThread().getName(),
                        new Throwable("Trace đóng DB"));

                db.close();
                db = null;        // Xóa tham chiếu để getDb() có thể khởi tạo lại nếu cần
                instance = null;  // Reset instance singleton
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi đóng database: " + e.getMessage());
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    private void createTableSet(SQLiteDatabase db, String suffix, String parentTableName, String childTableName, String queueTableName) {
        // 1. Tạo bảng Cha
        db.execSQL(String.format(CREATE_PARENT_URLS, parentTableName));

        // 2. Tạo bảng Con (Child)
        String createChildSql = createThuoc2kt_child
                .replace(TABLE_THUOC_2KT, childTableName)
                .replace(TABLE_PARENT_URLS_2KT, parentTableName);
        db.execSQL(createChildSql);

        // 3. Tạo bảng Queue
        db.execSQL(String.format(CREATE_URL_QUEUE_COMMON, queueTableName));

        // --- CÁC CHỈ MỤC (INDEX) BẮT BUỘC ĐỂ CHẠY NHANH ---
        // 4. TẠO INDEX (Sử dụng suffix để tên Index không bị trùng)
        // Index cho ID_URL để JOIN nhanh
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_urlId_" + suffix +
                " ON " + childTableName + "(" + ID_URL + ")");

        // Index cho MA_THUOC để kiểm tra trùng lặp (isMaThuocExists) nhanh
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_maThuoc_" + suffix +
                " ON " + childTableName + "(" + MA_THUOC + ")");

        // Index bảng Cha: Tìm URL cực nhanh (Dùng cho hàm isUrlExists)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_parentUrl_" + suffix + " ON " + parentTableName + "(" + URL + ")");

        // Bonus: Index cho STATUS(status = 0) ở bảng Cha để quét link chưa crawl nhanh hơn
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_queueStatus_" + suffix + " ON " + parentTableName + "(" + STATUS + ")");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // P. PHÁP tạo mới:
        // Tạo bộ bảng cho 2KT
        createTableSet(db, "2kt", TABLE_PARENT_URLS_2KT, TABLE_THUOC_2KT, TABLE_URLS_QUEUE_2KT);

        // Tạo bộ bảng cho 3KT
        createTableSet(db, "3kt", TABLE_PARENT_URLS_3KT, TABLE_THUOC_3KT, TABLE_URLS_QUEUE_3KT);

        // Tạo bộ bảng cho CUSTOM
        createTableSet(db, "custom", TABLE_PARENT_URLS_CUSTOM, TABLE_THUOC_CUSTOM, TABLE_URLS_QUEUE_CUSTOM);

        // Các bảng bổ trợ khác (Checkpoint...)
        //db.execSQL("CREATE TABLE IF NOT EXISTS CrawlCheckpoint (...)");

        //3. Checkpoint bền vững để phục hồi
        //Thêm bảng checkpoint (tuỳ chọn nhưng rất hữu ích):
        db.execSQL("CREATE TABLE IF NOT EXISTS CrawlCheckpoint (" +
                "  id INTEGER PRIMARY KEY CHECK (id=1), " +
                "  last_flush_seq INTEGER DEFAULT 0, " +      //-- số gói đã flush gần nhất
                "  last_flush_time INTEGER, " +               //-- epoch millis
                "  pending_in_queue INTEGER DEFAULT 0, " +    //-- ước lượng hàng đợi
                "  version INTEGER DEFAULT 1);"
        );


        /* Cũ
            // CŨ. 1. PP DÙNG CHA-CON Tạo Bản CON Thuoc
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
                    TABLE_THUOC_2KT +"(" + DBHelperThuoc.ID_URL +");"   //để tăng tốc join.
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
                    TABLE_THUOC_3KT +"(" + DBHelperThuoc.ID_URL +");"
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
                    TABLE_THUOC_CUSTOM +"(" + DBHelperThuoc.ID_URL +");"
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

         */

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

    /**
     * 4. getDb(): Lấy kết nối duy nhất dùng chung (singleton connection).
     * Đồng bộ hóa để tránh tạo nhiều kết nối khi nhiều luồng gọi cùng lúc.
     */
    public synchronized SQLiteDatabase getDb() {
        if (db == null || !db.isOpen()) {
            db = getWritableDatabase();
            Log.d("DB_LIFECYCLE", "DB OPEN (writable) by thread: " + Thread.currentThread().getName(), new Throwable("DB Open Trace"));
        }
        return db;
    }

    /**
     * 5. Ví dụ hàm Ghi Dữ Liệu: Sử dụng WriteLock
     */
    public long addUrlToQueue(String queueTableName, String url, int idUrl, int level, String kyTuSearch) {
        long result = -1;   //long newRowId = -1;
        dbLock.writeLock().lock(); // Khóa các luồng khác lại để ghi
        try {
            SQLiteDatabase db = getDb();
            ContentValues cv = new ContentValues();
            cv.put(URL, url);
            cv.put(ID_URL, idUrl);
            cv.put(LEVEL, level);
            cv.put(STATUS, 0);
            cv.put(KY_TU_SEARCH, kyTuSearch);

            result = db.insertWithOnConflict(queueTableName, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting URL", e);
        } finally {
            dbLock.writeLock().unlock(); // Luôn giải phóng khóa
        }
        return result;  // newRowId;
    }

    public void updateStatusUrlQueue(String queueTableName, String url, String errorMessage) {
        dbLock.writeLock().lock(); // Dùng readLock
        try {
            SQLiteDatabase db = getDb();
            ContentValues cv = new ContentValues();
            //values.put(URL, url);
            cv.put(ERR_MESSAGE, errorMessage);
            // URL already exists in queue: thì update lại
            int rowsAffected = db.update(queueTableName, cv, URL + " = ?", new String[]{url});
            Log.w(TAG, "URL already exists in queue, skipping insert: " + url + " in " + queueTableName);
        } catch (Exception e) {
            Log.e(TAG, "Error adding URL to queue: " + url + " in " + queueTableName, e);
        } finally {
            //dbWriteLock.unlock();
            // Đảm bảo khóa được giải phóng trong mọi trường hợp
            dbLock.writeLock().unlock(); // Giải phóng readLock
        }
        //return rowsAffected;  // Nếu muốn trả về giá trị tồng số hàng đã Update
    }

    public void initializeUrlQueue(String queueTableName, List<String> initialUrls, String[] kyTuArray) {
        // Sử dụng Write Lock vì có thao tác thay đổi dữ liệu (Delete, Insert, Update)
        dbLock.writeLock().lock(); // Dùng readLock
        SQLiteDatabase db = getDb();
        try {
            db.beginTransaction();
            // 1. Xóa các bản ghi cũ có Level > 0
            db.delete(queueTableName, LEVEL + " > 0", null);
            long currentTime = System.currentTimeMillis() / 1000;
            for (int i = 0; i < initialUrls.size(); i++) {
                String url = initialUrls.get(i);
                // kyTuSearch có vẻ chưa được dùng trong logic insert bên dưới,
                // bạn có thể bổ sung vào ContentValues nếu cần.
                String kyTuSearch = (i < kyTuArray.length) ? kyTuArray[i] : "";
                // 2. Kiểm tra URL đã tồn tại chưa bằng try-with-resources
                try (Cursor cursor = db.query(queueTableName, new String[]{ID}, URL + " = ?", new String[]{url}, null, null, null)) {
                    ContentValues cv = new ContentValues();
                    cv.put(STATUS, 0);
                    cv.put(LAST_ACCESSED, currentTime);

                    if (cursor != null && cursor.moveToFirst()) {
                        ContentValues updateValues = new ContentValues();
                        // Cập nhật nếu đã tồn tại
                        db.update(queueTableName, cv, URL + " = ?", new String[]{url});
                        Log.d(TAG, "Updated existing root URL in queue: " + url + " in " + queueTableName);
                    } else {
                        // Thêm mới nếu chưa có
                        cv.put(URL, url);
                        cv.put(LEVEL, 0);
                        cv.put(ID_URL, i + 1);   // Logic ID cha của bạn

                        db.insertOrThrow(queueTableName, null, cv);
                        Log.d(TAG, "Inserted new root URL to queue: " + url + " in " + queueTableName);
                    }
                }
                //
            }
            //
            db.setTransactionSuccessful();  // Đánh dấu giao dịch thành công nếu mọi thứ ổn
        } catch (Exception e) {
            Log.e(TAG, "Error initializing URL queue: " + queueTableName, e);
        } finally {
            // Luôn đảm bảo endTransaction() được gọi, bất kể thành công hay thất bại
            // KHÔNG GỌI db.close() Ở ĐÂY!
            //Không đóng database ở đây vì nó được quản lý bởi SQLiteOpenHelper
            // và có thể được sử dụng bởi các phương thức khác.
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
            dbLock.writeLock().unlock();    // Giải phóng readLock
        }
    }

    public Cursor getCursorUnprocessedUrlsFromQueue(String queueTableName, int limit) {
        SQLiteDatabase db = getDb();
        // Tách điều kiện lọc ra để dễ quản lý
        String selection = STATUS + " = ?";
        String[] selectionArgs = new String[]{"0"};

        // Sắp xếp: Ưu tiên Level thấp (BFS), sau đó đến ID cũ nhất
        String orderBy = LEVEL + " ASC, " + ID + " ASC";

        return db.query(
                queueTableName,
                new String[]{ID, URL, ID_URL, LEVEL, KY_TU_SEARCH},
                selection,       // STATUS + " = 0",
                selectionArgs,
                null,
                null,
                orderBy,      //LEVEL + " ASC, " + ID + " ASC",
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

        // Sử dụng WriteLock vì đây là thao tác cập nhật dữ liệu
        dbLock.writeLock().lock();

        try {
            //SQLiteDatabase db = this.getWritableDatabase();   //Gốc
            SQLiteDatabase db = getDb();
            // Cập nhật và lấy số dòng bị ảnh hưởng
            int rowsAffected = db.update(initUrlsTable, values, "url = ?", new String[]{url});
            // Dùng rowsAffected dể XEM có cập nhật thành công hay không
            if (rowsAffected == 0) {
                Log.d("DBHelperThuoc", "markUrlAsProcessing: Không thể cập nhật trạng thái của URL: " + url);
            } else {
                Log.d("DBHelperThuoc", "markUrlAsProcessing: Đã cập nhật trạng thái của URL: " + url + " thành Đang xử lý (status = 1)");
            }
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi cập nhật status URL: " + e.getMessage());
        } finally {
            dbLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public void markUrlAsProcessedInQueue(String queueTableName, String url) {
        // Đã Cào dữ liệu thuốc HOÀN THÀNH THÌ: status = 1
        ContentValues values = new ContentValues();
        values.put(STATUS, 1);
        // Lưu timestamp dạng long (giây)
        values.put(LAST_ACCESSED, System.currentTimeMillis()/1000);    //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())

        dbLock.writeLock().lock(); // Dùng writeLock

        try {
            db = getDb();
            //int rowsAffected = dbThuoc.update(queueTableName, values, ID + " = ?", new String[]{urlId});
            int rowsAffected = db.update(queueTableName, values, URL + " = ?", new String[]{url});
            if (rowsAffected > 0) {
                Log.d(TAG, "Marked URL ID " + url + " as processed in queue " + queueTableName + ".");

                //Log.d(TAG, "Marked URL ID " + urlId + " as processed in queue " + queueTableName + ".");
            } else {
                Log.w(TAG, "Failed to mark URL ID " + url + " as processed (not found or already processed) in " + queueTableName + ".");

                //Log.w(TAG, "Failed to mark URL ID " + urlId + " as processed (not found or already processed) in " + queueTableName + ".");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi markUrlAsProcessedInQueue: " + e.getMessage());
        } finally {
            dbLock.writeLock().unlock(); // Giải phóng writeLock
        }
    }

    public void resetUrlQueueTable(String queueTableName) {
        dbLock.writeLock().lock(); // Dùng writeLock
        SQLiteDatabase db = getDb();
        db.beginTransaction(); // Bước 1: (Ngoài try) Nếu Bước 1 thành công, chắc chắn Transaction đang mở.
        try {
            // 1. Xóa các URL cấp độ sâu (level > 0)
            db.delete(queueTableName, LEVEL + " > 0", null);

            // 2. Reset trạng thái các URL gốc (level = 0) về chưa xử lý (status = 0)
            ContentValues cv = new ContentValues();
            cv.put(STATUS, 0);
            cv.put(LAST_ACCESSED, System.currentTimeMillis()/1000);
            db.update(queueTableName, cv, LEVEL + " = 0", null);

            // Bước 2: Xử lý dữ liệu
            db.setTransactionSuccessful();
            Log.d(TAG, "Đã reset bảng queue: " + queueTableName);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi reset URL queue table: " + queueTableName, e);
        } finally {
            db.endTransaction(); // Bước 3
            //dbWriteLock.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock
        }
    }

    public long insertOrUpdateThuoc(String tableName, ThuocSQLite thuoc) {

        ContentValues cv = new ContentValues();
        //cv.put(URL, thuoc.url);
        cv.put(ID_URL, thuoc.id_url);
        //values.put(ID_URL, thuoc.id_url);
        //values.put(LEVEL, thuoc.level);
        //values.put(KY_TU_SEARCH, thuoc.ky_tu_search);
        cv.put(MA_THUOC, thuoc.ma_thuoc);
        cv.put(TEN_THUOC, thuoc.ten_thuoc);
        cv.put(THANH_PHAN, thuoc.thanh_phan);
        cv.put(NHOM_THUOC, thuoc.nhom_thuoc);
        cv.put(DANG_THUOC, thuoc.dang_thuoc);
        cv.put(SAN_XUAT, thuoc.san_xuat);
        cv.put(DANG_KY, thuoc.dang_ky);
        cv.put(PHAN_PHOI, thuoc.phan_phoi);
        cv.put(SDK, thuoc.sdk);
        cv.put(CAC_THUOC, thuoc.cac_thuoc);
        cv.put(MA_THUOC_LINK, thuoc.ma_thuoc_link);
        cv.put(THANH_PHAN_LINK, thuoc.thanh_phan_link);
        cv.put(NHOM_THUOC_LINK, thuoc.nhom_thuoc_link);
        cv.put(DANG_THUOC_LINK, thuoc.dang_thuoc_link);
        cv.put(SAN_XUAT_LINK, thuoc.san_xuat_link);
        cv.put(DANG_KY_LINK, thuoc.dang_ky_link);
        cv.put(PHAN_PHOI_LINK, thuoc.phan_phoi_link);
        cv.put(SDK_LINK, thuoc.sdk_link);
        cv.put(CAC_THUOC_LINK, thuoc.cac_thuoc_link);
        //cv.put(GHI_CHU, thuoc.ghi_chu);
        //cv.put(INDEX_COLOR, thuoc.index_color);
        cv.put(CREATED_AT, System.currentTimeMillis()/1000);

        long resultId = -1;
        //dbWriteLock.lock();
        dbLock.writeLock().lock(); // Dùng writeLock
        //SQLiteDatabase db = this.getWritableDatabase();
        SQLiteDatabase db = getDb();
        db.beginTransaction(); // Bước 1: (Ngoài try) Nếu Bước 1 thành công, chắc chắn Transaction đang mở.
        try {
            int rowsAffected = db.update(tableName, cv, MA_THUOC + " = ?", new String[]{thuoc.ma_thuoc});
            if (rowsAffected == 0) {
                resultId = db.insertOrThrow(tableName, null, cv);
                Log.d(TAG, "Inserted Thuoc: " + thuoc.ma_thuoc + " into " + tableName + " from url_id: " + thuoc.id_url);
            } else {
                Log.d(TAG, "Updated Thuoc: " + thuoc.ma_thuoc + " in " + tableName + " from url_id: " + thuoc.id_url);
                Cursor cursor = db.query(tableName, new String[]{ID}, MA_THUOC + " = ?", new String[]{thuoc.ma_thuoc}, null, null, null);
                if (cursor.moveToFirst()) {
                    resultId = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
                }
                cursor.close();
            }
            // Bước 2: Xử lý dữ liệu
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "SQLiteConstraintException when inserting/updating Thuoc: " + thuoc.ma_thuoc + " in " + tableName, e);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting/updating Thuoc: " + thuoc.ma_thuoc + " in " + tableName, e);
        } finally {
            db.endTransaction(); // Bước 3
            //dbWriteLock.unlock();
            dbLock.writeLock().unlock(); // Giải phóng writeLock
        }
        return resultId;
    }

    /**
     * @param initUrlsTable
     * @param url
     * @return
     */
    private boolean isUrlExists(String initUrlsTable, String url) { // Cần không vì initUrlsTable có url là khóa chính
        // 1. Lấy DB trước - Nếu lỗi ở đây, nó sẽ crash hoặc thoát sớm
        // mà chưa kịp chiếm giữ khóa của người khác.
        SQLiteDatabase db = getDb();

        // 2. Bắt đầu chiếm giữ tài nguyên chung. Dùng readLock để nhiều luồng Crawler có thể kiểm tra URL cùng lúc(Nó không cho ghi)
        dbLock.readLock().lock();

        try {
            // 3. Thực hiện công việc cực nhanh: Sử dụng try-with-resources để tự động đóng Cursor
            //String query = "SELECT 1 FROM " + initUrlsTable + " WHERE url = ? LIMIT 1";
            //String query = "SELECT 1 FROM " + initUrlsTable + " WHERE " + DBHelperThuoc.URL + " = ? LIMIT 1";
            try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + initUrlsTable + " WHERE " + DBHelperThuoc.URL + " = ?", new String[]{url})) {
                return cursor.moveToFirst();
            }
        } catch (Exception e) {
            Log.e("DB_CHECK", "Lỗi kiểm tra URL tồn tại: " + e.getMessage());
            return false; // Nếu lỗi, coi như rỗng để an toàn hoặc xử lý tùy logic
        } finally {
            // 4. QUAN TRỌNG: Dù lệnh SELECT lỗi hay Cursor hỏng, Lock vẫn phải mở ngay lập tức cho luồng khác vào
            dbLock.readLock().unlock();
        }
    }

    /**
     * Chèn url con (phân trang) tìm được vào Bảng Queue dùng để Crawl (initUrlsTable)
     * @param initUrlsTable
     * @param url
     * @param idUrl
     * @param level
     */
    public void saveChildUrlToUrlQueueTable(String initUrlsTable,
                                            String url, int idUrl, int level) {

        // level đã + 1 khi gọi hàm này. PP mới level là số trang page của trang con
        if (isUrlExists(initUrlsTable, url)) return;  // Cần không vì initUrlsTable có url là khóa chính

        //Lưu url con vào bảng initUrlsTable
        ContentValues values = new ContentValues();
        values.put(DBHelperThuoc.ID_URL, idUrl);
        values.put(DBHelperThuoc.LEVEL, level); //leve > 0 là url con
        values.put(DBHelperThuoc.URL, url); // là khóa chính
        values.put(DBHelperThuoc.STATUS, 0);    //Mặc định là 0 (chưa xử lý)
        //values.put(DBHelperThuoc.IS_CHILD, level > 1?1:0); // Đánh dấu là url con (ban đầu là 1 (true))
        //Không ném lỗi nếu trùng url
        //dbThuoc.insertWithOnConflict( initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrls

        // Hoặc: bắt lỗi nếu trùng url
        //dbWriteLock.lock();
        dbLock.writeLock().lock(); // Dùng readLock

        /**
         * Lưu ý: beginTransactionNonExclusive() cho phép các luồng khác tiếp tục Đọc DB trong khi Transaction này đang chạy, giúp Crawler không bị khựng lại khi cần kiểm tra các URL khác.
         */
        try {   // của writeLock
            SQLiteDatabase db = getDb(); // Lấy thể hiện DB trong khối khóa
            // Chỉ thực hiện 1 record nên CÓ THỂ KHÔNG CẦN beginTransaction
            //db.beginTransaction(); // Bước 1: (Ngoài try) Nếu Bước 1 thành công, chắc chắn Transaction đang mở.
            db.beginTransactionNonExclusive(); // WAL cực kỳ hợp với NonExclusive
            try {   // của beginTransactionNonExclusive
                db.insertWithOnConflict(initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);

                db.setTransactionSuccessful();
            } catch (SQLiteConstraintException e) {
                Log.w("CrawlWorker", "URL already exists: " + url);
            } finally {
                //if (db != null) { // Chỉ kiểm tra db có null không. KHÔNG KIỂM TRA db.inTransaction(), NẾU KHÔNG SẼ GÂY LỖI
                // Gọi endTransaction() mà không cần kiểm tra inTransaction()
                // Vì thiếu lệnh này mà insertMultipleThuoc bị lỗi đã setTransactionSuccessful mà không endTransaction
                //if (db != null && db.inTransaction()) { // Kiểm tra inTransaction an toàn trước khi kết thúc
                //Nếu Bước 1 thất bại (ném ngoại lệ), code sẽ không bao giờ lọt vào khối finally này
                // để chạy lệnh endTransaction().
                db.endTransaction();

                //}
            }
        } finally {
            //dbWriteLock.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock
        }

    }

    public void insertParent(SQLiteDatabase db, String tableParent, String id_url,String level,
                             String url, String createdAt, String ghiChu, int indexColor, String errorMessage) {
        ContentValues values = new ContentValues();
        values.put(ID_URL, id_url);
        values.put(LEVEL, level);
        values.put(URL, url);
        values.put(CREATED_AT, createdAt);
        values.put(GHI_CHU, ghiChu);
        values.put(INDEX_COLOR, indexColor);
        //INSERT INTO " + tableThuoc2KThay3KT
        values.put(ERR_MESSAGE, ghiChu);  //Tạm thời ERR_MESSAGE nằm trong ghi_chu
        // Chỉ insert 1 record
        dbLock.writeLock().lock();
        try {
            db.insertWithOnConflict(tableParent, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            dbLock.writeLock().unlock();
        }
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
        dbLock.writeLock().lock();
        SQLiteDatabase db = getDb();
        SQLiteStatement stmt = null;
        try {
            // Insert vào Bảng con
            String query = "INSERT INTO " + tableThuoc2KThay3KT + " (" +
                    ID_URL + ", " +
                    ID_URL + ", " +
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
                    ID_URL + " = excluded." + ID_URL + ", " +
                    ID_URL + " = excluded." + ID_URL + ", " +
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
                stmt.bindLong(colIndex++, t.id_url);      //parent=parent_id_of_crawled_url
                //stmt.bindLong(colIndex++, t.id_url);      //parent=parent_id_of_crawled_url
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
            dbLock.writeLock().unlock();
        }

    }

    /**
     * Gemini: Dù bạn đặt tên là Notransaction, nhưng mình khuyên chân thành nên dùng Transaction
     * vì Crawler cần tốc độ. Nếu bạn sợ lỗi Thread, hãy dùng db.beginTransactionNonExclusive().
     *
     * Chèn nhiều bản ghi ThuocPrefs vào bảng, xử lý xung đột (ON CONFLICT DO UPDATE).
     *
     * @param tableThuoc2KThay3KT Tên bảng đích (ví dụ: TABLE_THUOC_TBDT_SEARCH)
     * @param listThuoc           Danh sách các đối tượng ThuocPrefs cần chèn/cập nhật.
     */
    public void insertMultipleThuoc(String tableThuoc2KThay3KT, List<ThuocSQLite> listThuoc) {
        //BỊ LỖI: Lỗi khi insertMultipleThuoc vào TABLE_THUOC_2KT: Cannot perform this operation because the transaction has already been marked successful.  The only thing you can do now is call endTransaction().
        Log.d(TAG, "insertMultipleThuoc: BẮT ĐẦU insertMultipleThuoc cho " + tableThuoc2KThay3KT);
        //dbWriteLock.lock();
        dbLock.writeLock().lock(); // Dùng writeLock
        try {   //Của writeLock
            //SQLiteStatement stmt = null; // Khai báo stmt ở đây
            String query = "INSERT INTO " + tableThuoc2KThay3KT + " (" +
                    ID_URL + ", " +

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
                    SDK + ", " +

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
                    "ON CONFLICT(" + ID_URL + ", " + MA_THUOC + ") " +
                    "DO UPDATE SET " +

                    ID_URL + " = excluded." + ID_URL + ", " +
                    ID_URL + " = excluded." + ID_URL + ", " +
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

            SQLiteStatement stmt = db.compileStatement(query); // Gán giá trị cho stmt ở đây
            SQLiteDatabase db = getDb(); // Khai báo db ở đây
            //db.beginTransaction(); //Bước 1: Tăng tốc độ lên hàng chục lần(Bắt đầu giao dịch)
            db.beginTransactionNonExclusive(); //Bước 1: WAL cực kỳ hợp với NonExclusive
            try {   //Của beginTransaction
                for (ThuocSQLite t : listThuoc) {
                    stmt.clearBindings();   // Xóa bind cũ
                    int colIndex = 1;
                    stmt.bindLong(colIndex++, t.id_url);
                    //stmt.bindLong(colIndex++, t.id_url);      //parent=parent_id_of_crawled_url
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

                    stmt.bindLong(colIndex++, System.currentTimeMillis() / 1000);        //currentTimestamp: là String time

                    stmt.executeInsert();
                }
                // Bước 2
                db.setTransactionSuccessful(); // Đánh dấu transaction thành công
                Log.d(TAG, "insertMultipleThuoc: Hoàn thành insert/update " + listThuoc.size() + " bản ghi vào " + tableThuoc2KThay3KT);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi Batch insertMultipleThuoc vào " + tableThuoc2KThay3KT + ": " + e.getMessage(), e);
            } finally {
                // Đảm bảo kết thúc giao dịch
                // Luôn đảm bảo endTransaction() được gọi, bất kể thành công hay thất bại
                // KHÔNG GỌI db.close() Ở ĐÂY!

                // Gọi endTransaction() mà không cần kiểm tra inTransaction()
                db.endTransaction();    // Chỉ ghi xuống đĩa 1 lần duy nhất tại đây

                if (stmt != null) {
                    stmt.close(); // Đóng SQLiteStatement
                }
            }
        } finally {
            //dbWriteLock.unlock();   // Giải phóng khóa ghi
            dbLock.writeLock().unlock(); // Giải phóng writeLock
            // QUAN TRỌNG: KHÔNG ĐÓNG DB Ở ĐÂY!
            // SQLiteOpenHelper sẽ tự động quản lý việc đóng/mở và giữ kết nối hiệu quả.
        }
    }

    /**
     * Lấy số lượng bản ghi trong một bảng cụ thể.
     * @param tableName Tên của bảng cần đếm số bản ghi.
     * @return Số lượng bản ghi trong bảng, hoặc 0 nếu bảng không tồn tại hoặc có lỗi.
     */
    public long getCountRecord(String tableName) {   //getThuocCount: đăt tên chung, rồi lấy tổng record của tên bảng
        dbLock.readLock().lock();
        try {
            // Dùng DatabaseUtils để code cực ngắn và tránh lỗi Cursor
            return DatabaseUtils.queryNumEntries(getDb(), tableName);
        } catch (Exception e) {
            Log.e(TAG, "Table " + tableName + " error: " + e.getMessage());
            return 0;
        } finally {
            dbLock.readLock().unlock();
        }
    }

    /**
     * Phương thức để xóa tất cả bản ghi trong một bảng cụ thể.
     * @param tableName Tên bảng cần xóa dữ liệu.
     * @return Số hàng đã xóa.
     */
    public long deleteAllRecords(String tableName) {
        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        dbLock.writeLock().lock();
        try {
            db.beginTransactionNonExclusive();  // Cho phép luồn khác đọc, không cho ghi: Nhanh hơn
            try {
                //1. Xóa toàn bộ bản ghi: Nm trong Transaction nếu muốn RollBack, phục hồi nếu cúp điện
                long rows = db.delete(tableName, null, null);

                //2. Reset ID tự tăng (sqlite_sequence) một cách an toàn
                // Reset ID trước hoặc sau đều được, miễn là nằm TRONG transaction
                db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", new String[]{tableName});

                db.setTransactionSuccessful();
                return rows; // Java sẽ giữ giá trị này, chạy finally xong mới thực sự return
            } catch (Exception e) {
                Log.e(TAG, "Lỗi xóa bảng: " + tableName, e);
                return 0;
            } finally {
                db.endTransaction();
            }
        } finally {
            // Dù trời sập, khóa vẫn phải mở
            dbLock.writeLock().unlock();
        }
    }

    public String getDateFromTable(String tableName, String typeMIN_MAX) {
        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        // Nên để giá trị mặc định rõ ràng
        String date = "N/A";
        // typeMIN_MAX thường là "MIN" hoặc "MAX"
        // Sử dụng hàm datetime() (tính theo giây) của SQLite để chuyển đổi timestamp thành chuỗi ngày tháng dễ đọc
        String query = "SELECT datetime(" + typeMIN_MAX + "(" + CREATED_AT + "), 'unixepoch', 'localtime') FROM " + tableName;
        // Hoặc
//        String query = "SELECT date(" + typeMIN_MAX + "(" + CREATED_AT + "), 'unixepoch', 'localtime') FROM " + tableName;

        dbLock.readLock().lock();
        try (Cursor cursor = db.rawQuery(query, null)){
            if (cursor != null && cursor.moveToFirst()) {
                String result = cursor.getString(0);
                if (result != null) {
                    date = result;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi lấy ngày (" + typeMIN_MAX + ") từ bảng " + tableName, e);
        } finally {
            dbLock.readLock().unlock();
        }
        return date;
    }

    //region Các phương thức mới cho InitUrlsTable
    public boolean addInitUrl(String initUrlsTable, String url) {
        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        // Trưc khóa cho nhanh
        long id = -1;
        ContentValues values = new ContentValues();
        values.put(URL, url);
        //lock3InitUrlsTable.lock();
        dbLock.writeLock().lock(); // Dùng writeLock
        try {
            id = db.insertWithOnConflict(initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            // Log.d("DBHelperThuoc", "Đã thêm Init URL: " + url + " vào " + initUrlsTable);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi thêm Init URL vào " + initUrlsTable + ": " + e.getMessage());
        } finally {
            //lock3InitUrlsTable.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock
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
        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();    // Nên truyền vào để thống nhất, tăng tốc độ
        int idUrl = 0;
        // Ghi URL vào bảng initUrlsTable
        //dbWriteLock.lock();
        dbLock.writeLock().lock(); // Dùng readLock
        try {
            assert listKyTu != null;
            db.beginTransactionNonExclusive(); //Bước 1: WAL cực kỳ hợp với NonExclusive
            try {
                for (String kytu : listKyTu) {
                    if (kytu.trim().isEmpty())
                        continue; //Chỉ kiểm tra url rỗng (hoặc chỉ chứa các khoảng trắng)
                    // Vẫn lấy kytu có khoảng trắng ở đầu và cuối
                    String[] row = new String[3];
                    row[0] = String.valueOf(idUrl);  //parentId
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
                    values.put(DBHelperThuoc.ID_URL, idUrl);
                    values.put(DBHelperThuoc.LEVEL, 1); //Cũ: Của url cha level = 0 (Mặc định); Mới: level (thành page) = 1 (Mặc định)
                    values.put("url", (AppConstants.URL0 + kytu + AppConstants.URL1).trim());
                    values.put("status", 0); // 0: chưa xử lý, có thể thêm cột 'completed' nếu cần. Mặc định completed = 0 (uncompleted)
                    //values.put(DBHelperThuoc.BOOLEAN_URL_CHILD, 0); // Mặc định ban đầu là 0 (false) : không phai là url Child (con)
                    //Cũ: có ném lô
                    //dbThuoc.insert( initUrlsTable, null, values); //initUrlsTable. tableNameThuoc_Nao phải có
                    //Mới: không ném lỗi
                    db.insertWithOnConflict(initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
                    idUrl++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            //dbWriteLock.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock
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

        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        // Chưa xử lý có STATUS = 0
        //dbWriteLock.lock();
        dbLock.readLock().lock(); // Dùng readLock: vì chỉ đọc, không ghi
        try (Cursor cursor = db.query(initUrlsTable, new String[]{URL, ID_URL, LEVEL}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null, null);){
            if (cursor.moveToFirst()) {
                do {
                    String[] row = new String[3];
                    row[0] = cursor.getString(0);   //url
                    row[1] = cursor.getString(1);   // ID_URL
                    row[2] = cursor.getString(2);   //LEVEL: Cũ: level = 0; Mới: level (thành page) = 1
                    listRowUrls.add(row);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } finally {
            //dbWriteLock.unlock();
            dbLock.readLock().unlock(); // Giải phóng readLock
        }

        return listRowUrls;
    }
    public List<String> getAllUrlQueue(String initUrlsTable) {  // Chỉ lấy các url chưa xử lý (STATUS = 0)
        // Chỉ lấy các url chưa xử lý (STATUS = 0)
        List<String> urls = new ArrayList<>();

        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();

        //lock3InitUrlsTable.lock();
        dbLock.readLock().lock(); // Dùng readLock: vì chỉ đọc, không ghi
        try (Cursor cursor = db.query(initUrlsTable, new String[]{URL}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null);){
            if (cursor.moveToFirst()) {
                do {
                    urls.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi lấy tất cả Init URL từ " + initUrlsTable + ": " + e.getMessage());
        } finally {
            //lock3InitUrlsTable.unlock();
            dbLock.readLock().unlock(); // Giải phóng readLock
        }
        return urls;
    }

    public boolean deleteInitUrl(String initUrlsTable, String url) {
        // Chỉ Delete 1 url cụ thể
        // Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        int rowsAffected = 0;
        //lock3InitUrlsTable.lock();
        dbLock.writeLock().lock(); // Dùng writeLock, vì có delete
        try {
            //
            rowsAffected = db.delete(initUrlsTable, URL + " = ?", new String[]{url});
            // Log.d("DBHelperThuoc", "Đã xóa Init URL: " + url + " khỏi " + initUrlsTable + ". Số hàng ảnh hưởng: " + rowsAffected);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi xóa Init URL khỏi " + initUrlsTable + ": " + e.getMessage());
        } finally {
            //lock3InitUrlsTable.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock

        }
        return rowsAffected > 0;
    }

    /** Kiểm tra table rỗng
     *
     * @param initUrlsTable
     * @return
     */

    public boolean isInitUrlsTableEmpty(String initUrlsTable) {
        //boolean isEmpty = true;
        // 1. Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();

        //lock3InitUrlsTable.lock();
        dbLock.readLock().lock(); // / 2. Bắt đầu chiếm giữ tài nguyên chung. Dùng readLock
        try {
            // 3. Thực hiện công việc cực nhanh
            // SELECT 1 giúp tối ưu bộ nhớ, LIMIT 1 giúp dừng quét ngay khi thấy 1 dòng
            String query = "SELECT 1 FROM " + initUrlsTable + " WHERE " + DBHelperThuoc.URL + " = ? LIMIT 1";
            try (Cursor cursor = db.rawQuery(query, null)) {
                //1.
//                if (cursor != null && cursor.moveToFirst()) {
//                    // Nếu tìm thấy ít nhất 1 dòng, bảng KHÔNG rỗng
//                    isEmpty = false;
//                }
                // 2. Hoặc ngắn gọn
                //if (cursor != null){
                    return !cursor.moveToFirst();
                //}

            }
        } catch (Exception e) {
            Log.e("DB", "Lỗi kiểm tra bảng rỗng: " + e.getMessage());
            return true;
        } finally {
            // 4. Giải phóng ngay lập tức cho luồng khác vào
            dbLock.readLock().unlock();
        }
        //return isEmpty;
    }

    public boolean addCompletedUrlChild(String completedUrlsChildTable, String url) {
        // 1. Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();
        long id = -1;
        ContentValues cv = new ContentValues();
        cv.put(URL, url);
        //dbWriteLock.lock();
        //lock2CompletedUrlsTable.lock();
        dbLock.writeLock().lock(); // Dùng writeLock vì có ghi
        try {   // Chỉ insert 1 url, nên không cần transaction
            id = db.insertWithOnConflict(completedUrlsChildTable, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e("DBHelperThuoc", "Lỗi khi thêm URL đã hoàn thành vào " + completedUrlsChildTable + ": " + e.getMessage());
        } finally {
            //dbWriteLock.unlock();
            //lock2CompletedUrlsTable.unlock();
            dbLock.writeLock().unlock(); // Giải phóng readLock
        }
        return id != -1;
    }
    public long addThuoc(String tableName, ThuocSQLite thuoc) {
        // Chỉ thêm 1 dối tượng thuốc
        // 1. Lấy DB trước khi khóa để nếu DB hỏng thì crash luôn, chưa kịp khóa
        SQLiteDatabase db = getDb();

        long result = -1;
        ContentValues cv = new ContentValues();
        //values.put(ID_URL, thuoc.id_url);   //PARENT=PARENT_ID_OF_CRAWLED_URL
        //values.put(LEVEL, thuoc.level);
        //values.put(KY_TU_SEARCH, thuoc.ky_tu_search);
        cv.put(MA_THUOC, thuoc.ma_thuoc);
        cv.put(TEN_THUOC, thuoc.ten_thuoc);
        cv.put(THANH_PHAN, thuoc.thanh_phan);
        cv.put(NHOM_THUOC, thuoc.nhom_thuoc);
        cv.put(DANG_THUOC, thuoc.dang_thuoc);
        cv.put(SAN_XUAT, thuoc.san_xuat);
        cv.put(DANG_KY, thuoc.dang_ky);
        cv.put(PHAN_PHOI, thuoc.phan_phoi);
        cv.put(SDK, thuoc.sdk);
        cv.put(CAC_THUOC, thuoc.cac_thuoc);
        //values.put(LINK_KY_TU_SEARCH, thuoc.link_ky_tu_search);
        cv.put(MA_THUOC_LINK, thuoc.ma_thuoc_link);
        cv.put(THANH_PHAN_LINK, thuoc.thanh_phan_link);
        cv.put(NHOM_THUOC_LINK, thuoc.nhom_thuoc_link);
        cv.put(DANG_THUOC_LINK, thuoc.dang_thuoc_link);
        cv.put(SAN_XUAT_LINK, thuoc.san_xuat_link);
        cv.put(DANG_KY_LINK, thuoc.dang_ky_link);
        cv.put(PHAN_PHOI_LINK, thuoc.phan_phoi_link);
        cv.put(SDK_LINK, thuoc.sdk_link);
        cv.put(CAC_THUOC_LINK, thuoc.cac_thuoc_link);
        //values.put(GHI_CHU, thuoc.ghi_chu);
        //values.put(INDEX_COLOR, thuoc.index_color);
        cv.put(CREATED_AT, System.currentTimeMillis()/1000); // tính giây. Tự động thêm timestamp

        //dbWriteLock.lock(); // Khóa ghi
        dbLock.writeLock().lock(); // Dùng writeLock, vì ghi 1 thuoc

        try {
            result = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_IGNORE); // SỬA: Conflict policy

        } finally {
            //dbWriteLock.unlock(); // Mở khóa ghi
            dbLock.writeLock().unlock(); // Giải phóng writeLock
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
        // Kiểm tra bảng tồn tại: Trong thực tế, nếu bạn là người quản lý Database, bạn biết bảng đó chắc chắn tồn tại.
        // Việc query vào sqlite_master mỗi lần xóa là không cần thiết và làm chậm tốc độ.
        // Bạn có thể bỏ qua bước này để tối ưu.
        SQLiteDatabase db = getDb();
        int deletedRows = 0;

        dbLock.writeLock().lock();
        try {
            // Bắt đầu Transaction để tối ưu tốc độ xóa
            db.beginTransactionNonExclusive();
            try {
                // Câu lệnh xóa trùng lặp giữ lại bản ghi đầu tiên (ID nhỏ nhất)
                String query = "DELETE FROM " + tableThuoc +
                        " WHERE " + ID + " NOT IN (" +
                        "    SELECT MIN(" + ID + ")" +
                        "    FROM " + tableThuoc +
                        "    GROUP BY " + MA_THUOC +
                        ");";

                db.execSQL(query);

                // CÁCH TỐI ƯU: Lấy số hàng vừa bị xóa mà không cần đếm lại bảng
                try (Cursor c = db.rawQuery("SELECT changes()", null)) {
                    if (c.moveToFirst()) {
                        deletedRows = c.getInt(0);
                    }
                }

                db.setTransactionSuccessful();
                Log.d(TAG, "Deleted " + deletedRows + " duplicate records from " + tableThuoc);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting duplicates: " + e.getMessage());
            deletedRows = 0;
        } finally {
            dbLock.writeLock().unlock();
        }
        return deletedRows;
    }

    // Đảm bảo CREATE_TABLE_URL_QUEUE có các cột này:
// public static final String CREATE_TABLE_URL_QUEUE = "CREATE TABLE " + TABLE_URL_QUEUE + " (" +
//         ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
//         URL + " TEXT UNIQUE," + // URL phải là UNIQUE
//         STATUS + " INTEGER," +
//         LEVEL + " INTEGER," + // Thêm cột LEVEL
//         ID_URL + " INTEGER," + // Thêm cột ID_URL
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
        // 1. Lấy DB trước khi khóa (Dùng Readable cho thao tác SELECT)
        SQLiteDatabase db = this.getReadableDatabase();

//        Cursor cursor = null;
        //dbWriteLock.lock();
        dbLock.readLock().lock(); // Dùng readLock
        try {
            String[] columns = {URL, LEVEL, ID_URL, STATUS}; // Các cột cần lấy
            try (Cursor cursor = db.query(table_Url_Queue, columns,
                    STATUS + " = ?", new String[]{String.valueOf(status)},
                    null, null, null)) {

                Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.isBeforeFirst()=" + cursor.isBeforeFirst()); // <-- false
                Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.isAfterLast()=" + cursor.isAfterLast());     // <-- true
                Log.d(TAG, "getDetailedUrlInfoByStatus: cursor.getCount()=" + cursor.getCount());           // <-- 1
                Log.d(TAG, "SQL Query: SELECT " + TextUtils.join(",", columns) + " FROM " + table_Url_Queue + " WHERE " + STATUS + " = " + status);

                if (cursor != null && cursor.moveToFirst()) {
                    // 2. Lấy Index của cột MỘT LẦN DUY NHẤT trước vòng lặp
                    int urlIdx = cursor.getColumnIndexOrThrow(URL);
                    int levelIdx = cursor.getColumnIndexOrThrow(LEVEL);
                    int parentIdIdx = cursor.getColumnIndexOrThrow(ID_URL);
                    long currentTime = System.currentTimeMillis() / 1000;

                    do {
                        String url = cursor.getString(urlIdx);
                        int level = cursor.getInt(levelIdx);
                        int idUrl = cursor.getInt(parentIdIdx);

                        // Logic xử lý chuỗi của bạn
                        if (url.contains("+")) {    //Xem
                            int iStop = 0;
                            Log.d("CrawlType", "getLisUrLFromCrawlType: DAU CONG=" + url);
                        }

                        //thuocSQLite.ky_tu_search = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
                        //thuocSQLite.ma_thuoc = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn

                        //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
                        String maThuocP = "";
                        if (url.contains("key")) {
                            maThuocP = "https://www..." + "key" + url.split("key")[1];    // Tạo Mã Thuốc giả chu Url cha
                        }
                        urlInfos.add(new UrlInfo(url, idUrl, level, maThuocP, status,
                                null, 0, -1, System.currentTimeMillis() / 1000));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting detailed URL info: " + e.getMessage(), e);
        } finally {
            // Không đóng DB ở đây
            //dbWriteLock.unlock();
            dbLock.readLock().unlock(); // Giải phóng readLock
        }
        return urlInfos;
    }

    public void removeUrlFromInitUrlsTable(CrawlType crawlType, String url) {
        // 1. Lấy DB bên ngoài Lock để đảm bảo luồng chỉ giữ Lock khi thực sự sẵn sàng ghi
        SQLiteDatabase db = this.getWritableDatabase();
        // tableName = initUrlsTable
        String initUrlsTable = crawlType.getUrlQueueTableName(); //initUrlsTable = crawlType.getUrlQueueTableName()

        // 2. Kiểm tra an toàn trước khi lock
        if (initUrlsTable == null || url == null) return;

//        dbWriteLock.lock();
        dbLock.writeLock().lock(); // Dùng writeLock

        try {
            // 3. Thực hiện xóa.
            // Dùng db.delete() là chuẩn vì nó trả về số dòng xóa (có thể log nếu cần)
            int rows = db.delete(initUrlsTable, "url = ?", new String[]{url});

            // Log nhẹ để debug khi cần
            // Log.d(TAG, "Removed " + rows + " URL: " + url);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi xóa URL khỏi bảng " + initUrlsTable + ": " + e.getMessage());
        } finally {
            // 4. Luôn nhả khóa để luồng Crawler khác tiếp tục công việc
            dbLock.writeLock().unlock();
        }
    }

    public boolean isUrlExists(CrawlType crawlType, String url) {
        // 1. Khởi tạo giá trị mặc định
        boolean exists = false;

        // 2. Lấy DB (Nên dùng phương thức getDb() của bạn nếu nó đã bao hàm getWritable/Readable)
        SQLiteDatabase db = getReadableDatabase();    //getDb() Hoặc getReadableDatabase();: dùng getReadableDatabase() Trong SQLite (đặc biệt là chế độ WAL),
                                        // Readable cho phép đọc dữ liệu ngay cả khi có một luồng khác đang Writable (ghi)
                                        // vào bảng khác. Điều này giúp Crawler của bạn kiểm tra URL cũ trong khi vẫn đang lưu URL mới mà không bị khựng lại.

        // 3. Khóa để đọc
        dbLock.readLock().lock();

        // 4. Sử dụng Try-with-resources để tự động đóng Cursor
        try (Cursor cursor = db.query(
                crawlType.getTableThuocName(),  //Dùng "new String[]{"1"}" thay vì "new String[]{"url"}":
                // Thay vì lấy cả cột url (chuỗi dài), ta chỉ lấy số 1. Điều này giúp tiết kiệm dung lượng RAM khi Cursor nhận dữ liệu.
                new String[]{"1"}, // Chỉ cần lấy giá trị 1 để tối ưu bộ nhớ
                "url = ?",
                new String[]{url},
                null, null, null,
                "1" // LIMIT 1: Dừng ngay khi tìm thấy 1 dòng
        )) {
            if (cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e("DB_CHECK", "Lỗi kiểm tra URL: " + url, e);
        } finally {
            // 5. Luôn giải phóng khóa
            dbLock.readLock().unlock();
        }

        return exists;
    }

    private void deleteOldDataThuoc(CrawlType crawlType, String url) {
        // 1. Lấy DB trước khi khóa (để đảm bảo db sẵn sàng)
        SQLiteDatabase db = getDb(); // PHẢI LÀ getWritableDatabase(). Hàm getDb() (Singleton) xác đnh là getWritableDatabase()

        // 2. Chiếm quyền ghi (WriteLock)
        dbLock.writeLock().lock(); // Dùng writeLock

        try {
            // 3. Thực hiện xóa
            // Hàm delete trả về số dòng đã xóa, bạn có thể log nếu cần
            int rows = db.delete(crawlType.getTableThuocName(), "url = ?", new String[]{url});

            if (rows > 0) {
                Log.d("DB_DELETE", "Đã xóa " + rows + " bản ghi cũ của URL: " + url);
            }
        } catch (Exception e) {
            Log.e("DB_DELETE", "Lỗi khi xóa dữ liệu cũ cho URL: " + url, e);
        } finally {
            // 4. QUAN TRỌNG:
            dbLock.writeLock().unlock();
        }
    }

    /** Dùng cho tác vụ đơn lẻ
     * Overloading (Nạp chồng hàm):
     * @param tableUrlParent
     * @param url
     * @return
     */
    public long getUrl_id(String tableUrlParent, String url) {
        dbLock.readLock().lock();
        try {
            //DÙNG: getDb() CHO ÍT LẦN
            return getUrl_id(getDb(), tableUrlParent, url); //Hàm gọi cũ: getUrl_idInternal
        } finally {
            dbLock.readLock().unlock();
        }
    }

    /** Hàm nội bộ dùng khi bạn đã có sẵn DB và đã Lock ở ngoài
     * Overloading (Nạp chồng hàm):
     * @param db
     * @param tableUrlParent
     * @param url
     * @return
     */
    private long getUrl_id(SQLiteDatabase db, String tableUrlParent, String url) {  //Tên hàm cũ: getUrl_idInternal
        //Nên truyền db từ ngoài vào (Tối ưu cho Crawler) khi gọi hàm này trong một vòng lặp (vòng lặp 1000 URL),
        //Trong Crawler, bạn thường thực hiện một chuỗi thao tác: Kiểm tra ID -> Nếu chưa có thì Insert -> Lấy ID vừa Insert.
        //Hàm nội bộ dùng khi bạn đã có sẵn DB và đã Lock ở ngoài
        try (Cursor cursor = db.query(tableUrlParent, new String[]{ID}, URL + "=?", new String[]{url}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0); // Dùng 0 cho nhanh nếu chỉ lấy 1 cột
            }
        }
        return -1;
    }

    // Xem lại trùng chức năng
    public boolean isMaThuocExists(SQLiteDatabase db, String tableThuocCon, String maThuoc, String url) { // Cần không vì initUrlsTable có url là khóa chính
        // CHÚ Ý HM NÀY TÌM MÃ THUỐC MÀ CÓ URL "KHÁC" url ĐANG XÉT THÌ MỚI LÀ TRUE
        /**
         * 1. Giải đáp: "Cần không vì url là khóa chính?"
         * Dù url ở bảng cha là duy nhất, bạn VẪN CẦN hàm này. Lý do:
         *
         * Một mã thuốc (ví dụ: "PANADOL001") có thể xuất hiện ở nhiều URL khác nhau trên website.
         *
         * Nếu bạn muốn tránh việc lưu trùng dữ liệu thuốc khi cùng một loại thuốc nằm ở 2 đường dẫn khác nhau, thì việc kiểm tra maThuoc kết hợp loại trừ url_id hiện tại là hoàn toàn chính xác.
         *
         * 2. Các điểm cần tối ưu trong code
         * Lấy SQLiteDatabase: Bạn đang gọi this.getReadableDatabase() bên trong lock. Điều này ổn, nhưng hãy đảm bảo biến db được dùng xuyên suốt các hàm con (như getUrl_id) để tránh việc mở kết nối nhiều lần.
         *
         * Vấn đề đóng Cursor: Bạn đang đóng cursor bằng cursor.close() thủ công. Nếu dòng cursor.getCount() xảy ra lỗi, cursor.close() sẽ bị bỏ qua gây rò rỉ bộ nhớ (Memory Leak). Nên dùng try-with-resources.
         *
         * Hiệu năng getCount(): Như đã nói ở các câu trước, dùng moveToFirst() với LIMIT 1 sẽ nhanh hơn việc đếm toàn bộ.
         */
        // 1. Xác định bảng cha dựa trên tên bảng con
        String tableUrlCha;
        if (tableThuocCon.endsWith("2kt")) {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_2KT;
        } else if (tableThuocCon.endsWith("3kt")) {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_3KT;
        } else {
            tableUrlCha = DBHelperThuoc.TABLE_PARENT_URLS_CUSTOM;
        }

        //Đảm bảo biến db được dùng xuyên suốt các hàm con (như getUrl_id) để tránh việc mở kết nối nhiều lần.
        //SQLiteDatabase db = this.getReadableDatabase(); // LẤY DB KHI CẦN

        //NẾU DÙNG VÒNG LẠP GỌI, NÊN KHÓA BÊN NGOÀI KHI GỌI NÓ: TRÁNH LOCK VÀ UNLOCK NHIỀU LẦN
        //dbLock.readLock().lock(); // NÊN LOCK KHI GỌI NÓ,
        try {

            // 3. Lấy url_id (Nên truyền db vào để dùng chung connection)
            long url_id = this.getUrl_id(db, tableUrlCha, url);
            if (url_id == -1) return false;

            // 4. Truy vấn kiểm tra tồn tại (loại trừ url_id hiện tại)
            // Dùng LIMIT 1 để tối ưu tốc độ
            String selection = MA_THUOC + " = ? AND " + ID_URL + " != ?";
            String[] selectionArgs = {maThuoc, String.valueOf(url_id)};

            try (Cursor cursor = db.query(
                    tableThuocCon,
                    new String[]{"1"}, // Chỉ lấy hằng số 1 cho nhẹ
                    selection,
                    selectionArgs,
                    null, null, null, "1")) {

                return cursor.moveToFirst();
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Lỗi check MaThuoc: " + maThuoc, e);
            return false;
        } finally {
            // 5. Luôn giải phóng lock
            dbLock.readLock().unlock();
        }
    }

    // Thêm
    // CẬP NHẬT PHƯƠNG THỨC loadQueueUrls (nếu có) tương tự như loadCompletedUrls
    private void loadQueueUrls(String tableName, ConcurrentLinkedQueue<String> urlQueue) {
        // Hàm này Pass-by-reference (Truyền tham chiếu): truyền một đối tượng như ConcurrentLinkedQueue vào một phương thức,
        // phương thức đó sẽ thao tác trực tiếp trên chính đối tượng mà bạn đã truyền vào.

        // Tương tự: loadCompletedUrls
        // ...
        // Thay vì: SQLiteDatabase db = dbHelperThuoc.getWritableDatabase();
        // Cứ gọi dbHelperThuoc.getReadableDatabase() khi cần.
        // Cursor cursor = db.rawQuery(...);
        // Thay thành:
        // Cursor cursor = dbHelperThuoc.getReadableDatabase().rawQuery(...);
        // ...

        // 1. Dùng readLock vì đây là thao tác đọc
        dbLock.readLock().lock();

        // 2. Lấy đối tượng DB dùng chung
        SQLiteDatabase db = getDb();

        // 3. Sử dụng try-with-resources để Cursor tự động đóng
        // Chỉ lấy các URL có status = 0 (đang chờ cào)
        String query = "SELECT " + URL + " FROM " + tableName + " WHERE " + STATUS + " = 0";

        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                int urlIndex = cursor.getColumnIndex(URL);

                // Xóa queue cũ nếu cần hoặc kiểm tra trước khi add
                // urlQueue.clear();

                do {
                    String url = cursor.getString(urlIndex);
                    if (url != null && !url.isEmpty()) {
                        urlQueue.add(url);
                    }
                } while (cursor.moveToNext());

                Log.d(TAG, "Đã load " + urlQueue.size() + " URL vào Queue từ bảng " + tableName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi load Queue URLs từ " + tableName + ": " + e.getMessage());
        } finally {
            // 4. Giải phóng khóa để các luồng khác có thể làm việc
            dbLock.readLock().unlock();
        }
    }

    public List<UrlInfo> getListQueueUrls(SettingsRepository settingsRepository) {
        CrawlType crawlType = settingsRepository.getSelectedCrawlType();

        //int k0 = settingsRepository.getK0(crawlType.getK0PrefKey()); // Đảm bảo getK0 nhận PrefKey
        //int k1 = settingsRepository.getK1(crawlType.getK1PrefKey()); // Đảm bảo getK1 nhận PrefKey
        String[] arrayKyTu = GetKyTuAZ.getArrayAZk0k1(settingsRepository);
        // Lấy Bảng Queue tùy loại đang làm
        String queueTable = crawlType.getUrlQueueTableName();

        // Lưu tổng Ký tự LẤY ĐƯỢC, CŨNG = list URL.size
        settingsRepository.saveTotalUrls(crawlType.getTotalUrlsPrefKey(), arrayKyTu.length);

        // Lấy list urlInfos: Mỗi Phần tử urlInfos là 1 record
        List<UrlInfo> urlInfos = new ArrayList<>();

        //int idUrl = 0;  // idUrl=parentId: PHẢI LÀ k0 ĐỂ ĐẢM BẢO THEO THỨ TỰ: VÌ CÓ THỂ CHỌN BAN ĐẦU K0=3
        int idUrl = settingsRepository.getK0(crawlType.getK0PrefKey());
        // 1. Chiếm giữ quyền ghi tuyệt đối
        SQLiteDatabase db = getDb();
        dbLock.writeLock().lock();

        try {
            // 2. Xóa dữ liệu cũ
        /** PHẢI DELETE HẾT RECORD TRONG BẢNG: crawlType.getUrlQueueTableName()
         * RỒI LOAD URL MỚI TÙY THEO HỆ SỐ K0, K1 LẠI
         */
            //this.deleteAllRecords(crawlType.getUrlQueueTableName());  //cũ
            db.delete(queueTable, null, null);  // Ngoài transaction: không cần rollback

            // RESET ID về 1 (Xóa bộ đếm trong sqlite_sequence)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", new String[]{queueTable});

            // 3. Bắt đầu Transaction để ghi hàng loạt cực nhanh
            db.beginTransactionNonExclusive();

            try {
                for (String kytu : arrayKyTu) {
                    if (kytu == null || kytu.trim().isEmpty()) continue;

                    String fullUrl = (AppConstants.URL0 + kytu + AppConstants.URL1).trim();
                    String maThuocP = "https://www..." + "key" + fullUrl.split("key")[1];
                    long currentTime = System.currentTimeMillis() / 1000;

                    // Thêm vào list trả về
                    urlInfos.add(new UrlInfo(fullUrl, idUrl, 1, maThuocP, 0,
                            null, 0, -1, currentTime));

                    // Ghi vào Database
                    ContentValues values = new ContentValues();
                    values.put(DBHelperThuoc.ID_URL, idUrl);
                    values.put(DBHelperThuoc.URL, fullUrl);
                    // STATUS và LEVEL đã có DEFAULT trong định nghĩa bảng nên không cần put nếu dùng giá trị mặc định

                    db.insertWithOnConflict(queueTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    idUrl++;
                }
                db.setTransactionSuccessful(); // Đánh dấu hoàn tất thành công
            } finally {
                db.endTransaction(); // Kết thúc transaction
            }
        } catch (Exception e) {
            Log.e("DB_QUEUE", "Lỗi khởi tạo Queue: " + e.getMessage());
        } finally {
            dbLock.writeLock().unlock(); // Nhả khóa cho các luồng khác
        }

        return urlInfos;


            //////////////////////////////////////////////////////
//        String kytu;
//
//        // Ghi URL vào bảng initUrlsTable
//        //SQLiteDatabase dbThuoc = DBHelperThuoc.getInstance(context).getWritableDatabase();
//        for (int n=0; n < arrayKyTu.length; n++){
//            kytu = arrayKyTu[n];
//            if (kytu.trim().isEmpty()) continue; //Chỉ kiểm tra url rỗng (hoặc chỉ chứa các khoảng trắng)
//            // Vẫn lấy kytu có khoảng trắng ở đầu và cuối
////            String[] row = new String[3];
////            row[0] = String.valueOf(parentId);  //parentId
////            row[1] = String.valueOf(1);     //Cũ: level = 0; Mới: level (thành page) = 1
////            row[2] = (AppConstants.URL0 + kytu + AppConstants.URL1).trim();   //url
////
////            // Xem: tìm lỗi thiếu đếm processedUrlStart1Counter thiếu 1
////            //Log.d(TAG, "loadUrlsFromAsset: processedUrlStart1Counter, url (start=1) = " + row[2]+ ";parentId=" + row[0]);
////            //
////            listRowUrls.add(row);   // Hoặc chỉ 1 lệnh: listRowUrls.add(new String[]{String.valueOf(parentId), String.valueOf(parentId), (url0 + kytu + url1).trim()});
//
////            kytu = url0 + kytu + url1;
////            String trimmedUrl = kytu.trim(); //Cắt khoảng trắng ở đầu và cuối
////            urls.add(trimmedUrl);
//
//            // Sử dụng List<UrlInfo> urlInfos
//
//            String url = (AppConstants.URL0 + kytu + AppConstants.URL1).trim();   //url
//            // Xem
//            if ((url.contains("+"))||url.contains("A+")||(url.contains("+A"))) {
//                int iStop = 0;
//                Log.d("CrawlType", "getLisUrLFromCrawlType: DAU CONG=" + url);
//            }
//
//            //int level = 1;     //Cũ: level = 0; Mới: level (thành page) = 1
//            //long parentId = String.valueOf(parentId);  //parentId
//            //urlInfos.add(new UrlInfo(url, level, parentId++));
//            // Giá trị trả về
//            //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
//            String maThuocP = "https://www..." +"key" + url.split("key")[1];
//            urlInfos.add(new UrlInfo(AppConstants.URL0 + kytu + AppConstants.URL1.trim(), parentId, 1, maThuocP,0,
//                    null, 0, -1, System.currentTimeMillis()/1000));
//
//            // Ghi URL vào bảng urlQueueTableName
//            ContentValues values = new ContentValues();
//            values.put(DBHelperThuoc.ID_URL, parentId);
//            // LEVEL: mặt đinh = 1
//            //values.put(DBHelperThuoc.LEVEL, 1); //Cũ: Của url cha level = 0 (Mặc định); Mới: level (thành page) = 1 (Mặc định)
//            // STATUS: BAN ĐẦU mặt đinh = 0 chưa xử lý
//            //values.put(STATUS, 0); // 0: chưa xử lý, có thể thêm cột 'completed' nếu cần. Mặc định completed = 0 (uncompleted)
//            values.put("url", (AppConstants.URL0 + kytu + AppConstants.URL1).trim());
//
//            //values.put(ERR_MESSAGE, "");    //Ban đầu chưa có ERR_MESSAGE=""
//            //values.put(LAST_RECORD_INDEX, -1);  //-1: Mặc đinh= -1, nghĩa là BAN ĐÀU chưa có record cuối thu được
//            //values.put(DBHelperThuoc.BOOLEAN_URL_CHILD, 0); // Mặc định ban đầu là 0 (false) : không phai là url Child (con)
//            //Cũ: có ném lô
//            //dbThuoc.insert( initUrlsTable, null, values); //initUrlsTable. tableNameThuoc_Nao phải có
//            //Mới: không ném lỗi
//            //dbThuoc.insertWithOnConflict( initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
//            dbThuoc.insertWithOnConflict( crawlType.getUrlQueueTableName(), null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
//            parentId++;
//        }
//
//        return urlInfos;
        ///

    }

    public static class ColumnInfoUrlCha {
        public final String dbColumnName;   // Tên cột trong SQLite
        //public final String headerName;     // Tiêu đề cột trong Excel
        //public final String title;        // Tiêu đề cột trong Excel
        //public final String dbLinkColumnName;  // Nếu cần hyperlink
        public final String dbUrlCol_p;  // Chứa URL: Nếu cần hyperlink
        public final String ghiChu_p;  // Chứa URL: Nếu cần hyperlink
        public final int width;
        public final boolean wrapText;
        public int colIndex = -1;        // Gán sau khi query
        public int linkColIndex = -1;

        public ColumnInfoUrlCha(String dbColumnName, String dbUrlCol_P, String ghiChu_P, int width, boolean wrapText) {
            this.dbColumnName = dbColumnName;
            this.dbUrlCol_p = dbUrlCol_P;
            this.ghiChu_p = ghiChu_P;
            //this.title = title;
            //this.headerName = headerName;
            //this.dbLinkColumnName = dbLinkColumnName;


            this.width = width;
            this.wrapText = wrapText;
        }
    }

    public static final List<ColumnInfoUrlCha> EXPORT_COLUMNS_CHA = Arrays.asList(
            // Ý nghĩa: ColumnInfo(String dbCol, String header, String linkCol, int width, boolean wrapText)new ColumnInfo(DBHelperThuoc.ID_URL, "ID_URL", null, 12 * 256, false));    // hoặc ID_URL=Mã cha
            new ColumnInfoUrlCha(ID_URL, "ID_URL", null,12 * 256, false),    // hoặc ID_URL=Mã cha
            new ColumnInfoUrlCha(LEVEL, "Cấp độ", null, 8 * 256, false),            // Hoặc : LEVEL

            new ColumnInfoUrlCha(KY_TU_SEARCH, "KÝ TỰ SEARCH", null, 15 * 256, false),  //"Chữ cái tìm kiếm"

            // URL = MA_THUOC_LINK_P: ĐẶT TRƯỚC KY_TU_SEARCH ĐỂ DUYỆT TRƯỚC ĐỂ LẤY ĐƯỢC KY_TU_SEARCH (SAU)
            new ColumnInfoUrlCha(MA_THUOC_P, "Mã thuốc", P_URL, 20 * 256, false),   // Chứa Mã Link https://...key=...
            //TEN_THUOC: dùng để ghi lại ghi chú
            new ColumnInfoUrlCha(TEN_THUOC, "Tên thuốc", null, 25 * 256, true),
            new ColumnInfoUrlCha(P_URL, "Url", null, 25 * 256, true),
            new ColumnInfoUrlCha(GHI_CHU, "Ghi chú", null, 25 * 256, true)
            // Thêm index column

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
        public int idxCol = -1;        // Sẽ gán giá sau khi query: columnIndex
        public int idxCol_Link = -1;    // Sẽ gán giá sau khi query: linkColumnIndex

        public ColumnInfo(String dbColumnName, String headerName, String dbLinkColumnName, int width, boolean wrapText) {
        //public ColumnInfo(String dbColumnName, int idxCol, String headerName, String dbLinkColumnName, int idxColLink, int width, boolean wrapText) {
            this.dbColumnName = dbColumnName;
            //this.title = title;
            this.headerName = headerName;
            this.dbLinkColumnName = dbLinkColumnName;
            this.width = width;
            this.wrapText = wrapText;

            // Thêm index column: để tìm cho nhanh: kHI DUYỆT cursor CHA-CON, EXPORT_COLUMNS: ta sẽ gán giá giá trị idxCol, idxColLink
            //this.idxCol = idxCol;       // Cột chứa giá trị value KHÔNG PHẢI LINK
            //this.idxColLink = idxColLink;   // Cột chứa giá trị value là link

        }
    }

    public static final List<ColumnInfo> EXPORT_COLUMNS = Arrays.asList(
            // Ý nghĩa: ColumnInfo(String dbCol, String header, String linkCol, int width, boolean wrapText)new ColumnInfo(DBHelperThuoc.ID_URL, "ID_URL", null, 12 * 256, false));    // hoặc ID_URL=Mã cha

            // Của CHA: Tự lấy index của cột sau khi lấy query CHA-CON
//            new ColumnInfo(ID_URL, "ID_URL", null, 12 * 256, false),    // hoặc ID_URL=Mã cha
//            new ColumnInfo(LEVEL, "Level", null, 8 * 256, false),            // Hoặc : LEVEL/Cấp độ
//            //new ColumnInfo(MA_THUOC_P, "KÝ TỰ SEARCH", null, 15 * 256, false),  //Chỉ dùng MA_THUOC của bảng con
//            new ColumnInfo(KY_TU_SEARCH, "KÝ TỰ SEARCH", null, 15 * 256, false),  //"Chữ cái tìm kiếm"

            // Của CON:
            // Thêm vào đầu list nếu muốn có STT
            new ColumnInfo(null, "STT", null, 5 * 256, false),
            new ColumnInfo(MA_THUOC, "Mã thuốc", MA_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(TEN_THUOC, "Tên thuốc", null, 25 * 256, true),
            new ColumnInfo(THANH_PHAN, "Thành phần", THANH_PHAN_LINK, 40 * 256, true),
            new ColumnInfo(NHOM_THUOC, "Nhóm thuốc", NHOM_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(DANG_THUOC, "Dạng thuốc", DANG_THUOC_LINK, 20 * 256, false),
            new ColumnInfo(SAN_XUAT, "Nhà sản xuất", SAN_XUAT_LINK, 20 * 256, true),
            new ColumnInfo(DANG_KY, "Đăng ký", DANG_KY_LINK, 15 * 256, false),
            new ColumnInfo(PHAN_PHOI, "Phân phối", PHAN_PHOI_LINK, 20 * 256, false),
            new ColumnInfo(SDK, "Số đăng ký", SDK_LINK, 20 * 256, false),
            new ColumnInfo(CAC_THUOC, "Các thuốc cùng nhóm", CAC_THUOC_LINK, 25 * 256, true),    //nếu còn phía sau thì thêm ","
            new ColumnInfo(GHI_CHU, "Ghi chú", null, 25 * 256, true)    // Tạo cột Ghi chú cho sheet Excel

    );

    /*
     *
     * @return
     */
    public boolean hasErrorUrls(CrawlType crawlType) {
        //. Mới: hàm DatabaseUtils.longForQuery được thiết kế để thay thế toàn bộ quy trình thủ công (mở cursor -> di chuyển về dòng đầu -> lấy giá trị -> đóng cursor).
        // Thực tế, hàm DatabaseUtils.longForQuery đã tự quản lý việc mở và đóng tài nguyên bên trong nó.
        // Nếu bạn không thực sự cần các dòng Log từ cursor,
        // hãy xóa bỏ hoàn toàn phần khởi tạo Cursor để code sạch sẽ nhất:
        long count = DatabaseUtils.longForQuery(
                getReadableDatabase(),
                "SELECT COUNT(*) FROM " + crawlType.getUrlQueueTableName() + " WHERE status=0",
                null
        );
        return count > 0;
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
        dbLock.writeLock().lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(STATUS, status);
            values.put(ERR_MESSAGE, errorMessage);
            db.update(tableQueue, values, "url=?", new String[]{url});
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    /**
     *
     * @return
     */
    public List<ErrorUrl> getListErrorUrls(CrawlType crawlType) {
        List<ErrorUrl> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + ID_URL + ", " + LEVEL + ", " + URL+ ", " + STATUS + ", " + ERR_MESSAGE + " FROM " + crawlType.getUrlQueueTableName() + " WHERE status=0", null);
        //cursor = db.query(crawlType.getUrlQueueTableName(), new String[]{ID_URL,LEVEL, URL, STATUS, ERR_MESSAGE}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                //int id = cursor.getInt(0);
                String url = cursor.getString(cursor.getColumnIndexOrThrow(URL));
//                long parentId = cursor.getLong(cursor.getColumnIndexOrThrow(ID_URL));
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
        return mContext.getDatabasePath(DATABASE_NAME).getAbsolutePath();
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

